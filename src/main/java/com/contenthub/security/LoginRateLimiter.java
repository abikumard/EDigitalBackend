package com.contenthub.security;

import com.contenthub.exception.AppExceptions.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// In-memory brute-force guard for the user and admin login endpoints. Blocks
// further attempts against the same identifier for a cool-down window after
// too many wrong passwords in a row. This is enough for a single-instance
// deployment; if you ever run multiple backend instances behind a load
// balancer, swap this for a shared store (Redis, Bucket4j + Redis, etc.)
// since each instance would otherwise track attempts separately.
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L; // 15 minutes

    private static class Attempts {
        int count;
        long windowStart = Instant.now().toEpochMilli();
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    // Call before attempting authentication. Throws if this identifier has
    // failed too many times recently.
    public void checkAllowed(String key) {
        Attempts a = attemptsByKey.get(normalize(key));
        if (a == null) {
            return;
        }
        synchronized (a) {
            long now = Instant.now().toEpochMilli();
            if (now - a.windowStart > WINDOW_MS) {
                return;
            }
            if (a.count >= MAX_ATTEMPTS) {
                long minutesLeft = (WINDOW_MS - (now - a.windowStart)) / 60000 + 1;
                throw new UnauthorizedException(
                        "Too many failed attempts. Please try again in about " + minutesLeft + " minute(s).");
            }
        }
    }

    public void recordFailure(String key) {
        String k = normalize(key);
        Attempts a = attemptsByKey.computeIfAbsent(k, x -> new Attempts());
        synchronized (a) {
            long now = Instant.now().toEpochMilli();
            if (now - a.windowStart > WINDOW_MS) {
                a.count = 0;
                a.windowStart = now;
            }
            a.count++;
        }
    }

    public void recordSuccess(String key) {
        attemptsByKey.remove(normalize(key));
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }
}
