package com.hotel.backend.service;

import com.hotel.backend.entity.RoomInventory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlternativeDatesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    /** One room per night from `from` to `to` inclusive, all free. */
    private List<RoomInventory> inventory(LocalDate from, LocalDate to) {
        List<RoomInventory> rows = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            RoomInventory r = new RoomInventory();
            r.setStayDate(d);
            r.setTotalRooms(1);
            r.setBookedRooms(0);
            rows.add(r);
        }
        return rows;
    }

    private void book(List<RoomInventory> rows, LocalDate date) {
        rows.stream().filter(r -> r.getStayDate().equals(date)).forEach(r -> r.setBookedRooms(1));
    }

    @Test
    void closestAvailableStaysComeFirst() {
        List<RoomInventory> rows = inventory(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 30));
        book(rows, LocalDate.of(2026, 11, 21)); // blocks every 3-night stay touching the 21st

        List<BookingRules.Alternative> result =
                BookingRules.findAlternatives(rows, LocalDate.of(2026, 11, 20), 3, TODAY, 7, 3);

        assertEquals(3, result.size());
        assertEquals(LocalDate.of(2026, 11, 22), result.get(0).checkIn()); // +2 days
        assertEquals(2, result.get(0).shiftDays());
        assertEquals(LocalDate.of(2026, 11, 18), result.get(1).checkIn()); // -2 days
        assertEquals(-2, result.get(1).shiftDays());
        assertEquals(LocalDate.of(2026, 11, 23), result.get(2).checkIn()); // +3 days
    }

    @Test
    void neverReturnsMoreThanTheLimit() {
        List<RoomInventory> rows = inventory(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 30));

        List<BookingRules.Alternative> result =
                BookingRules.findAlternatives(rows, LocalDate.of(2026, 11, 20), 2, TODAY, 7, 2);

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2026, 11, 21), result.get(0).checkIn());
        assertEquals(LocalDate.of(2026, 11, 19), result.get(1).checkIn());
    }

    @Test
    void neverSuggestsStaysStartingInThePast() {
        List<RoomInventory> rows = inventory(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 30));
        book(rows, TODAY);

        List<BookingRules.Alternative> result =
                BookingRules.findAlternatives(rows, TODAY, 1, TODAY, 7, 10);

        assertEquals(7, result.size()); // only the 7 days after today
        assertTrue(result.stream().noneMatch(a -> a.checkIn().isBefore(TODAY)));
    }

    @Test
    void returnsNothingWhenEverythingIsSoldOut() {
        List<RoomInventory> rows = inventory(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 30));
        rows.forEach(r -> r.setBookedRooms(1));

        assertTrue(BookingRules.findAlternatives(rows, LocalDate.of(2026, 11, 20), 3, TODAY, 7, 3).isEmpty());
    }

    @Test
    void stayWithMissingInventoryNightsIsNotSuggested() {
        List<RoomInventory> rows = inventory(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22));
        book(rows, LocalDate.of(2026, 11, 21));

        assertTrue(BookingRules.findAlternatives(rows, LocalDate.of(2026, 11, 21), 2, TODAY, 2, 3).isEmpty());
    }
}