package com.airwatch.sba;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Unit tests for {@link SecurityConfig}'s bean-factory logic. They run without a Spring context (no
 * {@code @SpringBootTest}), so the fail-closed credential contract is exercised directly and fast —
 * and they stay green in sandboxes where the full reactive SBA context can't bootstrap.
 */
class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void passwordEncoderIsBcryptAndRoundTrips() {
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        String hash = encoder.encode("correct horse battery staple");
        assertThat(encoder.matches("correct horse battery staple", hash)).isTrue();
        assertThat(encoder.matches("wrong password", hash)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void userDetailsServiceFailsClosedWhenHashMissing(String missingHash) {
        PasswordEncoder encoder = config.passwordEncoder();

        // Fail-closed: a missing/blank hash must abort startup, never silently allow a
        // password-less admin. The message points operators at the env var to set.
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> config.userDetailsService("admin", missingHash, encoder))
                .withMessageContaining("SBA_USER_PASSWORD_HASH");
    }

    @Test
    void userDetailsServiceBuildsAdminUserFromHash() {
        PasswordEncoder encoder = config.passwordEncoder();
        String hash = encoder.encode("s3cret");

        InMemoryUserDetailsManager mgr = config.userDetailsService("ops", hash, encoder);
        UserDetails user = mgr.loadUserByUsername("ops");

        assertThat(user.getUsername()).isEqualTo("ops");
        // The supplied value is already a BCrypt hash and is stored verbatim, not re-encoded.
        assertThat(user.getPassword()).isEqualTo(hash);
        assertThat(user.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }
}
