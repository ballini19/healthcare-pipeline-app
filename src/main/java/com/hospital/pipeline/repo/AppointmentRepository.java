package com.hospital.pipeline.repo;

import com.hospital.pipeline.domain.Appointment;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findBySlotId(Long slotId);

    List<Appointment> findAllByOrderByIdDesc(Pageable page);
}
