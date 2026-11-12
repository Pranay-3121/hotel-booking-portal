package com.hotel.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CancelRequest(
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email) {
}