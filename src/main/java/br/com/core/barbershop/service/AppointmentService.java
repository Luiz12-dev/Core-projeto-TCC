package br.com.core.barbershop.service;



import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.com.core.barbershop.domain.Appointment;
import br.com.core.barbershop.domain.BarberService;
import br.com.core.barbershop.domain.Client;
import br.com.core.barbershop.dto.AppointmentRequestDto;
import br.com.core.barbershop.dto.AppointmentResponseDto;
import br.com.core.barbershop.enuns.AppointmentStatus;
import br.com.core.barbershop.repository.AppointmentRepository;
import br.com.core.barbershop.repository.BarberServiceRepository;
import br.com.core.barbershop.repository.ClientRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentValidator validator;
    private final ClientRepository clientRepository;
    private final BarberServiceRepository serviceRepository;

    public AppointmentService(AppointmentRepository appointmentRepository, AppointmentValidator validator, ClientRepository clientRepository, BarberServiceRepository serviceRepository) {
        this.appointmentRepository = appointmentRepository;
        this.validator = validator;
        this.clientRepository = clientRepository;
        this.serviceRepository = serviceRepository;
    }

    public AppointmentResponseDto create(AppointmentRequestDto req){

        validator.validate(req);
        
        Client client = clientRepository.findById(req.clientId())
            .orElseThrow(()-> new EntityNotFoundException("Client not found"));

        BarberService service = serviceRepository.findById(req.serviceId())
            .orElseThrow(()-> new EntityNotFoundException("Service not found"));


        Appointment newAppointment = Appointment.builder()
        .client(client)
        .service(service)
        .dateTime(req.dateTime())
        .observation(req.observation())
        .status(AppointmentStatus.PENDENT)
        .price(service.getPrice())
        .build();

        Appointment savedApp = appointmentRepository.save(newAppointment);

        return toResponse(savedApp);
    }

    public List<AppointmentResponseDto> findAll(){

        return appointmentRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
    }

    public AppointmentResponseDto findAppById(UUID id){

        Appointment app = appointmentRepository.findById(id)
            .orElseThrow(()-> new EntityNotFoundException("Appointment not found"));

        return toResponse(app);
    }

    public void cancelApp(UUID id){

        Appointment app = appointmentRepository.findById(id)
            .orElseThrow(()-> new EntityNotFoundException("Appointment not found"));

         app.setStatus(AppointmentStatus.CANCELLED);   

         appointmentRepository.save(app);

    }
    public AppointmentResponseDto toResponse(Appointment app){
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
            app.getPrice()
        );
    }






    

}
