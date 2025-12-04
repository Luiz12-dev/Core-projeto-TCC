package br.com.core.barbershop.service;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

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


    public ServiceResponseDto creatService(ServiceRequestDto req){

        if(serviceRepository.exiexistsByServiceName(req.serviceName())){
            throw new RuntimeException("Service already registred");
        }

        BarberService newService = new BarberService();

        newService.setServiceName(req.serviceName());
        newService.setDescription(req.description());
        newService.setDurationMin(req.durationMin());
        newService.setValue(req.value());

        BarberService savedService = serviceRepository.save(newService);

       return toServiceResponseDTO(savedService);

    }

    public List<ServiceResponseDto> findAll(){

        return serviceRepository.findAll().stream()
        .map(this::toServiceResponseDTO)
        .collect(Collectors.toList());
        
    }

    public ServiceResponseDto update(UUID id,ServiceRequestDto req){

        BarberService service = serviceRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Service not found for update"));

        service.setDescription(req.description());
        service.setDurationMin(req.durationMin());
        service.setServiceName(req.serviceName());
        service.setValue(req.value());

        BarberService savedService = serviceRepository.save(service);

        return toServiceResponseDTO(savedService);
    }

    public ServiceResponseDto findServiceById(UUID id){
        BarberService service = serviceRepository.findById(id)
        .orElseThrow(()-> new EntityNotFoundException("Service id not found"));

        return toServiceResponseDTO(service);
    }


    public void removeService(UUID id){
        if(!serviceRepository.existsById(id)){
            throw new EntityNotFoundException("Service not found for delete");
        }

        serviceRepository.deleteById(id);
    }


    private ServiceResponseDto toServiceResponseDTO(BarberService barberService){
        return new ServiceResponseDto(
                barberService.getId(),
                barberService.getServiceName(),
                barberService.getDescription(),
                barberService.getValue(),
                barberService.getDurationMin()
        );

    }
}

