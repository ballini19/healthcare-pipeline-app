package com.hospital.pipeline.repo;

import com.hospital.pipeline.domain.LabResult;
import com.hospital.pipeline.domain.Severity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface LabResultRepository extends JpaRepository<LabResult, Long> {
    /** Resolved to the @NamedQuery declared on LabResult. */
    List<LabResult> findBySeverity(@Param("severity") Severity severity);

    List<LabResult> findAllByOrderByIdDesc(Pageable page);

    long countBySeverity(Severity severity);
}
