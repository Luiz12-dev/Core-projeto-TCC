package br.com.core.barbershop.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.core.barbershop.domain.BarberService;
import br.com.core.barbershop.domain.Client;
import br.com.core.barbershop.enuns.AppointmentStatus;

public record AppointmentResponseDto(

    UUID id,
    LocalDateTime dateTime,
    AppointmentStatus status,
    Client client,
    BarberService service



) {

}
