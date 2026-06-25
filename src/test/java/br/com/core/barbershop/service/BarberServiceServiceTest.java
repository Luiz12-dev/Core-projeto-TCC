package br.com.core.barbershop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import br.com.core.barbershop.domain.BarberService;
import br.com.core.barbershop.dto.ServiceRequestDto;
import br.com.core.barbershop.dto.ServiceResponseDto;
import br.com.core.barbershop.exception.DuplicateResourceException;
import br.com.core.barbershop.exception.ResourceNotFoundException;
import br.com.core.barbershop.repository.BarberServiceRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BarberServiceService Unit Tests")
class BarberServiceServiceTest {

    @Mock
    private BarberServiceRepository serviceRepository;

    @InjectMocks
    private BarberServiceService barberServiceService;

    private BarberService buildService(String name, BigDecimal price, Integer duration) {
        return BarberService.builder()
            .id(UUID.randomUUID())
            .serviceName(name)
            .description("Test description for " + name)
            .price(price)
            .durationMin(duration)
            .active(true)
            .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Should create a new service successfully")
        void shouldCreateServiceSuccessfully() {
            ServiceRequestDto req = new ServiceRequestDto("Corte Masculino", "Corte na máquina e tesoura", new BigDecimal("35.00"), 30);
            BarberService saved = buildService("Corte Masculino", new BigDecimal("35.00"), 30);

            when(serviceRepository.existsByServiceName("Corte Masculino")).thenReturn(false);
            when(serviceRepository.save(any(BarberService.class))).thenReturn(saved);

            ServiceResponseDto result = barberServiceService.create(req);

            assertThat(result).isNotNull();
            assertThat(result.serviceName()).isEqualTo("Corte Masculino");
            assertThat(result.price()).isEqualByComparingTo(new BigDecimal("35.00"));
            assertThat(result.durationMin()).isEqualTo(30);
            verify(serviceRepository).save(any(BarberService.class));
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when service name already exists")
        void shouldThrowWhenDuplicateName() {
            ServiceRequestDto req = new ServiceRequestDto("Corte Masculino", "desc", new BigDecimal("35.00"), 30);

            when(serviceRepository.existsByServiceName("Corte Masculino")).thenReturn(true);

            assertThatThrownBy(() -> barberServiceService.create(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

            verify(serviceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findAllActive()")
    class FindAllActive {

        @Test
        @DisplayName("Should return only active services")
        void shouldReturnActiveServices() {
            List<BarberService> activeServices = List.of(
                buildService("Corte", new BigDecimal("35.00"), 30),
                buildService("Barba", new BigDecimal("25.00"), 20)
            );

            when(serviceRepository.findAllByActiveTrue()).thenReturn(activeServices);

            List<ServiceResponseDto> result = barberServiceService.findAllActive();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).serviceName()).isEqualTo("Corte");
            assertThat(result.get(1).serviceName()).isEqualTo("Barba");
        }

        @Test
        @DisplayName("Should return empty list when no active services")
        void shouldReturnEmptyWhenNoActiveServices() {
            when(serviceRepository.findAllByActiveTrue()).thenReturn(List.of());

            List<ServiceResponseDto> result = barberServiceService.findAllActive();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Should find service by ID")
        void shouldFindById() {
            UUID id = UUID.randomUUID();
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);

            when(serviceRepository.findById(id)).thenReturn(Optional.of(service));

            ServiceResponseDto result = barberServiceService.findById(id);

            assertThat(result).isNotNull();
            assertThat(result.serviceName()).isEqualTo("Corte");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ID not found")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(serviceRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> barberServiceService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Should update service successfully")
        void shouldUpdateSuccessfully() {
            UUID id = UUID.randomUUID();
            BarberService existing = buildService("Corte", new BigDecimal("35.00"), 30);
            ServiceRequestDto req = new ServiceRequestDto("Corte Premium", "Corte VIP", new BigDecimal("50.00"), 45);

            BarberService updated = buildService("Corte Premium", new BigDecimal("50.00"), 45);

            when(serviceRepository.findById(id)).thenReturn(Optional.of(existing));
            when(serviceRepository.save(any(BarberService.class))).thenReturn(updated);

            ServiceResponseDto result = barberServiceService.update(id, req);

            assertThat(result.serviceName()).isEqualTo("Corte Premium");
            assertThat(result.price()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.durationMin()).isEqualTo(45);
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        @Test
        @DisplayName("Should deactivate service (soft delete)")
        void shouldDeactivateService() {
            UUID id = UUID.randomUUID();
            BarberService service = buildService("Corte", new BigDecimal("35.00"), 30);

            when(serviceRepository.findById(id)).thenReturn(Optional.of(service));
            when(serviceRepository.save(any(BarberService.class))).thenReturn(service);

            barberServiceService.deactivate(id);

            assertThat(service.getActive()).isFalse();
            verify(serviceRepository).save(service);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ID not found")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(serviceRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> barberServiceService.deactivate(id))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
