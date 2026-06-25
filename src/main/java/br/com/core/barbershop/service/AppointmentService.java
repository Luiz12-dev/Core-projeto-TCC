package br.com.core.barbershop.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.core.barbershop.domain.Appointment;
import br.com.core.barbershop.domain.BarberService;
import br.com.core.barbershop.domain.BusinessHours;
import br.com.core.barbershop.domain.Client;
import br.com.core.barbershop.dto.AppointmentRequestDto;
import br.com.core.barbershop.dto.AppointmentResponseDto;
import br.com.core.barbershop.dto.RevenueResponseDto;
import br.com.core.barbershop.enuns.AppointmentStatus;
import br.com.core.barbershop.exception.BusinessRuleException;
import br.com.core.barbershop.exception.ResourceNotFoundException;
import br.com.core.barbershop.repository.AppointmentRepository;
import br.com.core.barbershop.repository.BarberServiceRepository;
import br.com.core.barbershop.repository.BlockedPeriodRepository;
import br.com.core.barbershop.repository.BusinessHoursRepository;
import br.com.core.barbershop.repository.ClientRepository;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentValidator validator;
    private final ClientRepository clientRepository;
    private final BarberServiceRepository serviceRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final BlockedPeriodRepository blockedPeriodRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentValidator validator,
            ClientRepository clientRepository,
            BarberServiceRepository serviceRepository,
            BusinessHoursRepository businessHoursRepository,
            BlockedPeriodRepository blockedPeriodRepository) {
        this.appointmentRepository = appointmentRepository;
        this.validator = validator;
        this.clientRepository = clientRepository;
        this.serviceRepository = serviceRepository;
        this.businessHoursRepository = businessHoursRepository;
        this.blockedPeriodRepository = blockedPeriodRepository;
    }

    @Transactional
    public AppointmentResponseDto create(AppointmentRequestDto req, String authenticatedEmail) {
        validator.validate(req);

        Client client = clientRepository.findByEmail(authenticatedEmail)
                .orElseGet(() -> {
                    Client newClient = new Client();
                    newClient.setEmail(authenticatedEmail);
                    newClient.setUsername(authenticatedEmail); // Defaulting to email
                    newClient.setPhone("00000000000"); // Default phone, frontend should ideally provide this
                    return clientRepository.save(newClient);
                });

        BarberService service = serviceRepository.findById(req.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + req.serviceId()));

        Appointment newAppointment = Appointment.builder()
                .client(client)
                .service(service)
                .dateTime(req.dateTime())
                .observation(req.observation())
                .status(AppointmentStatus.PENDING)
                .price(service.getPrice())
                .build();

        Appointment saved = appointmentRepository.save(newAppointment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getMyHistory(String authenticatedEmail) {
        Client client = clientRepository.findByEmail(authenticatedEmail)
                .orElseGet(() -> {
                    Client newClient = new Client();
                    newClient.setEmail(authenticatedEmail);
                    newClient.setUsername(authenticatedEmail);
                    newClient.setPhone("00000000000");
                    return clientRepository.save(newClient);
                });

        return appointmentRepository.findByClientIdOrderByDateTimeDesc(client.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LocalTime> getAvailableSlots(LocalDate date, UUID serviceId) {
        BarberService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + serviceId));

        if (!service.getActive()) {
            throw new BusinessRuleException("This service is inactive");
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int durationMin = service.getDurationMin();

        List<BusinessHours> periods = businessHoursRepository.findByDayOfWeekAndActiveTrue(dayOfWeek);
        if (periods.isEmpty()) {
            return List.of();
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        List<Appointment> existingAppointments = appointmentRepository.findActiveByDateRange(dayStart, dayEnd);

        List<LocalTime> availableSlots = new ArrayList<>();

        for (BusinessHours bh : periods) {
            LocalTime current = bh.getOpenTime();
            LocalTime periodEnd = bh.getCloseTime();

            while (current.plusMinutes(durationMin).compareTo(periodEnd) <= 0) {
                LocalDateTime slotStart = date.atTime(current);
                LocalDateTime slotEnd = slotStart.plusMinutes(durationMin);

                if (slotStart.isAfter(LocalDateTime.now())) {
                    boolean hasConflict = existingAppointments.stream().anyMatch(app -> {
                        LocalDateTime appStart = app.getDateTime();
                        LocalDateTime appEnd = appStart.plusMinutes(app.getService().getDurationMin());
                        return slotStart.isBefore(appEnd) && slotEnd.isAfter(appStart);
                    });

                    boolean isBlocked = blockedPeriodRepository.existsConflictingBlock(slotStart, slotEnd);

                    if (!hasConflict && !isBlocked) {
                        availableSlots.add(current);
                    }
                }

                current = current.plusMinutes(30);
            }
        }

        return availableSlots;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getTodayAppointments() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return appointmentRepository.findByDateTimeBetweenOrderByDateTimeAsc(startOfDay, endOfDay)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RevenueResponseDto getMonthlyRevenue(int month, int year) {
        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        List<Appointment> completed = appointmentRepository
                .findByStatusAndDateRange(AppointmentStatus.COMPLETED, start, end);

        BigDecimal total = completed.stream()
                .map(Appointment::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RevenueResponseDto(month, year, total, (long) completed.size());
    }

    @Transactional
    public AppointmentResponseDto updateStatus(UUID id, AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        validateStatusTransition(appointment.getStatus(), newStatus);

        appointment.setStatus(newStatus);
        Appointment updated = appointmentRepository.save(appointment);
        return toResponse(updated);
    }

    @Transactional
    public AppointmentResponseDto cancelAppointment(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel a completed appointment");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessRuleException("Appointment is already cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment updated = appointmentRepository.save(appointment);
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> findAll() {
        return appointmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponseDto findById(UUID id) {
        Appointment app = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));
        return toResponse(app);
    }

    private void validateStatusTransition(AppointmentStatus current, AppointmentStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == AppointmentStatus.CONFIRMED || next == AppointmentStatus.CANCELLED;
            case CONFIRMED -> next == AppointmentStatus.IN_PROGRESS || next == AppointmentStatus.CANCELLED;
            case IN_PROGRESS -> next == AppointmentStatus.COMPLETED || next == AppointmentStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            throw new BusinessRuleException(
                    "Invalid status transition: " + current + " → " + next
                            + ". Allowed transitions: PENDING→CONFIRMED, CONFIRMED→IN_PROGRESS, IN_PROGRESS→COMPLETED. "
                            + "Cancellation is allowed from any active status.");
        }
    }

    private AppointmentResponseDto toResponse(Appointment app) {
        return new AppointmentResponseDto(
                app.getId(),
                app.getDateTime(),
                app.getObservation(),
                app.getStatus(),
                app.getClient().getId(),
                app.getClient().getUsername(),
                app.getClient().getPhone(),
                app.getService().getId(),
                app.getService().getServiceName(),
                app.getPrice());
    }
}
