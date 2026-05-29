package com.airwatch.sba;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Defense-in-depth security configuration for the SBA Server.
 *
 * <p>nginx at the edge already terminates auth for the public surface, but we layer HTTP BASIC on
 * top so a compromised internal network or a misconfigured proxy can't expose the actuator
 * dashboard. Credentials are sourced from environment variables and the password is a BCrypt hash
 * — no plaintext at rest.
 *
 * <p>CSRF is enabled by default but disabled for the {@code POST /instances} endpoints because SBA
 * clients self-register there without a session. The CSRF exemption does <em>not</em> bypass
 * authentication — {@code anyRequest().authenticated()} below still gates POST/DELETE on
 * {@code /instances/*} behind BASIC auth.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // anyRequest().authenticated() covers POST/DELETE on /instances/* —
        // the CSRF exemption below only relaxes the CSRF-token check, not BASIC auth.
        http.authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/actuator/health", "/actuator/info")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .httpBasic(basic -> {})
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(
                        csrf ->
                                csrf.ignoringRequestMatchers(
                                        PathPatternRequestMatcher.withDefaults()
                                                .matcher(HttpMethod.POST, "/instances"),
                                        PathPatternRequestMatcher.withDefaults()
                                                .matcher(HttpMethod.POST, "/instances/*")))
                .headers(
                        headers ->
                                headers.frameOptions(frame -> frame.sameOrigin())
                                        .contentSecurityPolicy(
                                                csp ->
                                                        csp.policyDirectives(
                                                                "default-src 'self'; style-src"
                                                                    + " 'self' 'unsafe-inline';"
                                                                    + " img-src 'self' data:;"
                                                                    + " script-src 'self'"))
                                        .referrerPolicy(
                                                ref ->
                                                        ref.policy(
                                                                ReferrerPolicyHeaderWriter
                                                                        .ReferrerPolicy
                                                                        .SAME_ORIGIN)));

        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(
            @Value("${spring.boot.admin.user.name:admin}") String username,
            @Value("${spring.boot.admin.user.password:}") String passwordHash,
            PasswordEncoder passwordEncoder) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalStateException(
                    "spring.boot.admin.user.password (env SBA_USER_PASSWORD_HASH) must be set to a"
                        + " BCrypt hash. Generate with: htpasswd -bnBC 10 \"\" \"MyPassword\" | tr"
                        + " -d ':\\n'");
        }
        UserDetails user = User.withUsername(username).password(passwordHash).roles("ADMIN").build();
        InMemoryUserDetailsManager mgr = new InMemoryUserDetailsManager(user);
        // The provided password is already a BCrypt hash; PasswordEncoder is wired as a bean so
        // the DaoAuthenticationProvider picks it up automatically.
        return mgr;
    }
}
