package com.hotel.backend.service;

import com.hotel.backend.entity.RoomInventory;
import com.hotel.backend.exception.ApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class BookingRules {

    public static final int MAX_NIGHTS = 30;

    private BookingRules() {
    }

    public static void validateStay(LocalDate checkIn, LocalDate checkOut, LocalDate today) {
        if (checkIn == null || checkOut == null) {
            throw ApiException.badRequest("INVALID_DATES", "Check-in and check-out dates are required.");
        }
        if (checkIn.isBefore(today)) {
            throw ApiException.badRequest("INVALID_DATES", "Check-in date cannot be in the past.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.badRequest("INVALID_DATES", "Check-out must be after check-in.");
        }
        if (nights(checkIn, checkOut) > MAX_NIGHTS) {
            throw ApiException.badRequest("INVALID_DATES", "A single stay cannot exceed " + MAX_NIGHTS + " nights.");
        }
    }

    public static int nights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static BigDecimal totalAmount(BigDecimal pricePerNight, int nights) {
        return pricePerNight.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Rooms free for the WHOLE stay = the lowest availability across its nights. */
    public static int minAvailable(List<RoomInventory> nightsInventory) {
        return nightsInventory.stream().mapToInt(RoomInventory::available).min().orElse(0);
    }
}