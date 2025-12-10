package br.com.core.barbershop.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.core.barbershop.dto.AppointmentRequestDto;
import br.com.core.barbershop.dto.AppointmentResponseDto;
import br.com.core.barbershop.service.AppointmentService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/agendamentos")
public class AppointmentController {

    private final AppointmentService appService;

    public AppointmentController(AppointmentService appService) {
        this.appService = appService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> createApp(@RequestBody @Valid AppointmentRequestDto req){
        AppointmentResponseDto newApp = appService.create(req);

        return new ResponseEntity<>(newApp, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDto>> allApp(){
        List<AppointmentResponseDto> all = appService.findAll();

        return ResponseEntity.ok(all);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDto> findById(@PathVariable UUID id){
        AppointmentResponseDto app = appService.findAppById(id);

        return ResponseEntity.ok(app);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancellApp(@PathVariable UUID id){

        appService.cancelApp(id);

        return ResponseEntity.noContent().build();
    }
}
