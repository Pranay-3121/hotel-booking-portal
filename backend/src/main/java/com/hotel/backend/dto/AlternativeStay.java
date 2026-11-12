package com.hotel.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AlternativeStay(LocalDate checkIn, LocalDate checkOut, int nights,
                              BigDecimal totalAmount, int availableRooms, int shiftDays) {
}