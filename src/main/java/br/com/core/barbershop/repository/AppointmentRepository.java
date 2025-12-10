package br.com.core.barbershop.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.core.barbershop.domain.Appointment;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID>{

    List<Appointment> findByClientId(UUID id);


    @Query(
        value = """
                SELECT COUNT(*)>0
                FROM appointments a
                JOIN services s ON a.service_id = s.id
                WHERE a.status <> 'CANCELADO'
                AND a.date_time < :newEndTime
                AND (a.date_time + make_interval(mins => s.duration_min)) > :newStartTime
                """, nativeQuery = true)
    boolean existsConflictingAppointment(
        @Param("newStartTime")LocalDateTime newStartTime,
        @Param("newEndTime")LocalDateTime newEndTime);
}
