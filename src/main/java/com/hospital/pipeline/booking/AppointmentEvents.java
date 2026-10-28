package com.hospital.pipeline.booking;

import com.hospital.pipeline.domain.Appointment;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Pushes new bookings to the open screen of the doctor they belong to. */
@Component
public class AppointmentEvents {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> byDoctor = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long doctorId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> list = byDoctor.computeIfAbsent(doctorId, id -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable drop = () -> list.remove(emitter);
        emitter.onCompletion(drop);
        emitter.onTimeout(drop);
        emitter.onError(e -> drop.run());
        try {
            emitter.send(SseEmitter.event().name("hello").data("connected"));
        } catch (IOException e) {
            drop.run();
        }
        return emitter;
    }

    public void published(Appointment appointment) {
        Long doctorId = appointment.slot.doctor.id;
        AppointmentView view = AppointmentView.from(appointment);
        for (SseEmitter emitter : byDoctor.getOrDefault(doctorId, List.of())) {
            try {
                emitter.send(SseEmitter.event().name("appointment").data(view, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                byDoctor.get(doctorId).remove(emitter);
            }
        }
    }

    /** Keeps idle connections open through proxies. */
    @Scheduled(fixedRate = 15000)
    void heartbeat() {
        byDoctor.forEach((id, list) -> list.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                list.remove(emitter);
            }
        }));
    }
}
