package br.com.core.barbershop.dto;

import br.com.core.barbershop.enuns.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequestDto(
    @NotNull(message = "New status is required")
    AppointmentStatus status
) {
}
