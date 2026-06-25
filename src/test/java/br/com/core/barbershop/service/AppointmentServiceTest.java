package br.com.core.barbershop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.core.barbershop.domain.Appointment;
import br.com.core.barbershop.domain.BarberService;
import br.com.core.barbershop.domain.Client;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService Unit Tests")
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentValidator validator;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private BarberServiceRepository serviceRepository;
    @Mock
    private BusinessHoursRepository businessHoursRepository;
    @Mock
    private BlockedPeriodRepository blockedPeriodRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private Client buildClient(String email) {
        return Client.builder()
                .id(UUID.randomUUID())
                .username("Test User")
                .email(email)
                .phone("44999999999")
                .build();
    }

    private BarberService buildService(String name, BigDecimal price, int duration) {
        return BarberService.builder()
                .id(UUID.randomUUID())
                .serviceName(name)
                .price(price)
                .durationMin(duration)
                .active(true)
                .build();
    }

    private Appointment buildAppointment(Client client, BarberService service, AppointmentStatus status) {
        return Appointment.builder()
                .id(UUID.randomUUID())
                .client(client)
                .service(service)
                .dateTime(LocalDateTime.now().plusDays(1))
                .status(status)
                .price(service.getPrice())
                .build();
    }

    @Nested
    @DisplayName("updateStatus() — State Machine")
    class UpdateStatus {

        @Test
        @DisplayName("PENDING → CONFIRMED should succeed")
        void pendingToConfirmed() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.PENDING);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
            when(appointmentRepository.save(any())).thenReturn(appointment);

            AppointmentResponseDto result = appointmentService.updateStatus(appointment.getId(),
                    AppointmentStatus.CONFIRMED);

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
            verify(appointmentRepository).save(appointment);
        }

        @Test
        @DisplayName("CONFIRMED → IN_PROGRESS should succeed")
        void confirmedToInProgress() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.CONFIRMED);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
            when(appointmentRepository.save(any())).thenReturn(appointment);

            appointmentService.updateStatus(appointment.getId(), AppointmentStatus.IN_PROGRESS);

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("IN_PROGRESS → COMPLETED should succeed")
        void inProgressToCompleted() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.IN_PROGRESS);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
            when(appointmentRepository.save(any())).thenReturn(appointment);

            appointmentService.updateStatus(appointment.getId(), AppointmentStatus.COMPLETED);

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        }

        @Test
        @DisplayName("PENDING → CANCELLED should succeed")
        void pendingToCancelled() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.PENDING);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
            when(appointmentRepository.save(any())).thenReturn(appointment);

            appointmentService.updateStatus(appointment.getId(), AppointmentStatus.CANCELLED);

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        }

        @Test
        @DisplayName("PENDING → COMPLETED should FAIL (invalid transition)")
        void pendingToCompletedShouldFail() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.PENDING);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> appointmentService.updateStatus(appointment.getId(), AppointmentStatus.COMPLETED))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Invalid status transition");
        }

        @Test
        @DisplayName("COMPLETED → CANCELLED should FAIL (cannot change completed)")
        void completedToCancelledShouldFail() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.COMPLETED);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> appointmentService.updateStatus(appointment.getId(), AppointmentStatus.CANCELLED))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Invalid status transition");
        }

        @Test
        @DisplayName("CANCELLED → CONFIRMED should FAIL (cannot reactivate cancelled)")
        void cancelledToConfirmedShouldFail() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.CANCELLED);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> appointmentService.updateStatus(appointment.getId(), AppointmentStatus.CONFIRMED))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Invalid status transition");
        }

        @Test
        @DisplayName("CONFIRMED → PENDING should FAIL (cannot go backwards)")
        void confirmedToPendingShouldFail() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.CONFIRMED);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> appointmentService.updateStatus(appointment.getId(), AppointmentStatus.PENDING))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Invalid status transition");
        }
    }

    @Nested
    @DisplayName("cancelAppointment()")
    class CancelAppointment {

        @Test
        @DisplayName("Should cancel a PENDING appointment")
        void shouldCancelPending() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.PENDING);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
            when(appointmentRepository.save(any())).thenReturn(appointment);

            appointmentService.cancelAppointment(appointment.getId());

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should throw when cancelling a COMPLETED appointment")
        void shouldThrowWhenCancellingCompleted() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.COMPLETED);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> appointmentService.cancelAppointment(appointment.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot cancel a completed appointment");
        }

        @Test
        @DisplayName("Should throw when appointment already cancelled")
        void shouldThrowWhenAlreadyCancelled() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.CANCELLED);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> appointmentService.cancelAppointment(appointment.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already cancelled");
        }
    }

    // ============================================================
    // REVENUE TESTS
    // ============================================================

    @Nested
    @DisplayName("getMonthlyRevenue()")
    class MonthlyRevenue {

        @Test
        @DisplayName("Should calculate revenue from completed appointments")
        void shouldCalculateRevenue() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);

            List<Appointment> completed = List.of(
                    buildAppointment(client, service, AppointmentStatus.COMPLETED),
                    buildAppointment(client, service, AppointmentStatus.COMPLETED),
                    buildAppointment(client, service, AppointmentStatus.COMPLETED));

            when(appointmentRepository.findByStatusAndDateRange(any(), any(), any()))
                    .thenReturn(completed);

            RevenueResponseDto result = appointmentService.getMonthlyRevenue(4, 2026);

            assertThat(result.totalRevenue()).isEqualByComparingTo(new BigDecimal("105.00"));
            assertThat(result.totalAppointments()).isEqualTo(3L);
            assertThat(result.month()).isEqualTo(4);
            assertThat(result.year()).isEqualTo(2026);
        }

        @Test
        @DisplayName("Should return zero when no completed appointments")
        void shouldReturnZeroWhenNoCompleted() {
            when(appointmentRepository.findByStatusAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());

            RevenueResponseDto result = appointmentService.getMonthlyRevenue(4, 2026);

            assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totalAppointments()).isEqualTo(0L);
        }
    }

    // ============================================================
    // CLIENT HISTORY TESTS
    // ============================================================

    @Nested
    @DisplayName("getMyHistory()")
    class MyHistory {

        @Test
        @DisplayName("Should return appointments for authenticated user")
        void shouldReturnHistory() {
            Client client = buildClient("luiz@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);

            List<Appointment> appointments = List.of(
                    buildAppointment(client, service, AppointmentStatus.COMPLETED),
                    buildAppointment(client, service, AppointmentStatus.PENDING));

            when(clientRepository.findByEmail("luiz@email.com")).thenReturn(Optional.of(client));
            when(appointmentRepository.findByClientIdOrderByDateTimeDesc(client.getId())).thenReturn(appointments);

            List<AppointmentResponseDto> result = appointmentService.getMyHistory("luiz@email.com");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should throw when client profile not found")
        void shouldThrowWhenClientNotFound() {
            when(clientRepository.findByEmail("unknown@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appointmentService.getMyHistory("unknown@email.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Client profile not found");
        }
    }

    // ============================================================
    // TODAY APPOINTMENTS TESTS
    // ============================================================

    @Nested
    @DisplayName("getTodayAppointments()")
    class TodayAppointments {

        @Test
        @DisplayName("Should return today's appointments")
        void shouldReturnToday() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.CONFIRMED);

            when(appointmentRepository.findByDateTimeBetweenOrderByDateTimeAsc(any(), any()))
                    .thenReturn(List.of(appointment));

            List<AppointmentResponseDto> result = appointmentService.getTodayAppointments();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).clientName()).isEqualTo("Test User");
        }
    }

    // ============================================================
    // FIND BY ID TESTS
    // ============================================================

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Should find appointment by ID")
        void shouldFind() {
            Client client = buildClient("test@email.com");
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);
            Appointment appointment = buildAppointment(client, service, AppointmentStatus.PENDING);

            when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

            AppointmentResponseDto result = appointmentService.findById(appointment.getId());

            assertThat(result.id()).isEqualTo(appointment.getId());
        }

        @Test
        @DisplayName("Should throw when not found")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(appointmentRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appointmentService.findById(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
