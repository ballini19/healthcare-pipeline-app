package com.hospital.pipeline.booking;

import com.hospital.pipeline.domain.Appointment;
import java.time.Instant;
import java.time.LocalDateTime;

/** What the portals see of a confirmed appointment. */
public record AppointmentView(String referenceId, String patientName, String patientEmail,
                              Long doctorId, String doctorName, String specialty,
                              LocalDateTime startTime, String reason, Instant confirmedAt) {

    public static AppointmentView from(Appointment a) {
        return new AppointmentView(a.referenceId, a.patientName, a.patientEmail,
                a.slot.doctor.id, a.slot.doctor.name, a.slot.doctor.specialty,
                a.slot.startTime, a.reason, a.confirmedAt);
    }
}
