# Contributing to AirWatch SBA

This service is **deliberately minimal** — one `@EnableAdminServer` annotation and a YAML config. Most "features" come from the upstream `spring-boot-admin-starter-server` library; we only customize:

- Context path (`/admin/sba`)
- UI branding
- Forwarded-header strategy
- Logging levels

If you find yourself wanting to add a Spring `@Component` or a controller, **stop and ask first** — it probably belongs in `airwatch-api` instead. The SBA service has no domain logic by design.

## Prerequisites

- Java 21 (Temurin recommended)
- Maven 3.9+

## Build + Run Locally

```bash
./mvnw -DskipTests package
java -jar target/airwatch-sba-1.0.0.jar
# → http://localhost:8080/admin/sba/
```

Or via Docker:

```bash
docker build -t airwatch-sba:dev .
docker run --rm -p 8080:8080 airwatch-sba:dev
```

## Branch + Commit Conventions

- Branches: `feat/<topic>`, `fix/<topic>`, `chore/<topic>`, `docs/<topic>`
- Commits: [Conventional Commits](https://www.conventionalcommits.org/) — e.g.
  - `chore(deps): bump Spring Boot Admin to 3.4.6`
  - `fix(config): correct context-path for forwarded headers`
  - `docs(readme): clarify nginx Host header requirement`

## What You Can Change

✅ Yes:
- Bump dependency versions in `pom.xml`
- Tighten/loosen `application.yml` (with rationale)
- Update Dockerfile for newer base images
- Improve docs

❌ No (without prior discussion):
- Add Spring `@Component`/`@Service`/`@RestController`
- Persistence (Postgres, Redis, etc.) — SBA is meant to be stateless here
- Auth at the application layer — edge auth (`AdminAuthFilter` at nginx) is the chosen model

## Testing Changes

```bash
# Build cleanly
./mvnw clean package

# Boot the jar locally
java -jar target/airwatch-sba-1.0.0.jar &
sleep 10

# Sanity-check the UI is up
curl -sI http://localhost:8080/admin/sba/ | head -1   # → 200 OK
curl -sI http://localhost:8080/admin/sba/applications | head -1
curl -s http://localhost:8080/admin/sba/actuator/health | grep status   # → "UP"

# Stop
pkill -f airwatch-sba
```

In the full compose stack:

```bash
# From the airwatch/ repo
docker compose up -d sba
docker compose up -d --build api
# Open Grafana / browse http://localhost:8080/admin/sba/
# After ~30 s the API replicas should appear in the SBA UI
```

## Dockerfile Hygiene

- Base images pinned to a specific Temurin tag — not `latest`
- Build dependencies installed in a separate cache layer
- Final image must run as non-root (`USER sba`)
- No secrets baked into the image — env-only

## Pull Request Checklist

- [ ] `./mvnw clean package` succeeds
- [ ] Image builds: `docker build -t airwatch-sba:test .`
- [ ] Image runs as non-root (`docker run --rm airwatch-sba:test id` → `uid=...(sba)`)
- [ ] UI loads on `http://localhost:8080/admin/sba/`
- [ ] In a full compose stack, API replicas register and appear
- [ ] CHANGELOG.md updated under `## [Unreleased]`
- [ ] No new application code introduced (config + deps only)

## Security

Vulnerabilities: see [SECURITY.md](SECURITY.md). Do not file public issues.

## License

Contributions are licensed under the project's license.
