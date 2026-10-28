package com.hospital.pipeline.auth;

import java.time.Instant;

/** A signed-in user. The token travels in the Authorization header, so each browser tab has its own login. */
public record Session(String token, String username, String displayName, Role role,
                      Long doctorId, String email, Instant expires) {
}
