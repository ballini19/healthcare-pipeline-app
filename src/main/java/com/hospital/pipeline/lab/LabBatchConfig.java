package com.hospital.pipeline.lab;

import com.hospital.pipeline.domain.LabResult;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/** Chunk-oriented pipeline: flat file in, triaged JPA entities out, bad rows skipped and logged. */
@Configuration
public class LabBatchConfig {

    @Bean
    @StepScope
    FlatFileItemReader<LabRecord> labReader(@Value("#{jobParameters['file']}") String file) {
        return new FlatFileItemReaderBuilder<LabRecord>()
                .name("labReader")
                .resource(new FileSystemResource(file))
                .linesToSkip(1)
                .delimited()
                .names("testType", "patientId", "testName", "value", "unit", "refLow", "refHigh")
                .targetType(LabRecord.class)
                .build();
    }

    @Bean
    JpaItemWriter<LabResult> labWriter(EntityManagerFactory emf) {
        return new JpaItemWriterBuilder<LabResult>().entityManagerFactory(emf).usePersist(true).build();
    }

    @Bean
    Step labStep(JobRepository repository, PlatformTransactionManager tx,
                 FlatFileItemReader<LabRecord> labReader, LabProcessor processor,
                 JpaItemWriter<LabResult> labWriter, LabRunLog runLog,
                 @Value("${lab.chunk-size:50}") int chunkSize) {
        return new StepBuilder("labStep", repository)
                .<LabRecord, LabResult>chunk(chunkSize, tx)
                .reader(labReader)
                .processor(processor)
                .writer(labWriter)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skip(ValidationException.class)
                .skipLimit(1000)
                .listener(skipListener(runLog))
                .build();
    }

    @Bean
    Job labResultJob(JobRepository repository, Step labStep, LabRunLog runLog) {
        return new JobBuilder("labResultJob", repository)
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution job) {
                        runLog.clearSkipped();
                        runLog.running(true);
                    }

                    @Override
                    public void afterJob(JobExecution job) {
                        StepExecution step = job.getStepExecutions().iterator().next();
                        long millis = job.getEndTime() == null || job.getStartTime() == null ? 0
                                : java.time.Duration.between(job.getStartTime(), job.getEndTime()).toMillis();
                        runLog.last(new LabRunLog.Summary(job.getStatus().name(), step.getReadCount(),
                                step.getWriteCount(), step.getSkipCount(), step.getCommitCount(), millis));
                        runLog.running(false);
                    }
                })
                .start(labStep)
                .build();
    }

    private SkipListener<LabRecord, LabResult> skipListener(LabRunLog runLog) {
        return new SkipListener<>() {
            @Override
            public void onSkipInRead(Throwable t) {
                if (t instanceof FlatFileParseException e) {
                    runLog.skip("Line " + e.getLineNumber() + ": " + e.getInput() + "  (unreadable)");
                } else {
                    runLog.skip("Unreadable row: " + t.getMessage());
                }
            }

            @Override
            public void onSkipInProcess(LabRecord item, Throwable t) {
                runLog.skip(item + "  (" + t.getMessage() + ")");
            }
        };
    }
}
