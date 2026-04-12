package br.com.core.barbershop.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BlockedPeriodResponseDto(
    UUID id,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    String reason,
    LocalDateTime createdAt
) {
}
