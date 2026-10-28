package com.hospital.pipeline.booking;

import com.hospital.pipeline.config.JmsConfig;
import com.hospital.pipeline.domain.Appointment;
import com.hospital.pipeline.domain.Slot;
import com.hospital.pipeline.domain.SlotStatus;
import com.hospital.pipeline.insurance.InsuranceClient;
import com.hospital.pipeline.repo.SlotRepository;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/** Background consumer: settles each queued request away from the HTTP threads. */
@Component
public class BookingWorker {

    private static final Logger log = LoggerFactory.getLogger(BookingWorker.class);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm");

    private final SlotRepository slots;
    private final BookingService bookingService;
    private final InsuranceClient insurance;
    private final BookingTracker tracker;
    private final AppointmentEvents events;

    public BookingWorker(SlotRepository slots, BookingService bookingService,
                         InsuranceClient insurance, BookingTracker tracker, AppointmentEvents events) {
        this.slots = slots;
        this.bookingService = bookingService;
        this.insurance = insurance;
        this.tracker = tracker;
        this.events = events;
    }

    @JmsListener(destination = JmsConfig.BOOKING_QUEUE)
    public void handle(BookingMessage m) {
        String ref = m.referenceId();
        tracker.update(ref, "PROCESSING", "A worker picked up the request", m.slotId(), m.patientName());
        try {
            Slot slot = slots.findById(m.slotId()).orElse(null);
            if (slot == null) {
                reject(m, "That slot does not exist");
                return;
            }
            if (slot.status != SlotStatus.AVAILABLE) {
                rejectTaken(m, slot, "Slot was already booked");
                return;
            }
            if (!insurance.isEligible(m.insuranceId(), m.patientName())) {
                reject(m, "Insurance verification failed for " + m.insuranceId());
                return;
            }
            Appointment booked = bookingService.book(m);
            tracker.update(ref, "CONFIRMED", "Appointment confirmed for " + slot.startTime.format(WHEN)
                    + " with " + slot.doctor.name, m.slotId(), m.patientName());
            events.published(booked);
        } catch (OptimisticLockingFailureException e) {
            log.info("{} lost the race for slot {}: {}", ref, m.slotId(), e.getClass().getSimpleName());
            rejectTaken(m, slots.findById(m.slotId()).orElse(null),
                    "Another request booked this slot first (optimistic lock conflict)");
        } catch (SlotUnavailableException e) {
            rejectTaken(m, slots.findById(m.slotId()).orElse(null), e.getMessage());
        } catch (Exception e) {
            log.warn("Booking {} failed", ref, e);
            reject(m, "Could not complete the booking: " + e.getMessage());
        }
    }

    private void reject(BookingMessage m, String why) {
        tracker.update(m.referenceId(), "REJECTED", why, m.slotId(), m.patientName());
    }

    private void rejectTaken(BookingMessage m, Slot slot, String why) {
        String offer = "";
        if (slot != null) {
            offer = slots.findFirstByDoctorIdAndStatusAndStartTimeAfterOrderByStartTime(
                            slot.doctor.id, SlotStatus.AVAILABLE, slot.startTime)
                    .map(next -> ". Next free slot: " + next.startTime.format(WHEN) + " (slot " + next.id + ")")
                    .orElse("");
        }
        reject(m, why + offer);
    }
}
