package br.com.core.barbershop.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.core.barbershop.domain.Appointment;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID>{

    List<Appointment> findByClientID(UUID id);

}
