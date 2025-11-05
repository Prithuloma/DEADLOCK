# Use Java 21 instead of 17
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code
COPY src ./src

# Build the application JAR
RUN mvn clean package -DskipTests

# Expose the port
EXPOSE 8080

# Run the built app
ENTRYPOINT ["java", "-jar", "target/deadlock-detector-1.0.0.jar"]




