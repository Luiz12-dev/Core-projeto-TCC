package br.com.core.barbershop.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.core.barbershop.domain.BarberService;
import br.com.core.barbershop.domain.BusinessHours;
import br.com.core.barbershop.dto.AppointmentRequestDto;
import br.com.core.barbershop.exception.BusinessRuleException;
import br.com.core.barbershop.exception.ResourceNotFoundException;
import br.com.core.barbershop.repository.AppointmentRepository;
import br.com.core.barbershop.repository.BarberServiceRepository;
import br.com.core.barbershop.repository.BlockedPeriodRepository;
import br.com.core.barbershop.repository.BusinessHoursRepository;

@Component
public class AppointmentValidator {

    private final AppointmentRepository appointmentRepository;
    private final BarberServiceRepository serviceRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final BlockedPeriodRepository blockedPeriodRepository;

    public AppointmentValidator(
            AppointmentRepository appointmentRepository,
            BarberServiceRepository serviceRepository,
            BusinessHoursRepository businessHoursRepository,
            BlockedPeriodRepository blockedPeriodRepository) {
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
        this.businessHoursRepository = businessHoursRepository;
        this.blockedPeriodRepository = blockedPeriodRepository;
    }

    public void validate(AppointmentRequestDto req) {
        LocalDateTime appointmentDate = req.dateTime();

        if (appointmentDate.isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new BusinessRuleException("Appointment must be scheduled at least 30 minutes in advance");
        }

        BarberService service = serviceRepository.findById(req.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + req.serviceId()));

        if (!service.getActive()) {
            throw new BusinessRuleException("Cannot book an inactive service");
        }

        LocalDateTime endTime = appointmentDate.plusMinutes(service.getDurationMin());

        validateBusinessHours(appointmentDate, endTime);
        if (blockedPeriodRepository.existsConflictingBlock(appointmentDate, endTime)) {
            throw new BusinessRuleException("This time slot is blocked by the owner");
        }
        if (appointmentRepository.existsConflictingAppointment(appointmentDate, endTime)) {
            throw new BusinessRuleException("Time slot not available — there is already an appointment in this period");
        }
    }

    private void validateBusinessHours(LocalDateTime start, LocalDateTime end) {
        List<BusinessHours> periods = businessHoursRepository
                .findByDayOfWeekAndActiveTrue(start.getDayOfWeek());

        if (periods.isEmpty()) {
            throw new BusinessRuleException("The barbershop is closed on " + start.getDayOfWeek());
        }

        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();

        boolean fitsInPeriod = periods.stream()
                .anyMatch(bh -> !startTime.isBefore(bh.getOpenTime()) && !endTime.isAfter(bh.getCloseTime()));

        if (!fitsInPeriod) {
            throw new BusinessRuleException(
                    "Appointment must fit entirely within business hours. Available periods for "
                            + start.getDayOfWeek() + ": "
                            + periods.stream()
                                    .map(bh -> bh.getOpenTime() + " - " + bh.getCloseTime())
                                    .reduce((a, b) -> a + ", " + b)
                                    .orElse("none"));
        }
    }
}
