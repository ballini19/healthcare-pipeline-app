package com.hospital.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hospital.pipeline.auth.CredentialFile;
import com.hospital.pipeline.auth.Role;
import com.hospital.pipeline.booking.BookingRequest;
import com.hospital.pipeline.booking.BookingStatus;
import com.hospital.pipeline.booking.BookingTracker;
import com.hospital.pipeline.domain.Slot;
import com.hospital.pipeline.domain.SlotStatus;
import com.hospital.pipeline.repo.AppointmentRepository;
import com.hospital.pipeline.repo.SlotRepository;
import java.nio.file.Path;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingConcurrencyTest {

    @Autowired TestRestTemplate http;
    @Autowired SlotRepository slots;
    @Autowired AppointmentRepository appointments;
    @Autowired BookingTracker tracker;

    @SuppressWarnings("unchecked")
    private String login(CredentialFile.Entry who) {
        ResponseEntity<Map> r = http.postForEntity("/api/auth/login",
                Map.of("username", who.username(), "password", who.password(), "role", who.role().name()), Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) r.getBody().get("token");
    }

    private CredentialFile.Entry patient() throws Exception {
        return CredentialFile.load(Path.of("password.txt")).stream()
                .filter(e -> e.role() == Role.PATIENT).findFirst().orElseThrow();
    }

    private CredentialFile.Entry doctorNamed(String name) throws Exception {
        return CredentialFile.load(Path.of("password.txt")).stream()
                .filter(e -> e.role() == Role.DOCTOR && e.name().equals(name)).findFirst().orElseThrow();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    @SuppressWarnings("unchecked")
    @Test
    void thirtyPatientsOneSlotExactlyOneBooking() throws Exception {
        String token = login(patient());
        Slot slot = slots.findAll().get(0);
        int patients = 30;
        ExecutorService pool = Executors.newFixedThreadPool(patients);
        CountDownLatch go = new CountDownLatch(1);
        List<java.util.concurrent.Future<ResponseEntity<Map>>> calls = new ArrayList<>();
        for (int i = 0; i < patients; i++) {
            int n = i;
            calls.add(pool.submit(() -> {
                go.await();
                return http.postForEntity("/api/appointments", new HttpEntity<>(new BookingRequest(slot.id,
                        "Patient " + n, "p" + n + "@example.com", "INS-" + n, "test"), bearer(token)), Map.class);
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
    void invalidInsuranceIsRejectedBySoapService() throws Exception {
        String token = login(patient());
        Slot slot = slots.findAll().get(5);
        ResponseEntity<Map> response = http.postForEntity("/api/appointments", new HttpEntity<>(
                new BookingRequest(slot.id, "Test Patient", "t@example.com", "INV-0001", "test"), bearer(token)), Map.class);
        String ref = (String) response.getBody().get("referenceId");
        await().atMost(Duration.ofSeconds(15)).until(() -> tracker.find(ref).map(BookingStatus::finished).orElse(false));
        assertThat(tracker.find(ref).get().state()).isEqualTo("REJECTED");
        assertThat(tracker.find(ref).get().message()).contains("Insurance");
        assertThat(slots.findById(slot.id).get().status).isEqualTo(SlotStatus.AVAILABLE);
    }

    @SuppressWarnings("unchecked")
    @Test
    void bookingShowsUpForTheRightDoctorAndPatientOnly() throws Exception {
        String patientToken = login(patient());
        Slot slot = slots.findAll().get(20); // a slot of the second or third doctor
        String doctorName = slot.doctor.name;
        String otherName = slots.findAll().stream().map(s -> s.doctor.name)
                .filter(n -> !n.equals(doctorName)).findFirst().orElseThrow();

        ResponseEntity<Map> response = http.postForEntity("/api/appointments", new HttpEntity<>(
                new BookingRequest(slot.id, "Ballini", "b@example.com", "INS-1", "check-up"), bearer(patientToken)), Map.class);
        String ref = (String) response.getBody().get("referenceId");
        await().atMost(Duration.ofSeconds(15)).until(() -> tracker.find(ref).map(BookingStatus::finished).orElse(false));
        assertThat(tracker.find(ref).get().state()).isEqualTo("CONFIRMED");

        String doctorToken = login(doctorNamed(doctorName));
        Map<String, Object> mine = http.exchange("/api/doctor/overview", HttpMethod.GET,
                new HttpEntity<>(bearer(doctorToken)), Map.class).getBody();
        assertThat(((List<Map<String, Object>>) mine.get("appointments")))
                .anyMatch(a -> ref.equals(a.get("referenceId")));

        String otherToken = login(doctorNamed(otherName));
        Map<String, Object> theirs = http.exchange("/api/doctor/overview", HttpMethod.GET,
                new HttpEntity<>(bearer(otherToken)), Map.class).getBody();
        assertThat(((List<Map<String, Object>>) theirs.get("appointments")))
                .noneMatch(a -> ref.equals(a.get("referenceId")));

        List<Map<String, Object>> patientList = http.exchange("/api/my/appointments", HttpMethod.GET,
                new HttpEntity<>(bearer(patientToken)), List.class).getBody();
        assertThat(patientList).anyMatch(a -> ref.equals(a.get("referenceId")));
    }

    @SuppressWarnings("unchecked")
    @Test
    void endpointsAreProtectedByLoginAndRole() throws Exception {
        // the default JDK connection cannot read a 401 reply to a POST, so use the java.net.http client here
        http.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
        assertThat(http.getForEntity("/api/doctors", Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String patientToken = login(patient());
        ResponseEntity<Map> labAsPatient = http.exchange("/api/lab/status", HttpMethod.GET,
                new HttpEntity<>(bearer(patientToken)), Map.class);
        assertThat(labAsPatient.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // a patient cannot sign in through the doctors' box
        ResponseEntity<Map> wrongBox = http.postForEntity("/api/auth/login",
                Map.of("username", patient().username(), "password", patient().password(), "role", "DOCTOR"), Map.class);
        assertThat(wrongBox.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map> badPassword = http.postForEntity("/api/auth/login",
                Map.of("username", patient().username(), "password", "nope", "role", "PATIENT"), Map.class);
        assertThat(badPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
