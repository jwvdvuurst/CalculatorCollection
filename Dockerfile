# Multi-stage build: First stage builds the JAR, second stage runs it

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/CalCol-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Create directories for data and uploads
RUN mkdir -p /app/data /app/uploads

# Set the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]