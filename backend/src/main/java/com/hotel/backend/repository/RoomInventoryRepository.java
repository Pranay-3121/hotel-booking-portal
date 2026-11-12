package com.hotel.backend.repository;

import com.hotel.backend.entity.RoomInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomInventoryRepository extends JpaRepository<RoomInventory, Long> {

    // Read-only view of the nights in a stay (check-out night excluded)
    @Query("select i from RoomInventory i where i.hotelId = :hotelId "
         + "and i.stayDate >= :checkIn and i.stayDate < :checkOut order by i.stayDate")
    List<RoomInventory> findStay(@Param("hotelId") Long hotelId,
                                 @Param("checkIn") LocalDate checkIn,
                                 @Param("checkOut") LocalDate checkOut);

    // Same rows, but locked (SELECT ... FOR UPDATE) so two bookings can't race for the last room
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from RoomInventory i where i.hotelId = :hotelId "
         + "and i.stayDate >= :checkIn and i.stayDate < :checkOut order by i.stayDate")
    List<RoomInventory> findStayForUpdate(@Param("hotelId") Long hotelId,
                                          @Param("checkIn") LocalDate checkIn,
                                          @Param("checkOut") LocalDate checkOut);
}