# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Note: In actual use, build from parent directory
# mvn -pl services/{domain}-be -am package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# JVM memory optimization
ENV JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m"

# Copy jar from build context (built externally)
COPY target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
