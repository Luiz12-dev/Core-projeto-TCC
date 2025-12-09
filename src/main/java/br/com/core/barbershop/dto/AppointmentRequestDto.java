package br.com.core.barbershop.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record AppointmentRequestDto(
    @NotNull
    UUID clientId,

    @NotNull
    UUID serviceId,

    String observation,

    @NotNull
    @Future(message = "The appointment, cannot be on the past")
    LocalDateTime dateTime
) {

}
