FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Smart DX Team"

WORKDIR /app

# JVM memory optimization for VPS
ENV JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:+UseStringDeduplication"

# Copy the application
COPY target/property-be.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

EXPOSE 8081
