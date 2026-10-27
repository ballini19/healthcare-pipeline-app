package com.hospital.pipeline.lab;

/** Published on the labAlerts topic when a result is critical. */
public record AlertMessage(String patientId, String testName, double value, String unit,
                           double refLow, double refHigh, String severity, String raisedAt) {
}
