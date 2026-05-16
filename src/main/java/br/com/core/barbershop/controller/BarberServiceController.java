package br.com.core.barbershop.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.core.barbershop.dto.ServiceRequestDto;
import br.com.core.barbershop.dto.ServiceResponseDto;
import br.com.core.barbershop.service.BarberServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/services")
@Tag(name = "Services", description = "Barbershop service catalog management")
public class BarberServiceController {

    private final BarberServiceService barberServiceService;

    public BarberServiceController(BarberServiceService barberServiceService) {
        this.barberServiceService = barberServiceService;
    }

    @Operation(summary = "Create a new service", description = "OWNER only — registers a new service in the catalog")
    @ApiResponse(responseCode = "201", description = "Service created successfully")
    @ApiResponse(responseCode = "409", description = "Service name already exists")
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ServiceResponseDto> create(@RequestBody @Valid ServiceRequestDto req) {
        ServiceResponseDto service = barberServiceService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(service);
    }

    @Operation(summary = "List active services", description = "Public — returns all active services")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<ServiceResponseDto>> findAll() {
        List<ServiceResponseDto> services = barberServiceService.findAllActive();
        return ResponseEntity.ok(services);
    }

    @Operation(summary = "Find service by ID", description = "Public — returns service details")
    @ApiResponse(responseCode = "404", description = "Service not found")
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponseDto> findById(@PathVariable UUID id) {
        ServiceResponseDto service = barberServiceService.findById(id);
        return ResponseEntity.ok(service);
    }

    @Operation(summary = "Update a service", description = "OWNER only — updates service details")
    @ApiResponse(responseCode = "404", description = "Service not found")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ServiceResponseDto> update(@PathVariable UUID id, @RequestBody @Valid ServiceRequestDto req) {
        ServiceResponseDto updated = barberServiceService.update(id, req);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Deactivate a service", description = "OWNER only — soft delete (marks as inactive)")
    @ApiResponse(responseCode = "204", description = "Service deactivated")
    @ApiResponse(responseCode = "404", description = "Service not found")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        barberServiceService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
