package br.com.core.barbershop.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.core.barbershop.domain.BarberService;

public interface BarberServiceRepository extends JpaRepository<BarberService, UUID>{
    

}
