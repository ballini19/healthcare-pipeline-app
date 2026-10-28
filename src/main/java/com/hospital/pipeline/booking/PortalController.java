package com.hospital.pipeline.booking;

import com.hospital.pipeline.auth.AuthInterceptor;
import com.hospital.pipeline.auth.Session;
import com.hospital.pipeline.domain.Doctor;
import com.hospital.pipeline.domain.SlotStatus;
import com.hospital.pipeline.repo.AppointmentRepository;
import com.hospital.pipeline.repo.DoctorRepository;
import com.hospital.pipeline.repo.SlotRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Data for the two portals: a doctor's own appointments, and a patient's own bookings. */
@RestController
@RequestMapping("/api")
public class PortalController {

    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;
    private final SlotRepository slots;
    private final AppointmentEvents events;

    public PortalController(AppointmentRepository appointments, DoctorRepository doctors,
                            SlotRepository slots, AppointmentEvents events) {
        this.appointments = appointments;
        this.doctors = doctors;
        this.slots = slots;
        this.events = events;
    }

    @GetMapping("/doctor/overview")
    public Map<String, Object> overview(@RequestAttribute(AuthInterceptor.ATTR) Session session) {
        Doctor doctor = doctors.findById(session.doctorId()).orElseThrow();
        List<AppointmentView> list = appointments.findBySlotDoctorIdOrderBySlotStartTime(doctor.id)
                .stream().map(AppointmentView::from).toList();
        return Map.of("doctor", doctor, "booked", list.size(),
                "open", slots.countByDoctorIdAndStatus(doctor.id, SlotStatus.AVAILABLE),
                "appointments", list);
    }

    /** Server-sent events: one line arrives here the moment a booking for this doctor is confirmed. */
    @GetMapping(path = "/doctor/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestAttribute(AuthInterceptor.ATTR) Session session) {
        return events.subscribe(session.doctorId());
    }

    @GetMapping("/my/appointments")
    public List<AppointmentView> mine(@RequestAttribute(AuthInterceptor.ATTR) Session session) {
        return appointments.findByPatientUsernameOrderByIdDesc(session.username())
                .stream().map(AppointmentView::from).toList();
    }
}
