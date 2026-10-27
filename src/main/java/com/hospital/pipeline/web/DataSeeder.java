package com.hospital.pipeline.web;

import com.hospital.pipeline.domain.Doctor;
import com.hospital.pipeline.domain.Slot;
import com.hospital.pipeline.repo.DoctorRepository;
import com.hospital.pipeline.repo.SlotRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Three doctors with half-hour slots for the next three days, so there is something to book. */
@Component
public class DataSeeder implements ApplicationRunner {

    private final DoctorRepository doctors;
    private final SlotRepository slots;

    public DataSeeder(DoctorRepository doctors, SlotRepository slots) {
        this.doctors = doctors;
        this.slots = slots;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (doctors.count() > 0) return;
        List<Doctor> staff = doctors.saveAll(List.of(
                new Doctor("Dr. Meera Rao", "Cardiology"),
                new Doctor("Dr. Arun Nair", "General medicine"),
                new Doctor("Dr. Priya Das", "Pathology")));
        List<Slot> toSave = new ArrayList<>();
        for (Doctor d : staff) {
            for (int day = 1; day <= 3; day++) {
                LocalDate date = LocalDate.now().plusDays(day);
                for (LocalTime t = LocalTime.of(9, 0); t.isBefore(LocalTime.of(12, 0)); t = t.plusMinutes(30)) {
                    toSave.add(new Slot(d, date.atTime(t)));
                }
            }
        }
        slots.saveAll(toSave);
    }
}
