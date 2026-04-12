package br.com.core.barbershop.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record BusinessHoursResponseDto(
    UUID id,
    DayOfWeek dayOfWeek,
    LocalTime openTime,
    LocalTime closeTime,
    Boolean active
) {
}
