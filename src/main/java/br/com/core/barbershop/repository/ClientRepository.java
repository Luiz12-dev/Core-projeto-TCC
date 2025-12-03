package br.com.core.barbershop.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.core.barbershop.domain.Client;

public interface ClientRepository extends JpaRepository<ClientRepository, UUID>{
    Optional<Client> findByEmail(String email);
}
