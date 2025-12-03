package br.com.core.barbershop.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BarberService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "The service name cannot be empty")
    @Column(nullable = false, unique = true)
    private String serviceName;

    @NotBlank(message = "the description cannot be empty")
    @Column(nullable = false)
    private String Description;

    @NotNull(message = "the value cannot be null")
    @Min(value = 0, message = "price cannot be negative" )
    private BigDecimal value;
    
    @NotNull(message = "duration cannot be null")
    @Min(value = 1, message = "duration must be at least 1 minut")
    private Integer durationMin;


}
