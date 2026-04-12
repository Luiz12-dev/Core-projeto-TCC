package br.com.core.barbershop.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record BusinessHoursRequestDto(
    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Opening time is required")
    LocalTime openTime,

    @NotNull(message = "Closing time is required")
    LocalTime closeTime
) {
}
