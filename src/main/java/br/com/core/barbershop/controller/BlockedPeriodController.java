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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.core.barbershop.dto.BlockedPeriodRequestDto;
import br.com.core.barbershop.dto.BlockedPeriodResponseDto;
import br.com.core.barbershop.service.BlockedPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/blocked-periods")
@Tag(name = "Blocked Periods", description = "Owner schedule blocking management")
public class BlockedPeriodController {

    private final BlockedPeriodService blockedPeriodService;

    public BlockedPeriodController(BlockedPeriodService blockedPeriodService) {
        this.blockedPeriodService = blockedPeriodService;
    }

    @Operation(summary = "Block a time period", description = "OWNER only — blocks appointments during a specific period (holidays, days off, etc.)")
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BlockedPeriodResponseDto> create(@RequestBody @Valid BlockedPeriodRequestDto req) {
        BlockedPeriodResponseDto blocked = blockedPeriodService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(blocked);
    }

    @Operation(summary = "List future blocked periods", description = "Public — returns all upcoming blocked periods")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<BlockedPeriodResponseDto>> findAll() {
        List<BlockedPeriodResponseDto> all = blockedPeriodService.findAllFuture();
        return ResponseEntity.ok(all);
    }

    @Operation(summary = "Remove a blocked period", description = "OWNER only — removes a schedule block")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        blockedPeriodService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
