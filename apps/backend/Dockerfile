# Base image
FROM eclipse-temurin:17-jdk-alpine

# Maintainer info (use LABEL instead of deprecated MAINTAINER)
LABEL maintainer="dev"

# Set container timezone and install font libraries (for captchas)
RUN apk --no-cache add tzdata \
    && cp /usr/share/zoneinfo/Asia/Tokyo /etc/localtime \
    && echo "Asia/Tokyo" > /etc/timezone \
    && apk --no-cache add ttf-dejavu fontconfig

# Mount /tmp as an anonymous volume at runtime for portability.
VOLUME /tmp

# Working directory
WORKDIR /app

# Copy files required for build
COPY ./pom.xml ./pom.xml
COPY ./src ./src

# Install Maven and build the application
RUN apk add --no-cache maven \
    && mvn package -DskipTests \
    && mv target/youlai-boot.jar app.jar \
    && rm -rf target \
    && apk del maven

# Command to run the container (JSON form)
CMD ["java", "-Xms512m", "-Xmx512m", "-Djava.security.egd=file:/dev/urandom", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"]

# Expose container port
EXPOSE 8989
