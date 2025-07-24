# Use official OpenJDK base image
FROM eclipse-temurin:21-jre

# Set working directory inside container
WORKDIR /app

# Copy the JAR file to the container
COPY target/*.jar app.jar

# Expose port (adjust if different)
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
