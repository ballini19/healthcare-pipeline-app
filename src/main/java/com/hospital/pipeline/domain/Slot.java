package com.hospital.pipeline.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/** A bookable time slot. The version column is what makes concurrent bookings safe. */
@Entity
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    public Doctor doctor;

    public LocalDateTime startTime;

    @Enumerated(EnumType.STRING)
    public SlotStatus status = SlotStatus.AVAILABLE;

    @Version
    public long version;

    protected Slot() {
    }

    public Slot(Doctor doctor, LocalDateTime startTime) {
        this.doctor = doctor;
        this.startTime = startTime;
    }
}
