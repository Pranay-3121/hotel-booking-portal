package com.hotel.backend.service;

import com.hotel.backend.dto.HotelResponse;
import com.hotel.backend.entity.Hotel;
import com.hotel.backend.entity.RoomInventory;
import com.hotel.backend.exception.ApiException;
import com.hotel.backend.repository.HotelRepository;
import com.hotel.backend.repository.RoomInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomInventoryRepository inventoryRepository;
    private final Clock clock;

    public List<HotelResponse> search(String location, LocalDate checkIn, LocalDate checkOut) {
        validateOptionalDates(checkIn, checkOut);
        List<Hotel> hotels = (location == null || location.isBlank())
                ? hotelRepository.findAll()
                : hotelRepository.findByLocationContainingIgnoreCase(location.trim());
        return hotels.stream().map(h -> toResponse(h, checkIn, checkOut)).toList();
    }

    public HotelResponse get(Long id, LocalDate checkIn, LocalDate checkOut) {
        validateOptionalDates(checkIn, checkOut);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("HOTEL_NOT_FOUND", "Hotel not found."));
        return toResponse(hotel, checkIn, checkOut);
    }

    private HotelResponse toResponse(Hotel hotel, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null) {
            return HotelResponse.from(hotel, null, null, null);
        }
        int nights = BookingRules.nights(checkIn, checkOut);
        List<RoomInventory> rows = inventoryRepository.findStay(hotel.getId(), checkIn, checkOut);
        // Missing inventory rows (outside the bookable window) means nothing can be sold
        int available = rows.size() == nights ? BookingRules.minAvailable(rows) : 0;
        return HotelResponse.from(hotel, available, nights,
                BookingRules.totalAmount(hotel.getPricePerNight(), nights));
    }

    private void validateOptionalDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null && checkOut == null) {
            return;
        }
        if (checkIn == null || checkOut == null) {
            throw ApiException.badRequest("INVALID_DATES", "Provide both check-in and check-out dates.");
        }
        BookingRules.validateStay(checkIn, checkOut, LocalDate.now(clock));
    }
}