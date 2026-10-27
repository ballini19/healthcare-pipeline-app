package com.hospital.pipeline.web;

import com.hospital.pipeline.booking.BookingTracker;
import com.hospital.pipeline.domain.Doctor;
import com.hospital.pipeline.domain.Severity;
import com.hospital.pipeline.domain.Slot;
import com.hospital.pipeline.domain.SlotStatus;
import com.hospital.pipeline.lab.AlertService;
import com.hospital.pipeline.repo.DoctorRepository;
import com.hospital.pipeline.repo.LabResultRepository;
import com.hospital.pipeline.repo.SlotRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final DoctorRepository doctors;
    private final SlotRepository slots;
    private final BookingTracker tracker;
    private final LabResultRepository labResults;
    private final AlertService alerts;

    public CatalogController(DoctorRepository doctors, SlotRepository slots, BookingTracker tracker,
                             LabResultRepository labResults, AlertService alerts) {
        this.doctors = doctors;
        this.slots = slots;
        this.tracker = tracker;
        this.labResults = labResults;
        this.alerts = alerts;
    }

    @GetMapping("/doctors")
    public List<Doctor> doctors() {
        return doctors.findAll();
    }

    @GetMapping("/slots")
    public List<Slot> slots(@RequestParam Long doctorId,
                            @RequestParam(defaultValue = "AVAILABLE") SlotStatus status) {
        return slots.findByDoctorIdAndStatusOrderByStartTime(doctorId, status);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("requests", tracker.counts());
        out.put("slotsAvailable", slots.countByStatus(SlotStatus.AVAILABLE));
        out.put("slotsBooked", slots.countByStatus(SlotStatus.BOOKED));
        Map<String, Long> lab = new LinkedHashMap<>();
        for (Severity s : Severity.values()) lab.put(s.name(), labResults.countBySeverity(s));
        out.put("labResults", lab);
        out.put("alertsReceived", alerts.total());
        return out;
    }
}
