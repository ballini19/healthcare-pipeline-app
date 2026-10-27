package com.hospital.pipeline.repo;

import com.hospital.pipeline.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
}
