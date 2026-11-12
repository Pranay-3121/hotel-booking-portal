package com.hotel.backend.service;

import com.hotel.backend.entity.RoomInventory;
import com.hotel.backend.exception.ApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BookingRules {

    public static final int MAX_NIGHTS = 30;

    private BookingRules() {
    }

    /** A same-length stay, shifted by some days, that still has a free room on every night. */
    public record Alternative(LocalDate checkIn, int shiftDays, int availableRooms) {
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

    /**
     * Looks for stays of the same length that start 1..maxShift days before or after the requested
     * check-in. Closest first (a later start wins a tie), never before today, and only if every
     * night has an inventory row with a free room.
     */
    public static List<Alternative> findAlternatives(List<RoomInventory> inventory, LocalDate checkIn,
                                                     int nights, LocalDate today, int maxShift, int limit) {
        Map<LocalDate, Integer> free = new HashMap<>();
        for (RoomInventory r : inventory) {
            free.put(r.getStayDate(), r.available());
        }

        List<Alternative> result = new ArrayList<>();
        for (int distance = 1; distance <= maxShift && result.size() < limit; distance++) {
            for (int shift : new int[]{distance, -distance}) {
                if (result.size() >= limit) {
                    break;
                }
                LocalDate start = checkIn.plusDays(shift);
                if (start.isBefore(today)) {
                    continue;
                }
                int available = availableForStay(free, start, nights);
                if (available > 0) {
                    result.add(new Alternative(start, shift, available));
                }
            }
        }
        return result;
    }

    private static int availableForStay(Map<LocalDate, Integer> free, LocalDate start, int nights) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < nights; i++) {
            Integer rooms = free.get(start.plusDays(i));
            if (rooms == null) {
                return 0; // no inventory row = cannot be sold
            }
            min = Math.min(min, rooms);
        }
        return min;
    }
}