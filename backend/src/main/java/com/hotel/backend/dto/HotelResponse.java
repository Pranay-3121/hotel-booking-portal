package com.hotel.backend.dto;

import com.hotel.backend.entity.Hotel;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public record HotelResponse(Long id, String name, String location, String description,
                            List<String> amenities, BigDecimal pricePerNight,
                            Integer availableRooms, Integer nights, BigDecimal totalAmount) {

    public static HotelResponse from(Hotel h, Integer availableRooms, Integer nights, BigDecimal totalAmount) {
        List<String> amenities = Arrays.stream(h.getAmenities().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new HotelResponse(h.getId(), h.getName(), h.getLocation(), h.getDescription(),
                amenities, h.getPricePerNight(), availableRooms, nights, totalAmount);
    }
}