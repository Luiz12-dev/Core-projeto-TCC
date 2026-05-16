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

import br.com.core.barbershop.dto.BusinessHoursRequestDto;
import br.com.core.barbershop.dto.BusinessHoursResponseDto;
import br.com.core.barbershop.service.BusinessHoursService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/business-hours")
@Tag(name = "Business Hours", description = "Barbershop operating hours management")
public class BusinessHoursController {

    private final BusinessHoursService businessHoursService;

    public BusinessHoursController(BusinessHoursService businessHoursService) {
        this.businessHoursService = businessHoursService;
    }

    @Operation(summary = "Add business hours", description = "OWNER only — adds a working period for a day of week")
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BusinessHoursResponseDto> create(@RequestBody @Valid BusinessHoursRequestDto req) {
        BusinessHoursResponseDto bh = businessHoursService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(bh);
    }

    @Operation(summary = "List business hours", description = "Public — returns all active business hours")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<BusinessHoursResponseDto>> findAll() {
        List<BusinessHoursResponseDto> all = businessHoursService.findAll();
        return ResponseEntity.ok(all);
    }

    @Operation(summary = "Update business hours", description = "OWNER only — updates a specific business hours entry")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BusinessHoursResponseDto> update(@PathVariable UUID id, @RequestBody @Valid BusinessHoursRequestDto req) {
        BusinessHoursResponseDto updated = businessHoursService.update(id, req);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Remove business hours", description = "OWNER only — soft delete")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        businessHoursService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
