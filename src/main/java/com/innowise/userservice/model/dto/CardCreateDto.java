package com.innowise.userservice.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardCreateDto {
    @NotNull(message = "User id cannot be null")
    private UUID userId;

    @NotBlank(message = "Card number cannot be empty")
    @Pattern(regexp = "\\d{13,19}", message = "Card number must contain 13-19 digits")
    private String number;

    @NotBlank(message = "Card holder cannot be empty")
    private String holder;

    @NotNull(message = "Expiration date cannot be null")
    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;
}
