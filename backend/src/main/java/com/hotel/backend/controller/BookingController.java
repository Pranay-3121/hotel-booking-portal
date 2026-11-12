package com.hotel.backend.controller;

import com.hotel.backend.dto.BookingRequest;
import com.hotel.backend.dto.BookingResponse;
import com.hotel.backend.dto.CancelRequest;
import com.hotel.backend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest request) {
        return bookingService.create(request);
    }

    @GetMapping("/{reference}")
    public BookingResponse get(@PathVariable String reference, @RequestParam String email) {
        return bookingService.get(reference, email);
    }

    @PostMapping("/{reference}/cancel")
    public BookingResponse cancel(@PathVariable String reference, @Valid @RequestBody CancelRequest request) {
        return bookingService.cancel(reference, request.email());
    }
}