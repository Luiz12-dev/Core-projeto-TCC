package br.com.core.barbershop.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.com.core.barbershop.domain.BlockedPeriod;
import br.com.core.barbershop.repository.BlockedPeriodRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("BlockedPeriod Integration Tests")
class BlockedPeriodIntegrationTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private BlockedPeriodRepository blockedPeriodRepository;

        @BeforeEach
        void setUp() {
                blockedPeriodRepository.deleteAll();
        }

        @Test
        @DisplayName("OWNER should create blocked period — 201")
        @WithMockUser(roles = "OWNER")
        void ownerShouldCreate() throws Exception {
                LocalDateTime start = LocalDateTime.now().plusDays(5);
                LocalDateTime end = start.plusHours(8);

                String json = """
                                {
                                    "startDateTime": "%s",
                                    "endDateTime": "%s",
                                    "reason": "Feriado Municipal"
                                }
                                """.formatted(start, end);

                mockMvc.perform(post("/api/blocked-periods")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.reason").value("Feriado Municipal"))
                                .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @DisplayName("CLIENT should be FORBIDDEN — 403")
        @WithMockUser(roles = "CLIENT")
        void clientShouldBeForbidden() throws Exception {
                LocalDateTime start = LocalDateTime.now().plusDays(5);
                LocalDateTime end = start.plusHours(8);

                String json = """
                                { "startDateTime": "%s", "endDateTime": "%s", "reason": "test" }
                                """.formatted(start, end);

                mockMvc.perform(post("/api/blocked-periods")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should list future blocked periods without auth — 200")
        void shouldListWithoutAuth() throws Exception {
                // Create a future blocked period directly in DB
                blockedPeriodRepository.save(BlockedPeriod.builder()
                                .startDateTime(LocalDateTime.now().plusDays(10))
                                .endDateTime(LocalDateTime.now().plusDays(11))
                                .reason("Feriado")
                                .build());

                mockMvc.perform(get("/api/blocked-periods"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("OWNER should delete blocked period — 204")
        @WithMockUser(roles = "OWNER")
        void ownerShouldDelete() throws Exception {
                BlockedPeriod blocked = blockedPeriodRepository.save(BlockedPeriod.builder()
                                .startDateTime(LocalDateTime.now().plusDays(10))
                                .endDateTime(LocalDateTime.now().plusDays(11))
                                .reason("Test")
                                .build());

                mockMvc.perform(delete("/api/blocked-periods/" + blocked.getId()))
                                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Start after end should FAIL — 400")
        @WithMockUser(roles = "OWNER")
        void shouldRejectInvalidDates() throws Exception {
                LocalDateTime end = LocalDateTime.now().plusDays(5);
                LocalDateTime start = end.plusDays(3);

                String json = """
                                { "startDateTime": "%s", "endDateTime": "%s", "reason": "test" }
                                """.formatted(start, end);

                mockMvc.perform(post("/api/blocked-periods")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest());
        }
}
