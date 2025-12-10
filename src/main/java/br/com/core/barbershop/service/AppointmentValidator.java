package br.com.core.barbershop.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import br.com.core.barbershop.domain.BarberService;
import br.com.core.barbershop.dto.AppointmentRequestDto;
import br.com.core.barbershop.repository.AppointmentRepository;
import br.com.core.barbershop.repository.BarberServiceRepository;

@Component
public class AppointmentValidator {


    private final AppointmentRepository appRepository;

    private final BarberServiceRepository serviceRepository;

    public AppointmentValidator(AppointmentRepository appRepository, BarberServiceRepository serviceRepository) {
        this.appRepository = appRepository;
        this.serviceRepository = serviceRepository;
    }

    public void validate(AppointmentRequestDto req){

        LocalDateTime appointmentDate = req.dateTime();


        if(appointmentDate.isBefore(LocalDateTime.now().plusMinutes(30))){
            throw new RuntimeException("The appointment date must respect 30 min of cooldown");
        }

        if(appointmentDate.getHour() < 8 || appointmentDate.getHour() > 18){
            throw new RuntimeException("The appointmente date must be after 8am and before 18pm ");
        }
        BarberService service = serviceRepository.findById(req.serviceId())
        .orElseThrow(()-> new RuntimeException("Service not found"));

        LocalDateTime startTime = appointmentDate;

        LocalDateTime endTime = appointmentDate.plusMinutes(service.getDurationMin());

        boolean hasConflict = appRepository.existsConflictingAppointment(startTime, endTime);

        if(hasConflict){
            throw new RuntimeException("Time slot not available! aready exists an apoointment");
        }
    }

    

}
