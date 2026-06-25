package br.com.core.barbershop.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import br.com.core.barbershop.repository.BusinessHoursRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("BusinessHours Integration Tests")
class BusinessHoursIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BusinessHoursRepository businessHoursRepository;

    @BeforeEach
    void setUp() {
        businessHoursRepository.deleteAll();
    }

    @Test
    @DisplayName("OWNER should create business hours — 201")
    @WithMockUser(roles = "OWNER")
    void ownerShouldCreate() throws Exception {
        String json = """
                {
                    "dayOfWeek": "MONDAY",
                    "openTime": "08:00",
                    "closeTime": "12:00"
                }
                """;

        mockMvc.perform(post("/api/business-hours")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.openTime").value("08:00:00"))
                .andExpect(jsonPath("$.closeTime").value("12:00:00"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("CLIENT should be FORBIDDEN — 403")
    @WithMockUser(roles = "CLIENT")
    void clientShouldBeForbidden() throws Exception {
        String json = """
                { "dayOfWeek": "MONDAY", "openTime": "08:00", "closeTime": "12:00" }
                """;

        mockMvc.perform(post("/api/business-hours")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should list business hours without auth — 200")
    void shouldListWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/business-hours"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Opening time after closing time should FAIL — 400")
    @WithMockUser(roles = "OWNER")
    void shouldRejectInvalidHours() throws Exception {
        String json = """
                { "dayOfWeek": "MONDAY", "openTime": "18:00", "closeTime": "08:00" }
                """;

        mockMvc.perform(post("/api/business-hours")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Opening time must be before closing time"));
    }
}
