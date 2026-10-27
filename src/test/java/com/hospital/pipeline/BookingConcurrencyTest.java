package com.hospital.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hospital.pipeline.booking.BookingRequest;
import com.hospital.pipeline.booking.BookingStatus;
import com.hospital.pipeline.booking.BookingTracker;
import com.hospital.pipeline.domain.Slot;
import com.hospital.pipeline.domain.SlotStatus;
import com.hospital.pipeline.repo.AppointmentRepository;
import com.hospital.pipeline.repo.SlotRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingConcurrencyTest {

    @Autowired TestRestTemplate http;
    @Autowired SlotRepository slots;
    @Autowired AppointmentRepository appointments;
    @Autowired BookingTracker tracker;

    @SuppressWarnings("unchecked")
    @Test
    void thirtyPatientsOneSlotExactlyOneBooking() throws Exception {
        Slot slot = slots.findAll().get(0);
        int patients = 30;
        ExecutorService pool = Executors.newFixedThreadPool(patients);
        CountDownLatch go = new CountDownLatch(1);
        List<java.util.concurrent.Future<ResponseEntity<Map>>> calls = new ArrayList<>();
        for (int i = 0; i < patients; i++) {
            int n = i;
            calls.add(pool.submit(() -> {
                go.await();
                return http.postForEntity("/api/appointments", new BookingRequest(slot.id,
                        "Patient " + n, "p" + n + "@example.com", "INS-" + n, "test"), Map.class);
            }));
        }
        go.countDown();

        List<String> refs = new ArrayList<>();
        for (var call : calls) {
            ResponseEntity<Map> response = call.get();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            refs.add((String) response.getBody().get("referenceId"));
        }
        pool.shutdown();

        await().atMost(Duration.ofSeconds(30)).until(() ->
                refs.stream().allMatch(r -> tracker.find(r).map(BookingStatus::finished).orElse(false)));

        long confirmed = refs.stream().filter(r -> tracker.find(r).get().state().equals("CONFIRMED")).count();
        long rejected = refs.stream().filter(r -> tracker.find(r).get().state().equals("REJECTED")).count();

        assertThat(confirmed).isEqualTo(1);
        assertThat(rejected).isEqualTo(patients - 1);
        assertThat(appointments.findBySlotId(slot.id)).hasSize(1);
        assertThat(slots.findById(slot.id).get().status).isEqualTo(SlotStatus.BOOKED);
    }

    @SuppressWarnings("unchecked")
    @Test
    void invalidInsuranceIsRejectedBySoapService() {
        Slot slot = slots.findAll().get(5);
        ResponseEntity<Map> response = http.postForEntity("/api/appointments",
                new BookingRequest(slot.id, "Test Patient", "t@example.com", "INV-0001", "test"), Map.class);
        String ref = (String) response.getBody().get("referenceId");
        await().atMost(Duration.ofSeconds(15)).until(() -> tracker.find(ref).map(BookingStatus::finished).orElse(false));
        assertThat(tracker.find(ref).get().state()).isEqualTo("REJECTED");
        assertThat(tracker.find(ref).get().message()).contains("Insurance");
        assertThat(slots.findById(slot.id).get().status).isEqualTo(SlotStatus.AVAILABLE);
    }
}
