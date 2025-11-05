FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy Maven files first
COPY pom.xml .
COPY src ./src

# Build the JAR inside the container
RUN ./mvnw clean package -DskipTests

# Copy the built JAR to the container
COPY target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]


