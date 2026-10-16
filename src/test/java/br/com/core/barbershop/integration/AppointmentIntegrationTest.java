package br.com.core.barbershop.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.core.barbershop.domain.Appointment;
import br.com.core.barbershop.domain.BarberService;
import br.com.core.barbershop.domain.BlockedPeriod;
import br.com.core.barbershop.domain.BusinessHours;
import br.com.core.barbershop.domain.Client;
import br.com.core.barbershop.dto.AppointmentRequestDto;
import br.com.core.barbershop.enums.AppointmentStatus;
import br.com.core.barbershop.repository.AppointmentRepository;
import br.com.core.barbershop.repository.BarberServiceRepository;
import br.com.core.barbershop.repository.BlockedPeriodRepository;
import br.com.core.barbershop.repository.BusinessHoursRepository;
import br.com.core.barbershop.repository.ClientRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Appointment Integration Tests")
class AppointmentIntegrationTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;
        @Autowired
        private AppointmentRepository appointmentRepository;
        @Autowired
        private BarberServiceRepository barberServiceRepository;
        @Autowired
        private ClientRepository clientRepository;
        @Autowired
        private BusinessHoursRepository businessHoursRepository;
        @Autowired
        private BlockedPeriodRepository blockedPeriodRepository;

        private BarberService testService;
        private Client testClient;

        @BeforeEach
        void setUp() {
                appointmentRepository.deleteAll();
                blockedPeriodRepository.deleteAll();
                businessHoursRepository.deleteAll();
                barberServiceRepository.deleteAll();
                clientRepository.deleteAll();

                // Create test service
                testService = barberServiceRepository.save(BarberService.builder()
                                .serviceName("Corte Masculino")
                                .description("Corte profissional")
                                .price(new BigDecimal("35.00"))
                                .durationMin(30)
                                .active(true)
                                .build());

                // Create test client linked to "client@test.com" (email used in @WithMockUser)
                testClient = clientRepository.save(Client.builder()
                                .username("Test Client")
                                .email("client@test.com")
                                .phone("44999999999")
                                .build());

                // Create business hours for all week days (08:00-12:00 + 13:00-20:00)
                for (DayOfWeek day : DayOfWeek.values()) {
                        businessHoursRepository.save(BusinessHours.builder()
                                        .dayOfWeek(day)
                                        .openTime(LocalTime.of(8, 0))
                                        .closeTime(LocalTime.of(12, 0))
                                        .active(true)
                                        .build());
                        businessHoursRepository.save(BusinessHours.builder()
                                        .dayOfWeek(day)
                                        .openTime(LocalTime.of(13, 0))
                                        .closeTime(LocalTime.of(20, 0))
                                        .active(true)
                                        .build());
                }
        }

        private Appointment createAppointmentInDb(AppointmentStatus status, LocalDateTime dateTime) {
                return appointmentRepository.save(Appointment.builder()
                                .client(testClient)
                                .service(testService)
                                .dateTime(dateTime)
                                .status(status)
                                .price(testService.getPrice())
                                .build());
        }

        // ============================================================
        // RBAC TESTS
        // ============================================================

        @Nested
        @DisplayName("RBAC — Access Control")
        class RbacTests {

                @Test
                @DisplayName("CLIENT should access my-history — 200")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void clientShouldAccessHistory() throws Exception {
                        mockMvc.perform(get("/api/appointments/my-history"))
                                        .andExpect(status().isOk());
                }

                @Test
                @DisplayName("OWNER should be FORBIDDEN from my-history — 403")
                @WithMockUser(roles = "OWNER")
                void ownerShouldNotAccessHistory() throws Exception {
                        mockMvc.perform(get("/api/appointments/my-history"))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("OWNER should access today endpoint — 200")
                @WithMockUser(roles = "OWNER")
                void ownerShouldAccessToday() throws Exception {
                        mockMvc.perform(get("/api/appointments/today"))
                                        .andExpect(status().isOk());
                }

                @Test
                @DisplayName("CLIENT should be FORBIDDEN from today — 403")
                @WithMockUser(roles = "CLIENT")
                void clientShouldNotAccessToday() throws Exception {
                        mockMvc.perform(get("/api/appointments/today"))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("OWNER should access revenue — 200")
                @WithMockUser(roles = "OWNER")
                void ownerShouldAccessRevenue() throws Exception {
                        mockMvc.perform(get("/api/appointments/revenue")
                                        .param("month", "4")
                                        .param("year", "2026"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.month").value(4))
                                        .andExpect(jsonPath("$.year").value(2026));
                }

                @Test
                @DisplayName("CLIENT should be FORBIDDEN from revenue — 403")
                @WithMockUser(roles = "CLIENT")
                void clientShouldNotAccessRevenue() throws Exception {
                        mockMvc.perform(get("/api/appointments/revenue")
                                        .param("month", "4")
                                        .param("year", "2026"))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Available slots should be accessible without auth — 200")
                void availableSlotsShouldBePublic() throws Exception {
                        LocalDate futureDate = LocalDate.now().plusDays(3);

                        mockMvc.perform(get("/api/appointments/available-slots")
                                        .param("date", futureDate.toString())
                                        .param("serviceId", testService.getId().toString()))
                                        .andExpect(status().isOk());
                }

                @Test
                @DisplayName("Unauthenticated user should NOT access today — 401")
                void unauthenticatedShouldNotAccessToday() throws Exception {
                        mockMvc.perform(get("/api/appointments/today"))
                                        .andExpect(status().isUnauthorized());
                }
        }

        // ============================================================
        // STATUS TRANSITION INTEGRATION TESTS
        // ============================================================

        @Nested
        @DisplayName("PATCH /api/appointments/{id}/status — State Machine")
        class StatusTransition {

                @Test
                @DisplayName("OWNER should transition PENDING → CONFIRMED — 200")
                @WithMockUser(roles = "OWNER")
                void shouldConfirm() throws Exception {
                        Appointment app = createAppointmentInDb(AppointmentStatus.PENDING,
                                        LocalDateTime.now().plusDays(1).withHour(10));

                        mockMvc.perform(patch("/api/appointments/" + app.getId() + "/status")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"status\": \"CONFIRMED\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("CONFIRMED"));
                }

                @Test
                @DisplayName("OWNER should transition CONFIRMED → IN_PROGRESS — 200")
                @WithMockUser(roles = "OWNER")
                void shouldStartProgress() throws Exception {
                        Appointment app = createAppointmentInDb(AppointmentStatus.CONFIRMED,
                                        LocalDateTime.now().plusDays(1).withHour(10));

                        mockMvc.perform(patch("/api/appointments/" + app.getId() + "/status")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"status\": \"IN_PROGRESS\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
                }

                @Test
                @DisplayName("OWNER should transition IN_PROGRESS → COMPLETED — 200")
                @WithMockUser(roles = "OWNER")
                void shouldComplete() throws Exception {
                        Appointment app = createAppointmentInDb(AppointmentStatus.IN_PROGRESS,
                                        LocalDateTime.now().plusDays(1).withHour(10));

                        mockMvc.perform(patch("/api/appointments/" + app.getId() + "/status")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"status\": \"COMPLETED\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("COMPLETED"));
                }

                @Test
                @DisplayName("PENDING → COMPLETED should FAIL — 400")
                @WithMockUser(roles = "OWNER")
                void shouldBlockInvalidTransition() throws Exception {
                        Appointment app = createAppointmentInDb(AppointmentStatus.PENDING,
                                        LocalDateTime.now().plusDays(1).withHour(10));

                        mockMvc.perform(patch("/api/appointments/" + app.getId() + "/status")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"status\": \"COMPLETED\"}"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.error").value("Business Rule Violation"));
                }

                @Test
                @DisplayName("CLIENT should be FORBIDDEN from updating status — 403")
                @WithMockUser(roles = "CLIENT")
                void clientShouldNotUpdateStatus() throws Exception {
                        Appointment app = createAppointmentInDb(AppointmentStatus.PENDING,
                                        LocalDateTime.now().plusDays(1).withHour(10));

                        mockMvc.perform(patch("/api/appointments/" + app.getId() + "/status")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"status\": \"CONFIRMED\"}"))
                                        .andExpect(status().isForbidden());
                }
        }

        // ============================================================
        // CANCEL INTEGRATION TESTS
        // ============================================================

        @Nested
        @DisplayName("PATCH /api/appointments/{id}/cancel")
        class CancelTests {

                @Test
                @DisplayName("CLIENT should cancel own appointment — 200")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void clientShouldCancel() throws Exception {
                        Appointment app = createAppointmentInDb(AppointmentStatus.PENDING,
                                        LocalDateTime.now().plusDays(1).withHour(10));

                        mockMvc.perform(patch("/api/appointments/" + app.getId() + "/cancel"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("CANCELLED"));
                }

                @Test
                @DisplayName("OWNER should cancel appointment — 200")
                @WithMockUser(roles = "OWNER")
                void ownerShouldCancel() throws Exception {
                        Appointment app = createAppointmentInDb(AppointmentStatus.CONFIRMED,
                                        LocalDateTime.now().plusDays(1).withHour(10));

                        mockMvc.perform(patch("/api/appointments/" + app.getId() + "/cancel"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("CANCELLED"));
                }

                @Test
                @DisplayName("Cannot cancel COMPLETED appointment — 400")
                @WithMockUser(roles = "OWNER")
                void cannotCancelCompleted() throws Exception {
                        Appointment app = createAppointmentInDb(AppointmentStatus.COMPLETED,
                                        LocalDateTime.now().plusDays(1).withHour(10));

                        mockMvc.perform(patch("/api/appointments/" + app.getId() + "/cancel"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message")
                                                        .value("Cannot cancel a completed appointment"));
                }

                @Test
                @DisplayName("Client should NOT cancel another client's appointment — 403")
                @WithMockUser(username = "intruso@email.com", roles = "CLIENT")
                void clientShouldNotCancelOthersAppointment() throws Exception {
                        Appointment alheio = createAppointmentInDb(AppointmentStatus.CONFIRMED,
                                        LocalDateTime.now().plusDays(1).withHour(10));

                        mockMvc.perform(patch("/api/appointments/" + alheio.getId() + "/cancel"))
                                        .andExpect(status().isForbidden());

                        // E o agendamento continua ativo.
                        assertThat(appointmentRepository.findById(alheio.getId()).orElseThrow().getStatus())
                                        .isEqualTo(AppointmentStatus.CONFIRMED);
                }
        }

        // ============================================================
        // REVENUE INTEGRATION TEST
        // ============================================================

        @Nested
        @DisplayName("GET /api/appointments/revenue")
        class RevenueTests {

                @Test
                @DisplayName("Should calculate revenue from completed appointments")
                @WithMockUser(roles = "OWNER")
                void shouldCalculateRevenue() throws Exception {
                        // Create 3 completed appointments in current month
                        LocalDateTime thisMonth = LocalDate.now().withDayOfMonth(1).atTime(10, 0);
                        createAppointmentInDb(AppointmentStatus.COMPLETED, thisMonth);
                        createAppointmentInDb(AppointmentStatus.COMPLETED, thisMonth.plusDays(1));
                        // This one is CANCELLED — should NOT be counted
                        createAppointmentInDb(AppointmentStatus.CANCELLED, thisMonth.plusDays(2));

                        int currentMonth = LocalDate.now().getMonthValue();
                        int currentYear = LocalDate.now().getYear();

                        mockMvc.perform(get("/api/appointments/revenue")
                                        .param("month", String.valueOf(currentMonth))
                                        .param("year", String.valueOf(currentYear)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.totalAppointments").value(2))
                                        .andExpect(jsonPath("$.totalRevenue").value(70.00));
                }
        }

        // ============================================================
        // TODAY APPOINTMENTS INTEGRATION TEST
        // ============================================================

        @Nested
        @DisplayName("GET /api/appointments/today")
        class TodayTests {

                @Test
                @DisplayName("Should return only today's appointments")
                @WithMockUser(roles = "OWNER")
                void shouldReturnTodayOnly() throws Exception {
                        // Today's appointment
                        createAppointmentInDb(AppointmentStatus.CONFIRMED, LocalDate.now().atTime(10, 0));
                        // Tomorrow's appointment — should NOT be included
                        createAppointmentInDb(AppointmentStatus.CONFIRMED, LocalDate.now().plusDays(1).atTime(10, 0));

                        mockMvc.perform(get("/api/appointments/today"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.length()").value(1))
                                        .andExpect(jsonPath("$[0].clientName").value("Test Client"));
                }
        }

        // ============================================================
        // FIND BY ID — OWNERSHIP
        // ============================================================

        @Nested
        @DisplayName("GET /api/appointments/{id} — ownership")
        class FindByIdOwnership {

                @Test
                @DisplayName("Client should NOT read another client's appointment — 403")
                @WithMockUser(username = "intruso@email.com", roles = "CLIENT")
                void clientShouldNotReadOthersAppointment() throws Exception {
                        // createAppointmentInDb vincula ao testClient (client@test.com).
                        Appointment alheio = createAppointmentInDb(AppointmentStatus.PENDING,
                                        LocalDate.now().plusDays(1).atTime(10, 0));

                        mockMvc.perform(get("/api/appointments/" + alheio.getId()))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Client should read their own appointment — 200")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void clientShouldReadOwnAppointment() throws Exception {
                        Appointment proprio = createAppointmentInDb(AppointmentStatus.PENDING,
                                        LocalDate.now().plusDays(1).atTime(10, 0));

                        mockMvc.perform(get("/api/appointments/" + proprio.getId()))
                                        .andExpect(status().isOk());
                }

                @Test
                @DisplayName("Owner should read any appointment — 200")
                @WithMockUser(username = "dono@email.com", roles = "OWNER")
                void ownerShouldReadAnyAppointment() throws Exception {
                        Appointment qualquer = createAppointmentInDb(AppointmentStatus.PENDING,
                                        LocalDate.now().plusDays(1).atTime(10, 0));

                        mockMvc.perform(get("/api/appointments/" + qualquer.getId()))
                                        .andExpect(status().isOk());
                }
        }

        // ============================================================
        // CICLO DE VIDA APOS O HORARIO PASSAR
        // ============================================================

        @Nested
        @DisplayName("Ciclo de vida apos o horario passar")
        class PastAppointmentLifecycle {

                @Test
                @DisplayName("Owner should complete an appointment whose time has already passed")
                @WithMockUser(roles = "OWNER")
                void shouldCompleteAppointmentInThePast() throws Exception {
                        // Agendamento de ontem, ja em atendimento — o caso real de "concluir depois".
                        Appointment passado = createAppointmentInDb(AppointmentStatus.IN_PROGRESS,
                                        LocalDate.now().minusDays(1).atTime(10, 0));

                        mockMvc.perform(patch("/api/appointments/" + passado.getId() + "/status")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"status\":\"COMPLETED\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("COMPLETED"));
                }
        }

        // ============================================================
        // FORMATO DOS SLOTS
        // ============================================================

        @Nested
        @DisplayName("GET /api/appointments/available-slots — formato")
        class SlotFormat {

                @Test
                @DisplayName("Available slots should be formatted as HH:mm")
                void slotsShouldBeFormattedWithoutSeconds() throws Exception {
                        // O fixture da classe abre 08:00-12:00 e 13:00-20:00 todo dia.
                        LocalDate amanha = LocalDate.now().plusDays(1);

                        mockMvc.perform(get("/api/appointments/available-slots")
                                        .param("date", amanha.toString())
                                        .param("serviceId", testService.getId().toString()))
                                        .andExpect(status().isOk())
                                        // "08:00", nao "08:00:00" — sem segundos.
                                        .andExpect(jsonPath("$[0]").value("08:00"))
                                        .andExpect(jsonPath("$[1]").value("08:30"));
                }
        }

        // ============================================================
        // POST /api/appointments — CRIACAO
        // ============================================================

        @Nested
        @DisplayName("POST /api/appointments")
        class CreateAppointment {

                /** Amanha as 10:00 — dentro do periodo 08:00-12:00 do fixture. */
                private LocalDateTime amanhaAs(int hora) {
                        return LocalDate.now().plusDays(1).atTime(hora, 0);
                }

                private String corpo(LocalDateTime quando) throws Exception {
                        return objectMapper.writeValueAsString(
                                        new AppointmentRequestDto(testService.getId(), "sem observacao", quando));
                }

                @Test
                @DisplayName("Client books an available slot — 201")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void clientShouldBook() throws Exception {
                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(amanhaAs(10))))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.status").value("PENDING"))
                                        .andExpect(jsonPath("$.serviceName").value("Corte Masculino"));

                        assertThat(appointmentRepository.findAll()).hasSize(1);
                }

                @Test
                @DisplayName("Provisions the client from the JWT claims on first booking - 201")
                @WithMockUser(username = "novo@email.com", roles = "CLIENT")
                void shouldProvisionClientOnFirstBooking() throws Exception {
                        // Sob WithMockUser o principal e uma String, entao name e phone
                        // vem nulos e o fallback e o proprio e-mail.
                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(amanhaAs(10))))
                                        .andExpect(status().isCreated());

                        assertThat(clientRepository.findByEmail("novo@email.com")).isPresent();
                }

                @Test
                @DisplayName("Rejects a slot already taken - 400")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void shouldRejectOverlappingSlot() throws Exception {
                        createAppointmentInDb(AppointmentStatus.CONFIRMED, amanhaAs(10));

                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(amanhaAs(10))))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message", containsString("already an appointment")));
                }

                @Test
                @DisplayName("Rejects a slot that overlaps only partially - 400")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void shouldRejectPartialOverlap() throws Exception {
                        // Servico de 30 min as 10:00 ocupa ate 10:30; pedir 10:15 colide.
                        createAppointmentInDb(AppointmentStatus.PENDING, amanhaAs(10));

                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(LocalDate.now().plusDays(1).atTime(10, 15))))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Allows a slot freed by a cancellation - 201")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void shouldAllowSlotOfCancelledAppointment() throws Exception {
                        // Cancelado nao ocupa a agenda.
                        createAppointmentInDb(AppointmentStatus.CANCELLED, amanhaAs(10));

                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(amanhaAs(10))))
                                        .andExpect(status().isCreated());
                }

                @Test
                @DisplayName("Rejects a time outside business hours - 400")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void shouldRejectOutsideBusinessHours() throws Exception {
                        // O fixture abre 08:00-12:00 e 13:00-20:00; 12:30 cai no almoco.
                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(LocalDate.now().plusDays(1).atTime(12, 30))))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message", containsString("business hours")));
                }

                @Test
                @DisplayName("Rejects a booking that would run past closing - 400")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void shouldRejectBookingPastClosing() throws Exception {
                        // 11:45 + 30 min ultrapassa o fechamento das 12:00: precisa caber inteiro.
                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(LocalDate.now().plusDays(1).atTime(11, 45))))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Rejects a day the barbershop is closed - 400")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void shouldRejectClosedDay() throws Exception {
                        businessHoursRepository.deleteAll();

                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(amanhaAs(10))))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message", containsString("closed")));
                }

                @Test
                @DisplayName("Rejects a time inside a blocked period - 400")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void shouldRejectBlockedPeriod() throws Exception {
                        blockedPeriodRepository.save(BlockedPeriod.builder()
                                        .startDateTime(amanhaAs(9))
                                        .endDateTime(amanhaAs(11))
                                        .reason("Manutencao")
                                        .build());

                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(amanhaAs(10))))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message", containsString("blocked")));
                }

                @Test
                @DisplayName("Rejects less than 30 minutes of lead time - 400")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void shouldRejectShortNotice() throws Exception {
                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(LocalDateTime.now().plusMinutes(10))))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message", containsString("30 minutes")));
                }

                @Test
                @DisplayName("Rejects an inactive service - 400")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void shouldRejectInactiveService() throws Exception {
                        testService.setActive(false);
                        barberServiceRepository.save(testService);

                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(amanhaAs(10))))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message", containsString("inactive")));
                }

                @Test
                @DisplayName("Rejects an unknown service - 404")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void shouldRejectUnknownService() throws Exception {
                        String payload = objectMapper.writeValueAsString(
                                        new AppointmentRequestDto(UUID.randomUUID(), null, amanhaAs(10)));

                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(payload))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("Rejects a date in the past at the DTO level - 400")
                @WithMockUser(username = "client@test.com", roles = "CLIENT")
                void shouldRejectPastDate() throws Exception {
                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(LocalDateTime.now().minusDays(1))))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Owner cannot book - 403")
                @WithMockUser(username = "dono@email.com", roles = "OWNER")
                void ownerShouldNotBook() throws Exception {
                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(amanhaAs(10))))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Unauthenticated cannot book - 401")
                void unauthenticatedShouldNotBook() throws Exception {
                        mockMvc.perform(post("/api/appointments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(amanhaAs(10))))
                                        .andExpect(status().isUnauthorized());
                }
        }
}
