package br.com.core.barbershop.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ServiceRequestDto(
    @NotBlank(message = "The service name cannot be empty")
    String serviceName,
    @NotBlank(message = "The description cannot be empty")
    String description,

    @NotNull(message = "The value cannot be null")
    @Min(value = 0, message = "The value cannot be negative")
    BigDecimal value,

    @NotNull(message = "The duration cannot be null")
    @Min(value = 1, message = "The duration cannot be under 1 min")
    Integer durationMin
) {


}
