# Changelog

All notable changes to **AirWatch SBA** (Spring Boot Admin Server) are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive README documenting what the service does (JVM/actuator UI for API replicas)
  and what it intentionally doesn't do (no persistence)
- `CHANGELOG.md` (Keep-a-Changelog format)
- `CONTRIBUTING.md` with dev workflow and PR checklist

### Security
- Spring Security BASIC auth required for all paths except actuator health/info
- JAVA_TOOL_OPTIONS hardening (DisableAttachMechanism)
- Discovery client registration disabled
- Forward-headers strategy: NATIVE with internal-proxies allowlist
- server.error.* hardened (no stacktrace / message leak)
- Dependency: Spring Boot 3.5.5, SBA 3.4.5 (aligned with README)
- Java version enforced to [21,25)
- Dockerfile: pinned Temurin 21

## [1.0.0]

### Added
- Spring Boot Admin Server on Spring Boot 3.5.5 + Java 21
- `de.codecentric:spring-boot-admin-starter-server` 3.4.5 integration
- Reactive client lib (webflux) for live event streams from managed instances
- Configurable UI title and branding via `application.yml`
- Custom context-path `/admin/sba` for reverse-proxy compatibility
- `forward-headers-strategy: framework` honoring X-Forwarded-* from nginx
- Minimal actuator surface (health, info) — SBA's own management endpoints stay narrow
- Multi-stage Dockerfile:
  - Build stage: Maven 3.9 + JDK 21 with cached `dependency:go-offline` layer
  - Runtime stage: `eclipse-temurin:21-jre-alpine` with non-root user `sba`
- Configurable `JAVA_OPTS` for memory tuning at runtime
