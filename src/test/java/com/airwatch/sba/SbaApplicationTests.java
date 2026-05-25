package com.airwatch.sba;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: assert the Spring Boot Admin Server context loads cleanly.
 *
 * <p>The application has no domain logic — just one {@code @EnableAdminServer}
 * annotation and a YAML config — so a context-load test is the right level of
 * coverage. If this passes, we know:
 *   * The classpath wires up Spring + Admin Server + Webflux without conflict.
 *   * All Spring beans the SBA starter expects are present.
 *   * {@code application.yml} parses and binds correctly.
 *
 * <p>We don't assert specific endpoints here because the SBA starter owns
 * them; verifying its internals would be testing the library, not us. The
 * Dockerfile build and the {@code airwatch} compose stack do the actual
 * HTTP-level verification.
 */
@SpringBootTest
@ActiveProfiles("test")
class SbaApplicationTests {

    @Test
    void contextLoads() {
        // Empty body — the test passes iff the application context
        // bootstraps without throwing.
    }
}
