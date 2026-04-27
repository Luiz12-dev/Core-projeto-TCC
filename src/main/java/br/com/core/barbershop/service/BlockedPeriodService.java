package br.com.core.barbershop.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.core.barbershop.domain.BlockedPeriod;
import br.com.core.barbershop.dto.BlockedPeriodRequestDto;
import br.com.core.barbershop.dto.BlockedPeriodResponseDto;
import br.com.core.barbershop.exception.BusinessRuleException;
import br.com.core.barbershop.exception.ResourceNotFoundException;
import br.com.core.barbershop.repository.BlockedPeriodRepository;

@Service
public class BlockedPeriodService {

    private final BlockedPeriodRepository blockedPeriodRepository;

    public BlockedPeriodService(BlockedPeriodRepository blockedPeriodRepository) {
        this.blockedPeriodRepository = blockedPeriodRepository;
    }

    @Transactional
    public BlockedPeriodResponseDto create(BlockedPeriodRequestDto req) {
        if (!req.startDateTime().isBefore(req.endDateTime())) {
            throw new BusinessRuleException("Start date must be before end date");
        }

        BlockedPeriod blocked = BlockedPeriod.builder()
            .startDateTime(req.startDateTime())
            .endDateTime(req.endDateTime())
            .reason(req.reason())
            .build();

        BlockedPeriod saved = blockedPeriodRepository.save(blocked);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BlockedPeriodResponseDto> findAllFuture() {
        return blockedPeriodRepository
            .findAllByEndDateTimeAfterOrderByStartDateTimeAsc(LocalDateTime.now())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void delete(UUID id) {
        if (!blockedPeriodRepository.existsById(id)) {
            throw new ResourceNotFoundException("Blocked period not found with ID: " + id);
        }
        blockedPeriodRepository.deleteById(id);
    }

    private BlockedPeriodResponseDto toResponse(BlockedPeriod bp) {
        return new BlockedPeriodResponseDto(
            bp.getId(),
            bp.getStartDateTime(),
            bp.getEndDateTime(),
            bp.getReason(),
            bp.getCreatedAt()
        );
    }
}
