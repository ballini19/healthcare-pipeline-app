package com.hospital.pipeline.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(String username, String password, String role) {
    }

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req) {
        Role expected;
        try {
            expected = req.role() == null ? null : Role.valueOf(req.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            expected = null;
        }
        return auth.login(req.username(), req.password(), expected)
                .map(s -> ResponseEntity.ok(view(s, true)))
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(Map.of("message", "Wrong username or password for this sign-in")));
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestAttribute(AuthInterceptor.ATTR) Session session) {
        return view(session, false);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestAttribute(AuthInterceptor.ATTR) Session session) {
        auth.logout(session.token());
        return Map.of("message", "Signed out");
    }

    private static Map<String, Object> view(Session s, boolean withToken) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (withToken) out.put("token", s.token());
        out.put("username", s.username());
        out.put("displayName", s.displayName());
        out.put("role", s.role().name());
        out.put("doctorId", s.doctorId());
        out.put("email", s.email());
        return out;
    }
}
