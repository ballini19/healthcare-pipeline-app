package com.hospital.pipeline.booking;

import com.hospital.pipeline.domain.Appointment;
import com.hospital.pipeline.domain.Slot;
import com.hospital.pipeline.domain.SlotStatus;
import com.hospital.pipeline.repo.AppointmentRepository;
import com.hospital.pipeline.repo.SlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final SlotRepository slots;
    private final AppointmentRepository appointments;

    public BookingService(SlotRepository slots, AppointmentRepository appointments) {
        this.slots = slots;
        this.appointments = appointments;
    }

    /**
     * Flipping the slot bumps its @Version. If another worker committed first, the UPDATE matches
     * no row and Spring raises an ObjectOptimisticLockingFailureException, which rolls this back.
     */
    @Transactional
    public Appointment book(BookingMessage m) {
        Slot slot = slots.findById(m.slotId())
                .orElseThrow(() -> new SlotUnavailableException("Unknown slot " + m.slotId()));
        if (slot.status != SlotStatus.AVAILABLE) {
            throw new SlotUnavailableException("Slot already booked");
        }
        slot.status = SlotStatus.BOOKED;
        slots.saveAndFlush(slot);
        Appointment appointment = new Appointment(m.referenceId(), m.patientName(), m.patientEmail(),
                m.insuranceId(), m.reason(), slot);
        appointment.patientUsername = m.patientUsername();
        return appointments.save(appointment);
    }
}
