package br.com.core.barbershop.repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.core.barbershop.domain.BusinessHours;

public interface BusinessHoursRepository extends JpaRepository<BusinessHours, UUID> {

    List<BusinessHours> findByDayOfWeekAndActiveTrue(DayOfWeek dayOfWeek);

    List<BusinessHours> findAllByActiveTrue();
}
