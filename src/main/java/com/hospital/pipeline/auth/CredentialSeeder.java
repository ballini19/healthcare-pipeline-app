package com.hospital.pipeline.auth;

import com.hospital.pipeline.domain.Doctor;
import com.hospital.pipeline.domain.UserAccount;
import com.hospital.pipeline.repo.DoctorRepository;
import com.hospital.pipeline.repo.UserAccountRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Creates the doctor and patient logins from password.txt, storing only BCrypt hashes. */
@Component
@Order(2)
public class CredentialSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CredentialSeeder.class);

    private final UserAccountRepository users;
    private final DoctorRepository doctors;
    private final AuthService auth;
    private final String file;

    public CredentialSeeder(UserAccountRepository users, DoctorRepository doctors, AuthService auth,
                            @Value("${app.credentials-file:password.txt}") String file) {
        this.users = users;
        this.doctors = doctors;
        this.auth = auth;
        this.file = file;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (users.count() > 0) return;
        Path path = Path.of(file);
        if (!Files.exists(path)) {
            log.warn("No credentials file at {}; nobody will be able to sign in", path.toAbsolutePath());
            return;
        }
        for (CredentialFile.Entry e : CredentialFile.load(path)) {
            Long doctorId = e.role() == Role.DOCTOR
                    ? doctors.findAll().stream().filter(d -> d.name.equals(e.name())).map(d -> d.id).findFirst().orElse(null)
                    : null;
            users.save(new UserAccount(e.username(), auth.hash(e.password()), e.role(), e.name(),
                    e.username().replace(".", "") + "@example.com", doctorId));
        }
        log.info("Created {} logins from {}", users.count(), path);
    }
}
