package com.airwatch.sba;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration coverage for {@link SecurityConfig#securityFilterChain} — the HTTP contract the unit
 * tests can't reach: which paths are public, that everything else is gated behind auth, and that
 * the hardened response headers (CSP / Referrer-Policy / X-Frame-Options) are emitted. Runs with
 * the {@code test} profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityFilterChainIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRejectsAnonymous() throws Exception {
        // Spring Security runs ahead of routing, so any non-permitted path challenges with 401
        // before a handler (or a 404) is reached — this proves the auth gate is in place.
        mockMvc.perform(get("/instances")).andExpect(status().isUnauthorized());
    }

    @Test
    void responsesCarryHardenedSecurityHeaders() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(
                        header().string(
                                        "Content-Security-Policy",
                                        containsString("default-src 'self'")))
                .andExpect(header().string("Referrer-Policy", "same-origin"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }
}
