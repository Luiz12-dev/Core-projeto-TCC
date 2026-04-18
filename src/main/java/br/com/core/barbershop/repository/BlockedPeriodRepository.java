package br.com.core.barbershop.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.core.barbershop.domain.BlockedPeriod;

public interface BlockedPeriodRepository extends JpaRepository<BlockedPeriod, UUID> {

    @Query("SELECT COUNT(b) > 0 FROM BlockedPeriod b WHERE b.startDateTime < :endTime AND b.endDateTime > :startTime")
    boolean existsConflictingBlock(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    List<BlockedPeriod> findAllByEndDateTimeAfterOrderByStartDateTimeAsc(LocalDateTime now);
}
