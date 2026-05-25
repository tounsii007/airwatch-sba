# AirWatch SBA — Spring Boot Admin Server

> JVM/Actuator-Dashboard für die AirWatch-API-Replikas. Liefert eine fertige UI für Health, Beans, Environment, Threaddump, Heapdump, Scheduled Jobs, Loggers (mit Live-Level-Änderung), Caches, HTTP-Trace, Metrics und `/env` — ohne dass wir das in unserem eigenen Dashboard nachbauen.

[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Boot Admin](https://img.shields.io/badge/SBA-3.4-6DB33F)](https://docs.spring-boot-admin.com)

---

## Was macht der Service?

- Lauscht auf Port `8080` (im Compose über `SBA_PORT` gemappt).
- Akzeptiert `POST /instances`-Registrierungen der API-Replikas (die `spring-boot-admin-starter-client` mitbringen).
- Pollt die Actuator-Endpunkte jeder registrierten Instanz alle 10 Sekunden.
- Rendert alles in einer einheitlichen Web-UI unter `/admin/sba/`.

## Was er bewusst NICHT macht

- **Keine Persistenz** — Registry ist in-memory. Nach einem SBA-Restart rebuilden die Registrierungen sich innerhalb von ~30 s automatisch, weil die API-Clients regelmäßig re-registrieren.
- **Keine Auth in der App selbst** — die Route ist hinter dem Admin-NGINX-Port (loopback-only) gekapselt; die `/admin/sba/**`-Route auf NGINX wird vom `AdminAuthFilter` am Edge abgesichert.

## Tech-Stack

| Bereich | Wert |
|---|---|
| JVM | Java 21 (Temurin) |
| Framework | Spring Boot 3.5.5 |
| SBA | `de.codecentric:spring-boot-admin-starter-server` 3.4.5 |
| Web | spring-boot-starter-web + spring-boot-starter-webflux (für SSE-Streams) |
| Templating | Thymeleaf (transitive, für SBA-UI) |
| Build | Maven 3.9 |
| Image | `eclipse-temurin:21-jre-alpine` (non-root) |

## Konfiguration

`src/main/resources/application.yml`:

| Property | Default | Zweck |
|---|---|---|
| `server.port` | `8080` | HTTP-Port |
| `server.servlet.context-path` | `/admin/sba` | SBA hängt hinter NGINX bei `/admin/sba` |
| `server.forward-headers-strategy` | `framework` | Honoriert `X-Forwarded-*` Header von NGINX |
| `spring.boot.admin.ui.title` | `AirWatch · JVM Admin` | Browser-Tab-Titel |
| `spring.boot.admin.ui.brand` | `<strong>AIRWATCH</strong>…` | Brand im UI-Header |
| `management.endpoints.web.exposure.include` | `health,info` | SBA selbst legt nur ein Minimum offen |

## Lokales Starten

### Mit Maven

```bash
./mvnw spring-boot:run
# → http://localhost:8080/admin/sba/
```

### Mit Docker

```bash
docker build -t airwatch-sba:local .
docker run --rm -p 8080:8080 -e JAVA_OPTS="-Xmx256m" airwatch-sba:local
# → http://localhost:8080/admin/sba/
```

### Im Compose-Stack

Wird in `airwatch/docker-compose.yml` als `sba`-Service definiert. Die API-Replikas registrieren sich automatisch über `spring.boot.admin.client.url`.

## Build

```bash
./mvnw -DskipTests package          # Erzeugt target/airwatch-sba-1.0.0.jar (Fat JAR)
docker build -t airwatch-sba:1.0.0 .
```

Das Docker-Image ist Multi-Stage:
1. **Build-Stage** — Maven + JDK 21, Cache-Layer für Dependencies (`mvn dependency:go-offline`)
2. **Runtime-Stage** — `eclipse-temurin:21-jre-alpine` mit non-root User `sba`

## Endpunkte

| Pfad | Zweck |
|---|---|
| `GET /admin/sba/` | SBA-UI (Dashboard) |
| `GET /admin/sba/applications` | Liste registrierter Instanzen |
| `POST /admin/sba/instances` | Registrierungs-Endpoint für API-Clients |
| `GET /admin/sba/actuator/health` | SBA-eigener Health-Check |

## Operations

### Wenn die UI 404t mit `ERR_NAME_NOT_RESOLVED`

Häufige Ursache: NGINX vor SBA setzt nicht den `Host`-Header korrekt. Prüfe in der NGINX-Config:

```nginx
location /admin/sba/ {
    proxy_pass http://sba:8080;
    proxy_set_header Host $http_host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Port $server_port;
}
```

Die `forward-headers-strategy: framework` in `application.yml` sorgt dafür, dass SBA die Header respektiert.

### Wenn keine Instanzen erscheinen

1. Loggen die API-Replikas SBA-Registrierungs-Erfolge? (`grep "Application registered" api-replika-logs`)
2. Stimmt `spring.boot.admin.client.url` in den Replika-Configs?
3. Sind beide Container im selben Compose-Netzwerk?

## Sicherheit

Siehe [SECURITY.md](SECURITY.md).
Die SBA-UI selbst hat keine Auth — Schutz erfolgt am NGINX-Edge via `AdminAuthFilter`.

## Verwandte Repos

- **[airwatch](https://github.com/tounsii007/airwatch)** — Compose-Orchestrierung
- **[airwatch-api](https://github.com/tounsii007/airwatch-api)** — Spring-Boot-API-Replikas (SBA-Clients)

## Lizenz

© 2026 Ridha Abderrahmen. Alle Rechte vorbehalten.
