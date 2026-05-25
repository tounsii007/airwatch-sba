# ────────────────────────────────────────────────────────────────
# Spring Boot Admin Server — multi-stage build
# Stage 1 builds the fat-jar; stage 2 runs it on a minimal JRE.
# ────────────────────────────────────────────────────────────────

FROM maven:3-eclipse-temurin-26 AS build
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
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Non-root user — nothing in this container needs root.
RUN addgroup -S sba && adduser -S sba -G sba
USER sba

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Default JVM flags — operators can extend via JAVA_OPTS in compose.
#   UseContainerSupport + MaxRAMPercentage=75  — sized to the cgroup heap budget
#   ExitOnOutOfMemoryError                      — escalate OOM to a container
#                                                 exit so compose's restart
#                                                 policy cycles the JVM instead
#                                                 of leaving it degraded.
ENV JAVA_OPTS_DEFAULT="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS_DEFAULT $JAVA_OPTS -jar app.jar"]
