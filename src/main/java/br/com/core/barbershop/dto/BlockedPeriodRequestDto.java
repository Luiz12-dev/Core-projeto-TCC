package br.com.core.barbershop.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BlockedPeriodRequestDto(
    @NotNull(message = "Start date/time is required")
    @Future(message = "Start must be in the future")
    LocalDateTime startDateTime,

    @NotNull(message = "End date/time is required")
    @Future(message = "End must be in the future")
    LocalDateTime endDateTime,

    @NotBlank(message = "Reason is required")
    String reason
) {
}
