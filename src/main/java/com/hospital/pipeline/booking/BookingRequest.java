package com.hospital.pipeline.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingRequest(
        @NotNull Long slotId,
        @NotBlank @Size(max = 80) String patientName,
        @NotBlank @Email String patientEmail,
        @NotBlank @Size(max = 40) String insuranceId,
        @Size(max = 200) String reason) {
}
