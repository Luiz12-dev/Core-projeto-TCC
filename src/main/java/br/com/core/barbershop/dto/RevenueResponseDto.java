package br.com.core.barbershop.dto;

import java.math.BigDecimal;

public record RevenueResponseDto(
    Integer month,
    Integer year,
    BigDecimal totalRevenue,
    Long totalAppointments
) {
}
