# # Build Stage 
# FROM openjdk:21-jdk-slim AS build
# WORKDIR /app
# COPY pom.xml .
# COPY mvnw .          
# COPY .mvn ./.mvn      
# COPY src ./src
# RUN chmod +x ./mvnw 
# RUN ./mvnw clean install -DskipTests

# # Run Stage
# FROM openjdk:21-jdk-slim
# WORKDIR /app
# COPY --from=build /app/target/*.jar app.jar
# COPY src/main/resources/firebase/serviceAccountKey.json /app/serviceAccountKey.json
# ENV FIREBASE_SERVICE_CONFIG_PATH=/app/serviceAccountKey.json
# EXPOSE 8080
# ENTRYPOINT ["java","-jar","app.jar"]


# Build Stage 
FROM openjdk:21-jdk-slim AS build
WORKDIR /app
COPY pom.xml .
COPY mvnw .          
COPY .mvn ./.mvn      
COPY src ./src
RUN chmod +x ./mvnw 
RUN ./mvnw clean install -DskipTests

# Run Stage
FROM openjdk:21-jdk-slim
WORKDIR /app

# Create logs directory with proper permissions
RUN mkdir -p /app/logs && \
    chmod 755 /app/logs

# Copy the built JAR file
COPY --from=build /app/target/*.jar app.jar

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

EXPOSE 8080

# Set environment variable for log path
ENV LOG_PATH=/app/logs

ENTRYPOINT ["java", "-jar", "app.jar"]