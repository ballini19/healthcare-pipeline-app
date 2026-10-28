package com.hospital.pipeline.auth;

import com.hospital.pipeline.repo.UserAccountRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Duration LIFETIME = Duration.ofHours(12);

    private final UserAccountRepository users;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthService(UserAccountRepository users) {
        this.users = users;
    }

    public String hash(String raw) {
        return encoder.encode(raw);
    }

    public Optional<Session> login(String username, String password, Role expected) {
        String name = username == null ? "" : username.trim().toLowerCase();
        return users.findByUsername(name)
                .filter(u -> expected == null || u.role == expected)
                .filter(u -> password != null && encoder.matches(password, u.passwordHash))
                .map(u -> {
                    Session s = new Session(newToken(), u.username, u.displayName, u.role,
                            u.doctorId, u.email, Instant.now().plus(LIFETIME));
                    sessions.put(s.token(), s);
                    return s;
                });
    }

    public Optional<Session> find(String token) {
        if (token == null) return Optional.empty();
        Session s = sessions.get(token);
        if (s == null) return Optional.empty();
        if (s.expires().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(s);
    }

    public void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
