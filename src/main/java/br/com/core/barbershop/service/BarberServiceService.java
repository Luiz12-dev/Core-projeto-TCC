package br.com.core.barbershop.service;



import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.core.barbershop.domain.BarberService;
import br.com.core.barbershop.dto.ServiceRequestDto;
import br.com.core.barbershop.dto.ServiceResponseDto;
import br.com.core.barbershop.repository.BarberServiceRepository;
import jakarta.persistence.EntityNotFoundException;


@Service
public class BarberServiceService {

    private final BarberServiceRepository serviceRepository;

    public BarberServiceService(BarberServiceRepository serviceRepository){
        this.serviceRepository = serviceRepository;
    }


    @Transactional
    public ServiceResponseDto create(ServiceRequestDto req){

        if(serviceRepository.findByServiceName(req.serviceName()).isPresent()){
            throw new RuntimeException("Service name already registred");
        }

        BarberService newService = new BarberService();
        newService.setServiceName(req.serviceName());
        newService.setDescription(req.description());
        newService.setDurationMin(req.durationMin());
        newService.setPrice(req.price());

        BarberService savedService = serviceRepository.save(newService);

        return toResponse(savedService);
    }

    public List<ServiceResponseDto> findAll(){
        return serviceRepository.findAll().stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
    }

    public ServiceResponseDto findById(UUID id){

        BarberService service = serviceRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Service not found"));

        return toResponse(service);
    }


    @Transactional
    public ServiceResponseDto update(UUID id, ServiceRequestDto req){

        BarberService service = serviceRepository.findById(id)
        .orElseThrow(()-> new EntityNotFoundException("Service id not found"));

        service.setServiceName(req.serviceName());
        service.setDescription(req.description());
        service.setPrice(req.price());
        service.setDurationMin(req.durationMin());

        BarberService updatedService = serviceRepository.save(service);

        return toResponse(updatedService);
    }



    public void remove(UUID id){

        if(!serviceRepository.existsById(id)){
            throw new EntityNotFoundException("Entity not found");
        }

        serviceRepository.deleteById(id);
    }



    private ServiceResponseDto toResponse(BarberService req){
        return new ServiceResponseDto(
            req.getId(),
            req.getServiceName(),
            req.getDescription(),
            req.getPrice(),
            req.getDurationMin()
        );
    }


}

