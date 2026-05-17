package com.airwatch.sba;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Admin Server entry point.
 *
 * <p>One annotation, one main method. Everything else (UI, REST,
 * registry, notification routing) is wired by the
 * {@code spring-boot-admin-starter-server} starter.
 *
 * <h3>What it does</h3>
 *   * Listens on port 8080 (mapped to host {@code SBA_PORT} in compose).
 *   * Accepts {@code POST /instances} registrations from the api
 *     replicas (they ship with {@code spring-boot-admin-starter-client}).
 *   * Polls each registered instance's actuator endpoints every 10 s
 *     and renders them in a unified UI.
 *
 * <h3>What it deliberately doesn't do</h3>
 *   * No persistence — registry is in-memory, registrations rebuild
 *     within ~30 s of an SBA restart because the api clients keep
 *     re-registering.
 *   * No security here — the path is gated behind the admin nginx port
 *     (loopback only) and the {@code /admin/sba/**} route on nginx is
 *     covered by AdminAuthFilter at the edge.
 */
@SpringBootApplication
@EnableAdminServer
public class SbaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SbaApplication.class, args);
    }
}
