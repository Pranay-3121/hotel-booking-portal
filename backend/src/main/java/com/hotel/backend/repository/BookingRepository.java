package com.hotel.backend.repository;

import com.hotel.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByReferenceAndGuestEmailIgnoreCase(String reference, String guestEmail);

    boolean existsByReference(String reference);
}