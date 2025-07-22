# # --- Build jar di stage lain atau di Jenkins (pipeline sudah mvn package) ---

# FROM eclipse-temurin:17-jre-alpine
# WORKDIR /app
# COPY target/*.jar app.jar
# EXPOSE 8080
# ENTRYPOINT ["java", "-jar", "app.jar"]

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
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]