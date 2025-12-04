package br.com.core.barbershop.dto;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.core.barbershop.domain.BarberService;

public record ServiceResponseDto(

    UUID id,
    String serviceName,
    String description,
    BigDecimal value,
    Integer durationMin

) {

    public ServiceResponseDto toResponse(BarberService req){
        return new ServiceResponseDto(req.getId(),
         req.getServiceName(),
         req.getDescription(),
         req.getValue(),
        req.getDurationMin());
    }

}
