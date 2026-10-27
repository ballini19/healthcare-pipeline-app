package com.hospital.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hospital.pipeline.domain.Severity;
import com.hospital.pipeline.lab.AlertService;
import com.hospital.pipeline.lab.LabFeedGenerator;
import com.hospital.pipeline.lab.LabRunLog;
import com.hospital.pipeline.lab.LabService;
import com.hospital.pipeline.repo.LabResultRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LabBatchTest {

    @Autowired LabService service;
    @Autowired LabFeedGenerator generator;
    @Autowired LabRunLog runLog;
    @Autowired LabResultRepository results;
    @Autowired AlertService alerts;

    @Test
    void feedIsIngestedInChunksWithSkipsAndCriticalAlerts() throws Exception {
        Path feed = generator.generate(Files.createTempFile("feed", ".csv"), 300, 42);
        long before = results.count();

        service.run(feed);

        LabRunLog.Summary summary = runLog.last();
        assertThat(summary.status()).isEqualTo("COMPLETED");
        assertThat(summary.skipped()).isEqualTo(5);                    // the five deliberately bad rows
        assertThat(summary.written()).isEqualTo(300);
        assertThat(results.count() - before).isEqualTo(300);
        assertThat(summary.commits()).isGreaterThanOrEqualTo(6);       // 300 rows in chunks of 50
        assertThat(runLog.skipped()).hasSize(5);

        long critical = results.countBySeverity(Severity.CRITICAL);
        assertThat(critical).isPositive();
        await().atMost(Duration.ofSeconds(10)).until(() -> alerts.total() >= critical);
        assertThat(results.findBySeverity(Severity.CRITICAL)).allMatch(r -> r.severity == Severity.CRITICAL);
    }
}
