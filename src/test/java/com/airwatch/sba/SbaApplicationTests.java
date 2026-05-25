package com.airwatch.sba;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: assert the Spring Boot Admin Server context loads cleanly. The application has no
 * domain logic — just one {@code @EnableAdminServer} annotation and a YAML config — so a
 * context-load test is the right level of coverage.
 */
@SpringBootTest
@ActiveProfiles("test")
class SbaApplicationTests {

    @Test
    void contextLoads() {
        // Empty body — the test passes iff the application context bootstraps without throwing.
    }
}
