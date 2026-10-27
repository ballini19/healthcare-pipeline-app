package com.hospital.pipeline.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedQuery;
import java.time.Instant;

/** Base of the clinical test types. One table, one subclass per kind of test. */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "test_type")
@NamedQuery(name = "LabResult.findBySeverity",
        query = "select r from LabResult r where r.severity = :severity order by r.id desc")
public abstract class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String patientId;
    public String testName;

    @jakarta.persistence.Column(name = "result_value")
    public double value;

    public String unit;
    public double refLow;
    public double refHigh;

    @Enumerated(EnumType.STRING)
    public Severity severity;

    public Instant recordedAt = Instant.now();

    @JsonProperty("category")
    public abstract String category();
}
