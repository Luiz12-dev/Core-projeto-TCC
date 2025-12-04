package br.com.core.barbershop.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.core.barbershop.domain.BarberService;

public interface BarberServiceRepository extends JpaRepository<BarberService, UUID>{

    Optional<BarberService> findByServiceName(String serviceName);
    Optional<BarberService> deledeleteByServiceName(String serviceName);
    Boolean exiexistsByServiceName(String serviceName);

}
