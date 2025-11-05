# Use Java 21 base image
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy only the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code
COPY src ./src

# Build the JAR inside the container
RUN mvn clean package -DskipTests

# Expose port 8080 for the app
EXPOSE 8080

# Run the built JAR
ENTRYPOINT ["java", "-jar", "target/deadlock-detector-1.0.0.jar"]




