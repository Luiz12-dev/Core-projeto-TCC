package br.com.core.barbershop.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.core.barbershop.domain.Appointment;
import br.com.core.barbershop.domain.BarberService;
import br.com.core.barbershop.domain.BusinessHours;
import br.com.core.barbershop.domain.Client;
import br.com.core.barbershop.dto.AppointmentRequestDto;
import br.com.core.barbershop.dto.AppointmentResponseDto;
import br.com.core.barbershop.dto.RevenueResponseDto;
import br.com.core.barbershop.enums.AppointmentStatus;
import br.com.core.barbershop.exception.BusinessRuleException;
import br.com.core.barbershop.exception.DuplicateResourceException;
import br.com.core.barbershop.exception.ResourceNotFoundException;
import br.com.core.barbershop.repository.AppointmentRepository;
import br.com.core.barbershop.repository.BarberServiceRepository;
import br.com.core.barbershop.repository.BlockedPeriodRepository;
import br.com.core.barbershop.repository.BusinessHoursRepository;
import br.com.core.barbershop.repository.ClientRepository;
import br.com.core.barbershop.security.jwt.AuthenticatedUser;

@Service
public class AppointmentService {

    /** Marcador para telefone ausente; ver telefoneOu e atualizarDadosFaltantes. */
    private static final String TELEFONE_NAO_INFORMADO = "00000000000";

    /** Indice unico parcial criado na V6; ver reservar(). */
    static final String INDICE_HORARIO_ATIVO = "uq_appointments_horario_ativo";

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
    public AppointmentResponseDto create(AppointmentRequestDto req, AuthenticatedUser autenticado) {
        validator.validate(req);

        // O Core nao tem cadastro proprio de clientes: provisiona o Client na
        // primeira reserva, a partir dos claims do JWT. Nome e telefone so
        // faltam em tokens emitidos antes da V2 do servico de autenticacao —
        // nesse caso caimos no e-mail e num telefone vazio, que o painel
        // exibe como "nao informado".
        Client client = clientRepository.findByEmail(autenticado.email())
                .orElseGet(() -> {
                    Client newClient = new Client();
                    newClient.setEmail(autenticado.email());
                    newClient.setUsername(nomeOu(autenticado));
                    newClient.setPhone(telefoneOu(autenticado));
                    return clientRepository.save(newClient);
                });

        // Usuario que agendou antes da V2 ja tem Client com dados provisorios:
        // completa assim que o token passa a trazer os claims.
        atualizarDadosFaltantes(client, autenticado);

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

        return toResponse(reservar(newAppointment));
    }

    /**
     * O validador consulta e depois grava, entao duas requisicoes simultaneas
     * para o mesmo horario passam pela consulta juntas. Quem chega em segundo
     * esbarra no indice da V6. O flush imediato faz a violacao estourar aqui,
     * e nao no commit, onde ja nao daria para traduzi-la em 409.
     */
    private Appointment reservar(Appointment appointment) {
        try {
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            if (violouIndiceDeHorario(ex)) {
                throw new DuplicateResourceException(
                        "Time slot was just booked by another client — choose another time");
            }
            throw ex;
        }
    }

    private boolean violouIndiceDeHorario(DataIntegrityViolationException ex) {
        return ex.getCause() instanceof ConstraintViolationException violacao
                && INDICE_HORARIO_ATIVO.equalsIgnoreCase(violacao.getConstraintName());
    }

    /**
     * Client.phone e NOT NULL na V1. Tokens emitidos antes da V2 do servico de
     * autenticacao nao trazem o claim, entao gravamos o marcador historico em
     * vez de nulo — sem isto, a primeira reserva desses usuarios estoura 500.
     *
     * atualizarDadosFaltantes reconhece este mesmo marcador e substitui pelo
     * telefone real assim que o cliente volta com um token que o carregue.
     */
    private String telefoneOu(AuthenticatedUser autenticado) {
        return autenticado.phone() != null && !autenticado.phone().isBlank()
                ? autenticado.phone()
                : TELEFONE_NAO_INFORMADO;
    }

    private String nomeOu(AuthenticatedUser autenticado) {
        return autenticado.name() != null && !autenticado.name().isBlank()
                ? autenticado.name()
                : autenticado.email();
    }

    private void atualizarDadosFaltantes(Client client, AuthenticatedUser autenticado) {
        boolean mudou = false;

        if (autenticado.name() != null && !autenticado.name().isBlank()
                && client.getUsername().equals(client.getEmail())) {
            client.setUsername(autenticado.name());
            mudou = true;
        }

        if (autenticado.phone() != null && !autenticado.phone().isBlank()
                && (client.getPhone() == null || client.getPhone().isBlank()
                    || TELEFONE_NAO_INFORMADO.equals(client.getPhone()))) {
            client.setPhone(autenticado.phone());
            mudou = true;
        }

        if (mudou) {
            clientRepository.save(client);
        }
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getMyHistory(String authenticatedEmail) {
        // Cliente que nunca agendou nao tem historico — e nao deve ser criado aqui.
        // A auto-provisao do Client acontece em create(), que e o momento em que
        // ele passa a existir no dominio do Core. Escrever numa transacao readOnly
        // era o bug original.
        return clientRepository.findByEmail(authenticatedEmail)
                .map(client -> appointmentRepository.findByClientIdOrderByDateTimeDesc(client.getId())
                        .stream()
                        .map(this::toResponse)
                        .toList())
                .orElseGet(List::of);
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
    public AppointmentResponseDto cancelAppointment(UUID id, String requesterEmail, boolean isOwner) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        // A checagem de posse vem antes das regras de estado: caso contrario a
        // mensagem de erro vazaria que o agendamento alheio ja esta concluido.
        if (!isOwner && !appointment.getClient().getEmail().equals(requesterEmail)) {
            throw new AccessDeniedException("You can only cancel your own appointments");
        }

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
    public AppointmentResponseDto findById(UUID id, String requesterEmail, boolean isOwner) {
        Appointment app = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        // O OWNER enxerga tudo. O CLIENT so enxerga o proprio agendamento — sem
        // esta checagem, qualquer cliente le nome, telefone e e-mail de outro.
        if (!isOwner && !app.getClient().getEmail().equals(requesterEmail)) {
            throw new AccessDeniedException("You can only access your own appointments");
        }

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
