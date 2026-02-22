# Multi-stage build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy gradle files
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy source code
COPY src ./src

# Make gradlew executable and build the application
RUN chmod +x ./gradlew && \
    ./gradlew clean bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Install tzdata for timezone support
RUN apt-get update && apt-get install -y tzdata && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/MoonPhase-1.0-SNAPSHOT.jar app.jar
COPY README.md .

# Create a non-root user for security
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser


# Run the application with the required JVM arguments
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.util=ALL-UNNAMED", "-XX:+EnableDynamicAgentLoading", "-jar", "app.jar"]
