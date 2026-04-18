package br.com.core.barbershop.exception;

import java.time.LocalDateTime;

public record StandardErrorDto(
    LocalDateTime timestamp,
    Integer status,
    String error,
    String message,
    String path
) {
}
