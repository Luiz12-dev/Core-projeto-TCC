package br.com.core.barbershop.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.core.barbershop.domain.BarberService;
import br.com.core.barbershop.dto.ServiceRequestDto;
import br.com.core.barbershop.dto.ServiceResponseDto;
import br.com.core.barbershop.exception.DuplicateResourceException;
import br.com.core.barbershop.exception.ResourceNotFoundException;
import br.com.core.barbershop.repository.BarberServiceRepository;

@Service
public class BarberServiceService {

    private final BarberServiceRepository serviceRepository;

    public BarberServiceService(BarberServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Transactional
    public ServiceResponseDto create(ServiceRequestDto req) {
        if (serviceRepository.existsByServiceName(req.serviceName())) {
            throw new DuplicateResourceException("Service '" + req.serviceName() + "' already exists");
        }

        BarberService newService = BarberService.builder()
            .serviceName(req.serviceName())
            .description(req.description())
            .price(req.price())
            .durationMin(req.durationMin())
            .build();

        BarberService savedService = serviceRepository.save(newService);
        return toResponse(savedService);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponseDto> findAllActive() {
        return serviceRepository.findAllByActiveTrue().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponseDto findById(UUID id) {
        BarberService service = serviceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + id));
        return toResponse(service);
    }

    @Transactional
    public ServiceResponseDto update(UUID id, ServiceRequestDto req) {
        BarberService service = serviceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + id));

        service.setServiceName(req.serviceName());
        service.setDescription(req.description());
        service.setPrice(req.price());
        service.setDurationMin(req.durationMin());

        BarberService updatedService = serviceRepository.save(service);
        return toResponse(updatedService);
    }

    @Transactional
    public void deactivate(UUID id) {
        BarberService service = serviceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + id));

        service.setActive(false);
        serviceRepository.save(service);
    }

    private ServiceResponseDto toResponse(BarberService service) {
        return new ServiceResponseDto(
            service.getId(),
            service.getServiceName(),
            service.getDescription(),
            service.getPrice(),
            service.getDurationMin()
        );
    }
}
