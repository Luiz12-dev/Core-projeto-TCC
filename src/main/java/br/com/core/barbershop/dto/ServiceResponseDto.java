package br.com.core.barbershop.dto;

import java.math.BigDecimal;
import java.util.UUID;


public record ServiceResponseDto(

    UUID id,
    String serviceName,
    String description,
    BigDecimal price,
    Integer durationMin

) {
}
