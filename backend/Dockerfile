# Use lightweight JDK 17 image
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy the prebuilt JAR from target folder
COPY target/deadlock-detector-1.0.0.jar app.jar

EXPOSE 8080

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]





