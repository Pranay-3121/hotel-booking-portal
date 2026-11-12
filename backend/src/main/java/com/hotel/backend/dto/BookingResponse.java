package com.hotel.backend.dto;

import com.hotel.backend.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(String reference, Long hotelId, String hotelName, String location,
                              String guestName, String guestEmail, LocalDate checkIn, LocalDate checkOut,
                              int nights, BigDecimal pricePerNight, BigDecimal totalAmount,
                              String status, LocalDateTime createdAt) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getReference(), b.getHotel().getId(), b.getHotel().getName(),
                b.getHotel().getLocation(), b.getGuestName(), b.getGuestEmail(), b.getCheckIn(),
                b.getCheckOut(), b.getNights(), b.getPricePerNight(), b.getTotalAmount(),
                b.getStatus().name(), b.getCreatedAt());
    }
}