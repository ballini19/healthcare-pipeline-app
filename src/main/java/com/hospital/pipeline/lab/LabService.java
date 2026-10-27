package com.hospital.pipeline.lab;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LabService {

    private static final Logger log = LoggerFactory.getLogger(LabService.class);

    private final JobLauncher launcher;
    private final Job job;
    private final LabFeedGenerator generator;
    private final LabRunLog runLog;

    public LabService(JobLauncher launcher, Job labResultJob, LabFeedGenerator generator, LabRunLog runLog) {
        this.launcher = launcher;
        this.job = labResultJob;
        this.generator = generator;
        this.runLog = runLog;
    }

    /** Starts the job in the background; false means one is already running. */
    public synchronized boolean startAsync(Path file) {
        if (runLog.running()) return false;
        runLog.running(true);
        CompletableFuture.runAsync(() -> run(file));
        return true;
    }

    /** Runs the job on the calling thread. */
    public void run(Path file) {
        JobParameters params = new JobParametersBuilder()
                .addString("file", file.toAbsolutePath().toString())
                .addLong("run", System.currentTimeMillis())
                .toJobParameters();
        try {
            launcher.run(job, params);
        } catch (Exception e) {
            log.error("Lab batch failed to start", e);
            runLog.running(false);
        }
    }

    public Path freshFeed() {
        return generator.generate(Path.of("data", "lab_feed.csv"), 300, System.nanoTime());
    }

    @Scheduled(cron = "${lab.nightly-cron}")
    public void nightly() {
        log.info("Nightly lab batch starting");
        startAsync(freshFeed());
    }
}
