# Use Java 21 base image
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the JAR inside the container
RUN mvn clean package -DskipTests

# Expose port 8080
EXPOSE 8080

# Copy the JAR from the target folder after build
CMD ["java", "-jar", "/app/target/deadlock-detector-1.0.0.jar"]






