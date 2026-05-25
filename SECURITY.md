# Security Policy

## Reporting a vulnerability

**Please do not file a public GitHub issue for security vulnerabilities.**

To report a security problem privately, use GitHub's built-in mechanism:
**Security → Report a vulnerability** (top of this repo on GitHub).

Alternatively, email the maintainer directly (see `git log --format='%ae'` for
the active address).

A useful report includes:

1. A description of the vulnerability and what an attacker could achieve.
2. Reproduction steps — the shorter, the better.
3. The conditions required (authentication state, network position, browser).
4. A suggested mitigation if you have one.

## Threat model — what this service does (and doesn't) protect

The SBA Server has **no authentication of its own** — protection is delegated
to the edge:

- The `/admin/sba/**` route on the upstream nginx is gated by `AdminAuthFilter`
  in `airwatch-api`. Anyone bypassing nginx and reaching the container
  directly gets full admin access.
- The host port is bound to `127.0.0.1` in the compose stack — the container
  is not directly reachable from LAN/WAN.

What this means for vulnerability classification:

| Class | In-scope? | Notes |
|---|---|---|
| Auth bypass at the SBA layer | ❌ Out-of-scope by design | Auth is at the nginx edge — file against `airwatch-api`'s `AdminAuthFilter` |
| XSS in the SBA UI | ✅ In-scope | The SBA-rendered UI is the attack surface for an authenticated operator |
| RCE via SBA actuator proxying | ✅ Critical | SBA forwards to actuator endpoints — a poisoned `info`/`env` payload could escape if not sanitized |
| Container escape from the runtime image | ✅ In-scope | The image runs as non-root user `sba`; any escape is a finding |
| Dependency CVE (SBA / Spring Boot / Webflux / Thymeleaf) | ✅ In-scope | We track via Dependabot |
| Misconfiguration in `application.yml` | ⚠️ Triage case-by-case | Most settings are operational, not security boundaries |
| Vulnerability in `eclipse-temurin:21-jre-alpine` | ✅ In-scope | Bumps land via Dependabot or manual base-image refresh |

## Response timeline

| Severity  | Acknowledgement | Fix target |
|-----------|----------------|-----------|
| Critical  | 1 business day  | 7 days    |
| High      | 2 business days | 14 days   |
| Medium    | 3 business days | 30 days   |
| Low       | 5 business days | 90 days   |

## Supported versions

Only the latest commit on the **default branch** and the most recent tagged
release receive security patches. There are no long-term-support branches.

## Hardening checklist (operator-facing)

Before deploying this service to a non-local environment, verify:

- [ ] nginx in front with `AdminAuthFilter` is reachable; direct container
      port (`8080`) is **not** exposed to LAN/WAN
- [ ] `forward-headers-strategy: framework` stays in `application.yml`
      (so SBA generates URLs against the external host, not internal Docker DNS)
- [ ] The base image tag in the Dockerfile (`eclipse-temurin:21-jre-alpine`)
      is pinned to a digest in production deploys
- [ ] Container runs as non-root (the Dockerfile sets `USER sba`)
- [ ] `JAVA_OPTS` does not enable remote JMX without TLS+auth — never
      open the JMX port to anything but the local host
- [ ] Spring Boot Admin client URL in the API replicas points at the **internal**
      service hostname, not a public address
- [ ] `management.endpoints.web.exposure.include` stays minimal (default:
      `health,info`) — do **not** add `env`, `heapdump`, `threaddump` to
      SBA's own actuator surface

## Coordinated disclosure

After a fix is shipped we will publish a GitHub Security Advisory and credit
the reporter (with their consent). Please allow us the agreed fix window
before any public disclosure or PoC posting.
