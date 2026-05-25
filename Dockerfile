# ────────────────────────────────────────────────────────────────
# Spring Boot Admin Server — multi-stage build
# Stage 1 builds the fat-jar; stage 2 runs it on a minimal JRE.
# ────────────────────────────────────────────────────────────────

FROM maven:3.9-eclipse-temurin-21 AS build
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

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
