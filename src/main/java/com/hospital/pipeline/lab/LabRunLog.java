package com.hospital.pipeline.lab;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** What the staff screen shows about the latest batch run. */
@Component
public class LabRunLog {

    public record Summary(String status, long read, long written, long skipped, long commits, long millis) {
    }

    private final List<String> skipped = new CopyOnWriteArrayList<>();
    private volatile boolean running;
    private volatile Summary last;

    public boolean running() { return running; }
    public void running(boolean running) { this.running = running; }
    public Summary last() { return last; }
    public void last(Summary last) { this.last = last; }
    public List<String> skipped() { return skipped; }
    public void skip(String note) { skipped.add(note); }
    public void clearSkipped() { skipped.clear(); }
}
