# =============================================================================
# Stage 1: Build the native image
# =============================================================================
FROM ghcr.io/graalvm/native-image-community:25-ol9 AS builder

# Install necessary build tools
RUN microdnf install -y findutils tar gzip unzip && \
    microdnf clean all

WORKDIR /app

# Copy Gradle wrapper and config files first (for better layer caching)
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (cached layer if build files haven't changed)
RUN ./gradlew dependencies --no-daemon --quiet || true

# Copy source code
COPY src/ src/

# Compile to native image using GraalVM
# -Pspring.aot.enabled=true ensures AOT processing runs before native compilation
RUN ./gradlew nativeCompile \
    --no-daemon \
    --info \
    -Dspring.aot.enabled=true \
    -x test

# =============================================================================
# Stage 2: Minimal runtime image
# =============================================================================
FROM oraclelinux:9-slim AS runtime

# Install runtime dependencies (SSL certs, timezone data)
RUN microdnf install -y \
    ca-certificates \
    tzdata \
    && microdnf clean all

# Create non-root user for security
RUN groupadd --system --gid 1001 appgroup && \
    useradd --system --uid 1001 --gid appgroup --no-create-home appuser

WORKDIR /app

# Copy the native binary from the builder stage
# The binary name matches your project's artifact name from settings.gradle.kts
COPY --from=builder /app/build/native/nativeCompile/ ./

# Set correct ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose default Spring Boot port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD ["/bin/sh", "-c", "curl -f http://localhost:8080/actuator/health || exit 1"]

# Run the native binary
# Replace 'duckemu-core' with your artifact name from settings.gradle.kts
ENTRYPOINT ["./duckcore"]
