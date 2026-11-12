package com.hotel.backend.service;

import com.hotel.backend.dto.BookingRequest;
import com.hotel.backend.dto.BookingResponse;
import com.hotel.backend.entity.Booking;
import com.hotel.backend.entity.BookingStatus;
import com.hotel.backend.entity.Hotel;
import com.hotel.backend.entity.RoomInventory;
import com.hotel.backend.exception.ApiException;
import com.hotel.backend.repository.BookingRepository;
import com.hotel.backend.repository.HotelRepository;
import com.hotel.backend.repository.RoomInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 11, 20);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 11, 23); // 3 nights

    @Mock BookingRepository bookingRepository;
    @Mock HotelRepository hotelRepository;
    @Mock RoomInventoryRepository inventoryRepository;

    private BookingService service;
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        // "Today" is frozen at 12 Nov 2026 so tests never depend on the real date
        Clock clock = Clock.fixed(Instant.parse("2026-11-12T00:00:00Z"), ZoneId.of("UTC"));
        service = new BookingService(bookingRepository, hotelRepository, inventoryRepository, clock);

        hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Himalayan Retreat");
        hotel.setLocation("Manali");
        hotel.setPricePerNight(new BigDecimal("3900.00"));
    }

    private RoomInventory night(int total, int booked) {
        RoomInventory r = new RoomInventory();
        r.setHotelId(1L);
        r.setTotalRooms(total);
        r.setBookedRooms(booked);
        return r;
    }

    private BookingRequest request() {
        return new BookingRequest(1L, "Asha", "ASHA@Example.com", CHECK_IN, CHECK_OUT);
    }

    private Booking confirmedBooking(LocalDate checkIn, LocalDate checkOut) {
        Booking b = new Booking();
        b.setReference("BK-ABC12345");
        b.setHotel(hotel);
        b.setGuestName("Asha");
        b.setGuestEmail("asha@example.com");
        b.setCheckIn(checkIn);
        b.setCheckOut(checkOut);
        b.setNights(3);
        b.setPricePerNight(hotel.getPricePerNight());
        b.setTotalAmount(new BigDecimal("11700.00"));
        b.setStatus(BookingStatus.CONFIRMED);
        return b;
    }

    // ---------- create ----------

    @Test
    void bookingCalculatesTotalOnBackendAndReservesEveryNight() {
        List<RoomInventory> rows = List.of(night(2, 0), night(2, 0), night(2, 0));
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(inventoryRepository.findStayForUpdate(1L, CHECK_IN, CHECK_OUT)).thenReturn(rows);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.create(request());

        assertEquals(new BigDecimal("11700.00"), response.totalAmount());
        assertEquals(3, response.nights());
        assertEquals("CONFIRMED", response.status());
        assertEquals("asha@example.com", response.guestEmail()); // normalised to lower case
        assertTrue(response.reference().startsWith("BK-"));
        rows.forEach(r -> assertEquals(1, r.getBookedRooms()));
    }

    @Test
    void bookingFailsWhenAnyNightIsSoldOut() {
        // Middle night is full, so the stay cannot be sold even though other nights are free
        List<RoomInventory> rows = List.of(night(2, 0), night(2, 2), night(2, 0));
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(inventoryRepository.findStayForUpdate(1L, CHECK_IN, CHECK_OUT)).thenReturn(rows);

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request()));

        assertEquals("SOLD_OUT", ex.getCode());
        verify(bookingRepository, never()).save(any());
        assertEquals(0, rows.get(0).getBookedRooms()); // nothing reserved on failure
    }

    @Test
    void lastRoomCanBeBookedOnceButNotTwice() {
        List<RoomInventory> rows = List.of(night(1, 0), night(1, 0), night(1, 0));
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(inventoryRepository.findStayForUpdate(1L, CHECK_IN, CHECK_OUT)).thenReturn(rows);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(request()); // takes the last room

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request()));
        assertEquals("SOLD_OUT", ex.getCode());
    }

    @Test
    void bookingFailsWhenDatesAreOutsideInventoryWindow() {
        // Only 2 of the 3 nights have inventory rows
        List<RoomInventory> rows = List.of(night(2, 0), night(2, 0));
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(inventoryRepository.findStayForUpdate(1L, CHECK_IN, CHECK_OUT)).thenReturn(rows);

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request()));
        assertEquals("OUTSIDE_BOOKING_WINDOW", ex.getCode());
    }

    @Test
    void bookingFailsForPastCheckIn() {
        BookingRequest past = new BookingRequest(1L, "Asha", "asha@example.com",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(past));

        assertEquals("INVALID_DATES", ex.getCode());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookingFailsForUnknownHotel() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request()));
        assertEquals("HOTEL_NOT_FOUND", ex.getCode());
    }

    // ---------- cancel ----------

    @Test
    void cancellingReleasesAvailabilityForEveryNight() {
        Booking booking = confirmedBooking(CHECK_IN, CHECK_OUT);
        List<RoomInventory> rows = List.of(night(2, 1), night(2, 1), night(2, 1));
        when(bookingRepository.findByReferenceAndGuestEmailIgnoreCase("BK-ABC12345", "asha@example.com"))
                .thenReturn(Optional.of(booking));
        when(inventoryRepository.findStayForUpdate(1L, CHECK_IN, CHECK_OUT)).thenReturn(rows);

        BookingResponse response = service.cancel("BK-ABC12345", "asha@example.com");

        assertEquals("CANCELLED", response.status());
        rows.forEach(r -> assertEquals(0, r.getBookedRooms()));
        assertNotNull(booking.getCancelledAt());
    }

    @Test
    void cancellingTwiceIsRejectedAndDoesNotReleaseTwice() {
        Booking booking = confirmedBooking(CHECK_IN, CHECK_OUT);
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findByReferenceAndGuestEmailIgnoreCase("BK-ABC12345", "asha@example.com"))
                .thenReturn(Optional.of(booking));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cancel("BK-ABC12345", "asha@example.com"));

        assertEquals("ALREADY_CANCELLED", ex.getCode());
        verify(inventoryRepository, never()).findStayForUpdate(any(), any(), any());
    }

    @Test
    void cancellingWithWrongEmailLooksLikeNotFound() {
        when(bookingRepository.findByReferenceAndGuestEmailIgnoreCase("BK-ABC12345", "other@example.com"))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cancel("BK-ABC12345", "other@example.com"));
        assertEquals("BOOKING_NOT_FOUND", ex.getCode());
    }

    @Test
    void stayThatAlreadyStartedCannotBeCancelled() {
        Booking booking = confirmedBooking(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 13));
        when(bookingRepository.findByReferenceAndGuestEmailIgnoreCase("BK-ABC12345", "asha@example.com"))
                .thenReturn(Optional.of(booking));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cancel("BK-ABC12345", "asha@example.com"));
        assertEquals("CANNOT_CANCEL", ex.getCode());
    }
}