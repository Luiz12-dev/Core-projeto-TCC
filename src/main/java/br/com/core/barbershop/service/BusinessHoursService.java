package br.com.core.barbershop.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.core.barbershop.domain.BusinessHours;
import br.com.core.barbershop.dto.BusinessHoursRequestDto;
import br.com.core.barbershop.dto.BusinessHoursResponseDto;
import br.com.core.barbershop.exception.BusinessRuleException;
import br.com.core.barbershop.exception.ResourceNotFoundException;
import br.com.core.barbershop.repository.BusinessHoursRepository;

@Service
public class BusinessHoursService {

    private final BusinessHoursRepository businessHoursRepository;

    public BusinessHoursService(BusinessHoursRepository businessHoursRepository) {
        this.businessHoursRepository = businessHoursRepository;
    }

    @Transactional
    public BusinessHoursResponseDto create(BusinessHoursRequestDto req) {
        if (!req.openTime().isBefore(req.closeTime())) {
            throw new BusinessRuleException("Opening time must be before closing time");
        }

        BusinessHours businessHours = BusinessHours.builder()
            .dayOfWeek(req.dayOfWeek())
            .openTime(req.openTime())
            .closeTime(req.closeTime())
            .build();

        BusinessHours saved = businessHoursRepository.save(businessHours);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BusinessHoursResponseDto> findAll() {
        return businessHoursRepository.findAllByActiveTrue().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public BusinessHoursResponseDto update(UUID id, BusinessHoursRequestDto req) {
        BusinessHours bh = businessHoursRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Business hours not found with ID: " + id));

        if (!req.openTime().isBefore(req.closeTime())) {
            throw new BusinessRuleException("Opening time must be before closing time");
        }

        bh.setDayOfWeek(req.dayOfWeek());
        bh.setOpenTime(req.openTime());
        bh.setCloseTime(req.closeTime());

        BusinessHours updated = businessHoursRepository.save(bh);
        return toResponse(updated);
    }

    @Transactional
    public void delete(UUID id) {
        BusinessHours bh = businessHoursRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Business hours not found with ID: " + id));

        bh.setActive(false);
        businessHoursRepository.save(bh);
    }

    private BusinessHoursResponseDto toResponse(BusinessHours bh) {
        return new BusinessHoursResponseDto(
            bh.getId(),
            bh.getDayOfWeek(),
            bh.getOpenTime(),
            bh.getCloseTime(),
            bh.getActive()
        );
    }
}
