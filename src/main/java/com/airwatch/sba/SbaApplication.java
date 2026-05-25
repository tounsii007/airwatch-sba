package com.airwatch.sba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import de.codecentric.boot.admin.server.config.EnableAdminServer;

/**
 * Spring Boot Admin Server entry point. See README.md for what this service does, what it
 * deliberately doesn't do, how registrations work, and the security posture (auth lives at the
 * nginx edge, not here).
 */
@SpringBootApplication
@EnableAdminServer
public class SbaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SbaApplication.class, args);
    }
}
