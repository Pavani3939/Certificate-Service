# Stage 1: Build the Spring Boot Application
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy pom.xml and source files
COPY pom.xml .
COPY src ./src

# Build production jar skipping tests (tests verified during CI)
RUN mvn clean package -DskipTests

# Stage 2: Lightweight Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy compiled jar from builder stage
COPY --from=builder /app/target/certificate-service-*.jar app.jar

# Configuration environment variables
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=local
EXPOSE 8080

# Run Spring Boot application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
