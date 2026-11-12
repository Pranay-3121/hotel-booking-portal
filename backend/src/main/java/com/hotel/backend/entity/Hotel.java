package com.hotel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "hotels")
@Getter @Setter @NoArgsConstructor
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String amenities;
    private BigDecimal pricePerNight;
    private int totalRooms;
}