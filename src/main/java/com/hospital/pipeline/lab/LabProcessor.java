package com.hospital.pipeline.lab;

import com.hospital.pipeline.domain.BloodTestResult;
import com.hospital.pipeline.domain.LabResult;
import com.hospital.pipeline.domain.Severity;
import com.hospital.pipeline.domain.UrineTestResult;
import java.time.Instant;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.stereotype.Component;

/** Validates a raw record, triages it against its reference range, and raises alerts for critical values. */
@Component
public class LabProcessor implements ItemProcessor<LabRecord, LabResult> {

    private final AlertService alerts;

    public LabProcessor(AlertService alerts) {
        this.alerts = alerts;
    }

    @Override
    public LabResult process(LabRecord r) {
        if (blank(r.getPatientId()) || blank(r.getTestName())) {
            throw new ValidationException("Missing patient id or test name");
        }
        if (r.getRefLow() >= r.getRefHigh()) {
            throw new ValidationException("Reference range is empty for " + r.getTestName());
        }
        LabResult out = switch (String.valueOf(r.getTestType()).trim().toUpperCase()) {
            case "BLOOD" -> new BloodTestResult();
            case "URINE" -> new UrineTestResult();
            default -> throw new ValidationException("Unknown test type '" + r.getTestType() + "'");
        };
        out.patientId = r.getPatientId().trim();
        out.testName = r.getTestName().trim();
        out.value = r.getValue();
        out.unit = r.getUnit();
        out.refLow = r.getRefLow();
        out.refHigh = r.getRefHigh();
        out.severity = classify(r);
        out.recordedAt = Instant.now();

        if (out.severity == Severity.CRITICAL) {
            alerts.publish(new AlertMessage(out.patientId, out.testName, out.value, out.unit,
                    out.refLow, out.refHigh, out.severity.name(), Instant.now().toString()));
        }
        return out;
    }

    /** Outside the range is abnormal; more than half a range-width outside is critical. */
    static Severity classify(LabRecord r) {
        double span = r.getRefHigh() - r.getRefLow();
        double deviation = r.getValue() < r.getRefLow() ? r.getRefLow() - r.getValue()
                : r.getValue() > r.getRefHigh() ? r.getValue() - r.getRefHigh() : 0;
        if (deviation == 0) return Severity.NORMAL;
        return deviation > span * 0.5 ? Severity.CRITICAL : Severity.ABNORMAL;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
