package br.com.core.barbershop.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.core.barbershop.domain.Appointment;
import br.com.core.barbershop.enuns.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByClientIdOrderByDateTimeDesc(UUID clientId);

    List<Appointment> findByDateTimeBetweenOrderByDateTimeAsc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM Appointment a WHERE a.dateTime >= :start AND a.dateTime < :end AND a.status NOT IN ('CANCELLED', 'COMPLETED') ORDER BY a.dateTime ASC")
    List<Appointment> findActiveByDateRange(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query(
        value = """
                SELECT COUNT(*)>0
                FROM appointments a
                JOIN services s ON a.service_id = s.id
                WHERE a.status NOT IN ('CANCELLED', 'COMPLETED')
                AND a.date_time < :newEndTime
                AND (a.date_time + make_interval(mins => s.duration_min)) > :newStartTime
                """, nativeQuery = true)
    boolean existsConflictingAppointment(
        @Param("newStartTime") LocalDateTime newStartTime,
        @Param("newEndTime") LocalDateTime newEndTime
    );

    @Query("SELECT a FROM Appointment a WHERE a.status = :status AND a.dateTime >= :start AND a.dateTime < :end")
    List<Appointment> findByStatusAndDateRange(
        @Param("status") AppointmentStatus status,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
