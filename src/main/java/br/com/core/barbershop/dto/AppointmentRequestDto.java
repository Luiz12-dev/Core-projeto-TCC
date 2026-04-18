package br.com.core.barbershop.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record AppointmentRequestDto(
    @NotNull(message = "Service ID is required")
    UUID serviceId,

    String observation,

    @NotNull(message = "Date and time are required")
    @Future(message = "Appointment must be in the future")
    LocalDateTime dateTime
) {
}
