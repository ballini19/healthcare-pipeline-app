package com.hospital.pipeline.booking;

import java.time.Instant;

/** The state of one request as the client sees it: QUEUED, PROCESSING, CONFIRMED or REJECTED. */
public record BookingStatus(String referenceId, String state, String message,
                            Long slotId, String patientName, Instant updatedAt) {

    public boolean finished() {
        return "CONFIRMED".equals(state) || "REJECTED".equals(state);
    }
}
