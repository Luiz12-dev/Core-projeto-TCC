package br.com.core.barbershop.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.core.barbershop.dto.AppointmentRequestDto;
import br.com.core.barbershop.dto.AppointmentResponseDto;
import br.com.core.barbershop.dto.RevenueResponseDto;
import br.com.core.barbershop.dto.UpdateStatusRequestDto;
import br.com.core.barbershop.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "Appointment scheduling and management")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Operation(summary = "Book an appointment", description = "CLIENT only — creates a new appointment linked to the authenticated user")
    @ApiResponse(responseCode = "201", description = "Appointment booked successfully")
    @ApiResponse(responseCode = "400", description = "Business rule violation (conflict, closed, blocked)")
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<AppointmentResponseDto> create(
            @RequestBody @Valid AppointmentRequestDto req,
            Principal principal) {

        AppointmentResponseDto appointment = appointmentService.create(req, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    @Operation(summary = "My appointment history", description = "CLIENT only — returns all appointments for the authenticated user")
    @GetMapping("/my-history")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<AppointmentResponseDto>> myHistory(Principal principal) {
        List<AppointmentResponseDto> history = appointmentService.getMyHistory(principal.getName());
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Available time slots", description = "Public — returns available slots for a given date and service")
    @SecurityRequirements
    @GetMapping("/available-slots")
    public ResponseEntity<List<java.time.LocalTime>> availableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam UUID serviceId) {

        List<java.time.LocalTime> slots = appointmentService.getAvailableSlots(date, serviceId);
        return ResponseEntity.ok(slots);
    }

    @Operation(summary = "Today's appointments", description = "OWNER only — returns all appointments scheduled for today")
    @GetMapping("/today")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<AppointmentResponseDto>> todayAppointments() {
        List<AppointmentResponseDto> today = appointmentService.getTodayAppointments();
        return ResponseEntity.ok(today);
    }

    @Operation(summary = "Monthly revenue", description = "OWNER only — returns total revenue for a given month/year (completed appointments)")
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<RevenueResponseDto> monthlyRevenue(
            @RequestParam int month,
            @RequestParam int year) {

        RevenueResponseDto revenue = appointmentService.getMonthlyRevenue(month, year);
        return ResponseEntity.ok(revenue);
    }

    @Operation(summary = "Update appointment status", description = "OWNER only — transitions: PENDING→CONFIRMED→IN_PROGRESS→COMPLETED")
    @ApiResponse(responseCode = "400", description = "Invalid status transition")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<AppointmentResponseDto> updateStatus(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateStatusRequestDto req) {

        AppointmentResponseDto updated = appointmentService.updateStatus(id, req.status());
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Cancel appointment", description = "CLIENT or OWNER — cancels an appointment (cannot cancel completed)")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CLIENT', 'OWNER')")
    public ResponseEntity<AppointmentResponseDto> cancel(@PathVariable UUID id) {
        AppointmentResponseDto cancelled = appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(cancelled);
    }

    @Operation(summary = "List all appointments", description = "OWNER only — returns all appointments")
    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<AppointmentResponseDto>> findAll() {
        List<AppointmentResponseDto> all = appointmentService.findAll();
        return ResponseEntity.ok(all);
    }

    @Operation(summary = "Find appointment by ID", description = "Authenticated users only")
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDto> findById(@PathVariable UUID id) {
        AppointmentResponseDto appointment = appointmentService.findById(id);
        return ResponseEntity.ok(appointment);
    }
}
