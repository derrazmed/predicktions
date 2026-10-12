# syntax=docker/dockerfile:1

# =============================================================================
# Predicktions backend — multi-stage image
#
# Stage 1 (build) : compiles and packages the Spring Boot executable JAR using
#                   the project's own Maven Wrapper, so no Java/Maven is needed
#                   on the host.
# Stage 2 (runtime): minimal Java 21 JRE image, runs as a non-root user and
#                   contains only the packaged JAR.
#
# NOTE ON TESTS
# The project's test suite is Testcontainers-based and therefore requires a
# running Docker daemon, which is not available inside a normal `docker build`.
# Packaging here intentionally uses `-DskipTests` (test sources are still
# compiled) so the image can be built from a clean checkout. The full suite is
# run separately — see the "Testing" section of the repository README:
#     ./mvnw clean test
# =============================================================================

# -----------------------------------------------------------------------------
# Build stage
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Resolve dependencies first so this layer is cached across source-only changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

# Copy sources and package the executable JAR.
COPY src ./src
RUN ./mvnw -B -ntp clean package -DskipTests

# -----------------------------------------------------------------------------
# Runtime stage
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime

# Run as an unprivileged, non-root user.
RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid app --home-dir /app --shell /usr/sbin/nologin app

WORKDIR /app

# Only the packaged executable JAR is copied from the build stage.
COPY --from=build --chown=app:app /app/target/*.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
