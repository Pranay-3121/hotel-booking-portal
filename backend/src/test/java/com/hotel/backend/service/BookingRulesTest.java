package com.hotel.backend.service;

import com.hotel.backend.entity.RoomInventory;
import com.hotel.backend.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookingRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    private RoomInventory night(int total, int booked) {
        RoomInventory r = new RoomInventory();
        r.setTotalRooms(total);
        r.setBookedRooms(booked);
        return r;
    }

    @Test
    void nightsAreCheckOutMinusCheckIn() {
        assertEquals(3, BookingRules.nights(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 23)));
    }

    @Test
    void totalIsPricePerNightTimesNights() {
        assertEquals(new BigDecimal("11700.00"),
                BookingRules.totalAmount(new BigDecimal("3900.00"), 3));
    }

    @Test
    void validStayPasses() {
        assertDoesNotThrow(() ->
                BookingRules.validateStay(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22), TODAY));
    }

    @Test
    void checkInTodayIsAllowed() {
        assertDoesNotThrow(() ->
                BookingRules.validateStay(TODAY, TODAY.plusDays(1), TODAY));
    }

    @Test
    void checkInInThePastIsRejected() {
        ApiException ex = assertThrows(ApiException.class, () ->
                BookingRules.validateStay(TODAY.minusDays(1), TODAY.plusDays(1), TODAY));
        assertEquals("INVALID_DATES", ex.getCode());
    }

    @Test
    void checkOutBeforeCheckInIsRejected() {
        assertThrows(ApiException.class, () ->
                BookingRules.validateStay(TODAY.plusDays(5), TODAY.plusDays(3), TODAY));
    }

    @Test
    void checkOutSameAsCheckInIsRejected() {
        assertThrows(ApiException.class, () ->
                BookingRules.validateStay(TODAY.plusDays(5), TODAY.plusDays(5), TODAY));
    }

    @Test
    void staysLongerThanMaximumAreRejected() {
        assertThrows(ApiException.class, () ->
                BookingRules.validateStay(TODAY, TODAY.plusDays(BookingRules.MAX_NIGHTS + 1), TODAY));
    }

    @Test
    void availabilityIsTheLowestAcrossAllNights() {
        List<RoomInventory> stay = List.of(night(5, 1), night(5, 4), night(5, 0));
        assertEquals(1, BookingRules.minAvailable(stay));
    }

    @Test
    void emptyInventoryMeansNothingAvailable() {
        assertEquals(0, BookingRules.minAvailable(List.of()));
    }
}