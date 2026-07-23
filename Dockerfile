# ---- Stage 1: Build ----
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

# Copy Gradle wrapper and build files first (cache dependencies)
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Download dependencies (cached unless build.gradle changes)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q || true

# Copy source code and build the fat JAR
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:25-jre-noble

WORKDIR /app

# Install curl for Docker healthchecks
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r flowero && useradd -r -g flowero flowero

# Copy the built JAR
COPY --from=build /app/build/libs/*.jar app.jar

# Expose both ports:
#   8999 — REST API (service registration, discovery, heartbeats)
#   3999 — Dashboard (proxied by Nginx at discovery.panomete.com)
EXPOSE 8999 3999

# Run as non-root
USER flowero

# JVM flags per SAD: 256 MB total, 192 MB heap
ENTRYPOINT ["java", \
    "-Xms128m", \
    "-Xmx192m", \
    "-XX:+UseZGC", \
    "-XX:MaxHeapFreeRatio=20", \
    "-XX:MinHeapFreeRatio=10", \
    "-jar", "app.jar"]
