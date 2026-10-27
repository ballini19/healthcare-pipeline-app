package com.hospital.pipeline.booking;

/** What travels through appointmentQueue. */
public record BookingMessage(String referenceId, Long slotId, String patientName,
                             String patientEmail, String insuranceId, String reason) {
}
