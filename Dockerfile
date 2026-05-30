# ────────────────────────────────────────────────────────────────
# Spring Boot Admin Server — multi-stage build
# Stage 1 builds the fat-jar; stage 2 runs it on a minimal JRE.
# ────────────────────────────────────────────────────────────────

FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app

# Maven dependency cache layer — invalidates only when the pom changes,
# not when a Java file changes. dependency:go-offline can flake on
# transitive timeouts, so --fail-never lets the build proceed to the
# package step which downloads any missing leftovers anyway.
COPY pom.xml .
RUN mvn -B dependency:go-offline --fail-never

COPY src ./src
RUN mvn -B -DskipTests package

# ────────────────────────────────────────────────────────────────
# Runtime image
# ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user — nothing in this container needs root.
RUN addgroup -S sba && adduser -S sba -G sba
USER sba

# Explicit name avoids the *.jar.original ambiguity from spring-boot-maven-plugin
# (the repackage goal produces both airwatch-sba-1.0.0.jar and *.jar.original).
COPY --from=build /app/target/airwatch-sba-1.0.0.jar app.jar

# SBA UI + actuator. Compose binds this to 127.0.0.1:13099 so the dashboard
# is loopback-only; do NOT publish to 0.0.0.0 in production stacks.
EXPOSE 8080

# Default JVM flags — operators can extend via JAVA_OPTS in compose.
#   UseContainerSupport + MaxRAMPercentage=75  — sized to the cgroup heap budget
#   ExitOnOutOfMemoryError                      — escalate OOM to a container
#                                                 exit so compose's restart
#                                                 policy cycles the JVM instead
#                                                 of leaving it degraded.
ENV JAVA_OPTS_DEFAULT="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# DisableAttachMechanism blocks jcmd/jmap/jstack heap-dump from a co-located shell.
# Egd flag avoids the JDK's slow /dev/random fallback.
ENV JAVA_TOOL_OPTIONS="-XX:+DisableAttachMechanism -Djava.security.egd=file:/dev/urandom"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS_DEFAULT $JAVA_OPTS -jar app.jar"]
