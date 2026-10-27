package com.hospital.pipeline.repo;

import com.hospital.pipeline.domain.Slot;
import com.hospital.pipeline.domain.SlotStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByDoctorIdAndStatusOrderByStartTime(Long doctorId, SlotStatus status);

    Optional<Slot> findFirstByDoctorIdAndStatusAndStartTimeAfterOrderByStartTime(
            Long doctorId, SlotStatus status, LocalDateTime after);

    long countByStatus(SlotStatus status);
}
