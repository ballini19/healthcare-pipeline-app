package com.hospital.pipeline.booking;

import com.hospital.pipeline.config.JmsConfig;
import com.hospital.pipeline.domain.Appointment;
import com.hospital.pipeline.repo.AppointmentRepository;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final JmsTemplate queue;
    private final BookingTracker tracker;
    private final AppointmentRepository appointments;

    public BookingController(JmsTemplate queueJmsTemplate, BookingTracker tracker,
                             AppointmentRepository appointments) {
        this.queue = queueJmsTemplate;
        this.tracker = tracker;
        this.appointments = appointments;
    }

    /** Validates, enqueues and answers 202 straight away. No database access on this thread. */
    @PostMapping("/appointments")
    public ResponseEntity<Map<String, Object>> book(@Valid @RequestBody BookingRequest req) {
        String ref = enqueue(req);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header("Location", "/api/appointments/" + ref)
                .body(Map.of("referenceId", ref, "state", "QUEUED",
                        "statusUrl", "/api/appointments/" + ref));
    }

    @GetMapping("/appointments/{ref}")
    public ResponseEntity<BookingStatus> status(@PathVariable String ref) {
        return tracker.find(ref).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/appointments")
    public List<Appointment> confirmed(@RequestParam(defaultValue = "20") int limit) {
        return appointments.findAllByOrderByIdDesc(PageRequest.of(0, Math.min(limit, 100)));
    }

    /** Staff demo: fire many requests for one slot at once and watch only one win. */
    @PostMapping("/demo/race")
    public Map<String, Object> race(@RequestParam Long slotId, @RequestParam(defaultValue = "20") int count) {
        int n = Math.max(2, Math.min(count, 200));
        List<String> refs = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            refs.add(enqueue(new BookingRequest(slotId, "Demo patient " + i,
                    "patient" + i + "@example.com", "INS-" + (1000 + i), "Concurrency demo")));
        }
        return Map.of("sent", n, "referenceIds", refs);
    }

    @GetMapping("/demo/race/status")
    public Map<String, Object> raceStatus(@RequestParam Long slotId) {
        List<BookingStatus> mine = tracker.recent(1000).stream()
                .filter(s -> slotId.equals(s.slotId()) && s.patientName() != null
                        && s.patientName().startsWith("Demo patient")).toList();
        Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (String s : List.of("QUEUED", "PROCESSING", "CONFIRMED", "REJECTED")) {
            counts.put(s, mine.stream().filter(b -> b.state().equals(s)).count());
        }
        return Map.of("counts", counts, "requests", mine.stream().limit(30).toList());
    }

    private String enqueue(BookingRequest req) {
        String ref = "APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        tracker.update(ref, "QUEUED", "Waiting in appointmentQueue", req.slotId(), req.patientName());
        queue.convertAndSend(JmsConfig.BOOKING_QUEUE, new BookingMessage(ref, req.slotId(),
                req.patientName(), req.patientEmail(), req.insuranceId(), req.reason()));
        return ref;
    }
}
