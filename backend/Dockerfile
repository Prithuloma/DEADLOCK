FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy project files
COPY . .

# Install Maven
RUN apt-get update && apt-get install -y maven

# Build the JAR
RUN mvn clean package -DskipTests

# Expose port and run the app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/deadlock-detector-1.0.0.jar"]



