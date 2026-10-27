package com.hospital.pipeline.booking;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Keeps request progress in memory so the REST layer can answer 202 and later report the outcome
 * without touching the database or its locks.
 */
@Component
public class BookingTracker {

    private final Map<String, BookingStatus> statuses = new ConcurrentHashMap<>();

    public void update(String ref, String state, String message, Long slotId, String patientName) {
        statuses.put(ref, new BookingStatus(ref, state, message, slotId, patientName, Instant.now()));
    }

    public Optional<BookingStatus> find(String ref) {
        return Optional.ofNullable(statuses.get(ref));
    }

    public List<BookingStatus> recent(int limit) {
        return statuses.values().stream()
                .sorted(Comparator.comparing(BookingStatus::updatedAt).reversed())
                .limit(limit).toList();
    }

    public Map<String, Long> counts() {
        Map<String, Long> out = new java.util.LinkedHashMap<>();
        for (String s : List.of("QUEUED", "PROCESSING", "CONFIRMED", "REJECTED")) out.put(s, 0L);
        statuses.values().forEach(v -> out.merge(v.state(), 1L, Long::sum));
        return out;
    }
}
