# Multi-stage build for reproducible Docker builds
# Context should be set to apps/backend/ (not apps/backend/app/)
# See: docs/adr/006-modular-monolith-deployment.md

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy only pom files first to leverage Docker cache for dependencies
COPY pom.xml ./
COPY libs/smart-be-core/pom.xml ./libs/smart-be-core/
COPY libs/smart-be-security/pom.xml ./libs/smart-be-security/
COPY libs/smart-be-tenant/pom.xml ./libs/smart-be-tenant/
COPY libs/smart-be-redis/pom.xml ./libs/smart-be-redis/
COPY libs/smart-be-opensearch/pom.xml ./libs/smart-be-opensearch/
COPY libs/smart-be-observability/pom.xml ./libs/smart-be-observability/
COPY services/property-be/pom.xml ./services/property-be/
COPY services/system-be/pom.xml ./services/system-be/
COPY services/retail-be/pom.xml ./services/retail-be/
COPY app/pom.xml ./app/

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B -q || true

# Copy source code
COPY libs/ ./libs/
COPY services/ ./services/
COPY app/ ./app/

# Build the unified application
RUN mvn -pl app -am package -DskipTests -B -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Smart DX Team"

WORKDIR /app

# JVM memory optimization for VPS (modular monolith - single JVM)
# Target: 512-768MB for all domains combined
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:MaxMetaspaceSize=192m -XX:+UseStringDeduplication"

# Copy the application from builder stage
COPY --from=builder /build/app/target/smart-dx-app.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

EXPOSE 8080
