package com.hospital.pipeline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

@Entity
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false)
    public String referenceId;

    public String patientName;
    public String patientEmail;
    public String insuranceId;
    public String reason;

    @ManyToOne(optional = false)
    public Slot slot;

    public Instant confirmedAt = Instant.now();

    protected Appointment() {
    }

    public Appointment(String referenceId, String patientName, String patientEmail,
                       String insuranceId, String reason, Slot slot) {
        this.referenceId = referenceId;
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.insuranceId = insuranceId;
        this.reason = reason;
        this.slot = slot;
    }
}
