package com.hospital.pipeline.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.web.servlet.HandlerInterceptor;

/** Checks the token on every /api call and enforces which role may use which endpoints. */
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR = "session";

    private final AuthService auth;

    public AuthInterceptor(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws IOException {
        String path = req.getRequestURI();
        if ("OPTIONS".equals(req.getMethod()) || path.equals("/api/auth/login")) return true;

        Optional<Session> session = auth.find(token(req));
        if (session.isEmpty()) {
            reject(res, 401, "Please sign in");
            return false;
        }
        Role need = requiredRole(req.getMethod(), path);
        if (need != null && session.get().role() != need) {
            reject(res, 403, "This area is for " + need.name().toLowerCase() + " accounts");
            return false;
        }
        req.setAttribute(ATTR, session.get());
        return true;
    }

    static Role requiredRole(String method, String path) {
        if (path.startsWith("/api/doctor/") || path.startsWith("/api/lab/")
                || path.startsWith("/api/demo/") || path.equals("/api/stats")) {
            return Role.DOCTOR;
        }
        if (path.startsWith("/api/my/") || ("POST".equals(method) && path.equals("/api/appointments"))) {
            return Role.PATIENT;
        }
        return null;
    }

    /** EventSource cannot set headers, so the live stream passes the token as a query parameter. */
    static String token(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header.substring(7).trim();
        return req.getParameter("token");
    }

    private static void reject(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
