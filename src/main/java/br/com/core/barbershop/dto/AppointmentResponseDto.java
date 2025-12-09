package br.com.core.barbershop.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import br.com.core.barbershop.enuns.AppointmentStatus;

public record AppointmentResponseDto(

    UUID id,
    LocalDateTime dateTime,
    String observation,
    AppointmentStatus status,

    UUID clientId,
    String clientName,
    String clientPhone,

    UUID serviceId,
    String serviceName,
    BigDecimal price

) {

}
