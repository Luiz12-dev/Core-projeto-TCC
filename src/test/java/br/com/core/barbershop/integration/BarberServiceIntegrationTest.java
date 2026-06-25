package br.com.core.barbershop.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.core.barbershop.domain.BarberService;
import br.com.core.barbershop.dto.ServiceRequestDto;
import br.com.core.barbershop.repository.BarberServiceRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("BarberService Integration Tests")
class BarberServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BarberServiceRepository serviceRepository;

    @BeforeEach
    void setUp() {
        serviceRepository.deleteAll();
    }

    private ServiceRequestDto buildRequest(String name) {
        return new ServiceRequestDto(name, "Description for " + name, new BigDecimal("35.00"), 30);
    }

    private BarberService createServiceInDb(String name) {
        BarberService service = BarberService.builder()
                .serviceName(name)
                .description("Description for " + name)
                .price(new BigDecimal("35.00"))
                .durationMin(30)
                .active(true)
                .build();
        return serviceRepository.save(service);
    }

    // ============================================================
    // CREATE TESTS
    // ============================================================

    @Nested
    @DisplayName("POST /api/services")
    class CreateService {

        @Test
        @DisplayName("OWNER should create a service — 201")
        @WithMockUser(roles = "OWNER")
        void ownerShouldCreate() throws Exception {
            ServiceRequestDto req = buildRequest("Corte Masculino");

            mockMvc.perform(post("/api/services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.serviceName").value("Corte Masculino"))
                    .andExpect(jsonPath("$.price").value(35.00))
                    .andExpect(jsonPath("$.durationMin").value(30))
                    .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @DisplayName("CLIENT should be FORBIDDEN to create — 403")
        @WithMockUser(roles = "CLIENT")
        void clientShouldBeForbidden() throws Exception {
            ServiceRequestDto req = buildRequest("Corte");

            mockMvc.perform(post("/api/services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated should be UNAUTHORIZED — 401")
        void unauthenticatedShouldBeUnauthorized() throws Exception {
            ServiceRequestDto req = buildRequest("Corte");

            mockMvc.perform(post("/api/services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Duplicate service name should return 409")
        @WithMockUser(roles = "OWNER")
        void duplicateNameShouldConflict() throws Exception {
            createServiceInDb("Corte Masculino");

            ServiceRequestDto req = buildRequest("Corte Masculino");

            mockMvc.perform(post("/api/services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Duplicate Resource"));
        }

        @Test
        @DisplayName("Invalid request body should return 400")
        @WithMockUser(roles = "OWNER")
        void invalidRequestShouldFail() throws Exception {
            String invalidJson = """
                    {
                        "serviceName": "",
                        "description": "",
                        "price": -5,
                        "durationMin": 0
                    }
                    """;

            mockMvc.perform(post("/api/services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Error"));
        }
    }

    // ============================================================
    // READ TESTS
    // ============================================================

    @Nested
    @DisplayName("GET /api/services")
    class ReadServices {

        @Test
        @DisplayName("Should list active services without authentication")
        void shouldListWithoutAuth() throws Exception {
            createServiceInDb("Corte");
            createServiceInDb("Barba");

            mockMvc.perform(get("/api/services"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].serviceName").exists());
        }

        @Test
        @DisplayName("Should NOT list inactive services")
        void shouldNotListInactive() throws Exception {
            createServiceInDb("Ativo");
            BarberService inactive = createServiceInDb("Inativo");
            inactive.setActive(false);
            serviceRepository.save(inactive);

            mockMvc.perform(get("/api/services"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].serviceName").value("Ativo"));
        }

        @Test
        @DisplayName("Should find service by ID without authentication")
        void shouldFindById() throws Exception {
            BarberService service = createServiceInDb("Corte");

            mockMvc.perform(get("/api/services/" + service.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.serviceName").value("Corte"));
        }

        @Test
        @DisplayName("Non-existent ID should return 404")
        void shouldReturn404() throws Exception {
            mockMvc.perform(get("/api/services/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Resource Not Found"));
        }
    }

    // ============================================================
    // UPDATE TESTS
    // ============================================================

    @Nested
    @DisplayName("PUT /api/services/{id}")
    class UpdateService {

        @Test
        @DisplayName("OWNER should update service — 200")
        @WithMockUser(roles = "OWNER")
        void ownerShouldUpdate() throws Exception {
            BarberService service = createServiceInDb("Corte");
            ServiceRequestDto update = new ServiceRequestDto("Corte Premium", "VIP", new BigDecimal("50.00"), 45);

            mockMvc.perform(put("/api/services/" + service.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.serviceName").value("Corte Premium"))
                    .andExpect(jsonPath("$.price").value(50.00))
                    .andExpect(jsonPath("$.durationMin").value(45));
        }

        @Test
        @DisplayName("CLIENT should be FORBIDDEN to update — 403")
        @WithMockUser(roles = "CLIENT")
        void clientShouldBeForbidden() throws Exception {
            BarberService service = createServiceInDb("Corte");
            ServiceRequestDto update = new ServiceRequestDto("Corte Premium", "VIP", new BigDecimal("50.00"), 45);

            mockMvc.perform(put("/api/services/" + service.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // DELETE (SOFT DELETE) TESTS
    // ============================================================

    @Nested
    @DisplayName("DELETE /api/services/{id}")
    class DeleteService {

        @Test
        @DisplayName("OWNER should deactivate service (soft delete) — 204")
        @WithMockUser(roles = "OWNER")
        void ownerShouldDeactivate() throws Exception {
            BarberService service = createServiceInDb("Corte");

            mockMvc.perform(delete("/api/services/" + service.getId()))
                    .andExpect(status().isNoContent());

            // Verify it's still in DB but inactive
            BarberService deactivated = serviceRepository.findById(service.getId()).orElseThrow();
            assert !deactivated.getActive();
        }

        @Test
        @DisplayName("CLIENT should be FORBIDDEN to delete — 403")
        @WithMockUser(roles = "CLIENT")
        void clientShouldBeForbidden() throws Exception {
            BarberService service = createServiceInDb("Corte");

            mockMvc.perform(delete("/api/services/" + service.getId()))
                    .andExpect(status().isForbidden());
        }
    }
}
