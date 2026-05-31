package com.airwatch.sba;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration coverage for {@link SecurityConfig#securityFilterChain} — the actual HTTP contract
 * that the unit tests can't reach: which paths are public, that everything else is gated behind
 * BASIC auth, and that the hardened response headers (CSP / Referrer-Policy / X-Frame-Options) are
 * emitted. Runs with the {@code test} profile, whose in-memory credentials are {@code
 * test}/{@code test-password}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityFilterChainIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private static final String BASIC_AUTH =
            "Basic "
                    + Base64.getEncoder()
                            .encodeToString("test:test-password".getBytes(StandardCharsets.UTF_8));

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRejectsAnonymous() throws Exception {
        // Spring Security runs ahead of routing, so any non-permitted path challenges with 401
        // before a handler (or a 404) is reached.
        mockMvc.perform(get("/instances")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsValidBasicAuth() throws Exception {
        int code =
                mockMvc.perform(get("/instances").header("Authorization", BASIC_AUTH))
                        .andReturn()
                        .getResponse()
                        .getStatus();
        // Valid credentials clear the BASIC challenge — anything but 401 proves auth succeeded,
        // independent of how the downstream SBA handler responds.
        assertThat(code).isNotEqualTo(401);
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
