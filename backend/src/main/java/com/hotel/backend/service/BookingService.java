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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
    private final SecureRandom random = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomInventoryRepository inventoryRepository;
    private final Clock clock;

    @Transactional
    public BookingResponse create(BookingRequest request) {
        LocalDate today = LocalDate.now(clock);
        BookingRules.validateStay(request.checkIn(), request.checkOut(), today);

        Hotel hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> ApiException.notFound("HOTEL_NOT_FOUND", "Hotel not found."));

        int nights = BookingRules.nights(request.checkIn(), request.checkOut());

        // Locks the night rows until commit, so two guests cannot both take the last room
        List<RoomInventory> rows = inventoryRepository
                .findStayForUpdate(hotel.getId(), request.checkIn(), request.checkOut());

        if (rows.size() != nights) {
            throw ApiException.badRequest("OUTSIDE_BOOKING_WINDOW",
                    "These dates are outside the period we accept bookings for. Try earlier dates.");
        }
        if (BookingRules.minAvailable(rows) < 1) {
            throw ApiException.conflict("SOLD_OUT",
                    "Sorry, no rooms are available for the selected dates. Try different dates.");
        }

        rows.forEach(r -> r.setBookedRooms(r.getBookedRooms() + 1));

        Booking booking = new Booking();
        booking.setReference(newReference());
        booking.setHotel(hotel);
        booking.setGuestName(request.guestName().trim());
        booking.setGuestEmail(request.guestEmail().trim().toLowerCase());
        booking.setCheckIn(request.checkIn());
        booking.setCheckOut(request.checkOut());
        booking.setNights(nights);
        booking.setPricePerNight(hotel.getPricePerNight());
        booking.setTotalAmount(BookingRules.totalAmount(hotel.getPricePerNight(), nights));
        booking.setStatus(BookingStatus.CONFIRMED);

        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public BookingResponse get(String reference, String email) {
        return BookingResponse.from(find(reference, email));
    }

    @Transactional
    public BookingResponse cancel(String reference, String email) {
        Booking booking = find(reference, email);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.conflict("ALREADY_CANCELLED", "This booking has already been cancelled.");
        }
        if (booking.getCheckIn().isBefore(LocalDate.now(clock))) {
            throw ApiException.conflict("CANNOT_CANCEL", "This stay has already started and can no longer be cancelled.");
        }

        List<RoomInventory> rows = inventoryRepository
                .findStayForUpdate(booking.getHotel().getId(), booking.getCheckIn(), booking.getCheckOut());
        rows.forEach(r -> r.setBookedRooms(Math.max(0, r.getBookedRooms() - 1)));

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now(clock));
        return BookingResponse.from(booking);
    }

    private Booking find(String reference, String email) {
        return bookingRepository
                .findByReferenceAndGuestEmailIgnoreCase(reference.trim().toUpperCase(), email.trim())
                .orElseThrow(() -> ApiException.notFound("BOOKING_NOT_FOUND",
                        "No booking found for this reference and email."));
    }

    private String newReference() {
        String ref;
        do {
            StringBuilder sb = new StringBuilder("BK-");
            for (int i = 0; i < 8; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            ref = sb.toString();
        } while (bookingRepository.existsByReference(ref));
        return ref;
    }
}