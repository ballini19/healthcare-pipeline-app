package com.hospital.pipeline.lab;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

/** Writes an analyzer-style CSV so the pipeline has a realistic nightly feed to chew on. */
@Component
public class LabFeedGenerator {

    private record Test(String type, String name, String unit, double low, double high) {
    }

    private static final List<Test> TESTS = List.of(
            new Test("BLOOD", "Hemoglobin", "g/dL", 12, 17),
            new Test("BLOOD", "Glucose", "mg/dL", 70, 100),
            new Test("BLOOD", "Potassium", "mmol/L", 3.5, 5.0),
            new Test("BLOOD", "WBC", "10^9/L", 4, 11),
            new Test("BLOOD", "Creatinine", "mg/dL", 0.6, 1.3),
            new Test("URINE", "pH", "pH", 4.5, 8.0),
            new Test("URINE", "Protein", "mg/dL", 0, 14),
            new Test("URINE", "Specific gravity", "SG", 1.005, 1.030));

    public Path generate(Path file, int rows, long seed) {
        Random random = new Random(seed);
        List<String> lines = new ArrayList<>();
        lines.add("testType,patientId,testName,value,unit,refLow,refHigh");
        for (int i = 0; i < rows; i++) {
            Test t = TESTS.get(random.nextInt(TESTS.size()));
            double span = t.high() - t.low();
            double roll = random.nextDouble();
            double value;
            if (roll < 0.76) value = t.low() + span * random.nextDouble();
            else if (roll < 0.92) value = random.nextBoolean() ? t.low() - span * 0.3 * random.nextDouble() - 0.01 * span
                    : t.high() + span * 0.3 * random.nextDouble() + 0.01 * span;
            else value = random.nextBoolean() ? t.low() - span * (0.6 + random.nextDouble())
                    : t.high() + span * (0.6 + random.nextDouble());
            if (t.low() == 0 && value < 0) value = 0;
            lines.add(String.format(java.util.Locale.ROOT, "%s,P%04d,%s,%.3f,%s,%s,%s",
                    t.type(), 1000 + random.nextInt(80), t.name(), value, t.unit(), t.low(), t.high()));
        }
        // a few rows an analyzer might really produce wrongly
        lines.add(Math.min(lines.size(), 20), "BLOOD,P1042,Glucose,n/a,mg/dL,70,100");
        lines.add(Math.min(lines.size(), 75), "BLOOD,,Hemoglobin,13.2,g/dL,12,17");
        lines.add(Math.min(lines.size(), 130), "PLASMA,P1007,Sodium,140,mmol/L,135,145");
        lines.add(Math.min(lines.size(), 190), "BLOOD,P1013,Potassium");
        lines.add(Math.min(lines.size(), 240), "URINE,P1031,pH,6.5,pH,8,4.5");
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return file;
    }
}
