# # Gunakan base image yang sesuai JDK 21
# FROM eclipse-temurin:21-jdk AS build

# # Set direktori kerja di dalam container
# WORKDIR /app

# # Copy file Maven dan project
# COPY . .

# # Build aplikasi
# RUN ./mvnw clean package -DskipTests

# # Gunakan JDK ringan untuk running
# FROM eclipse-temurin:21-jre

# WORKDIR /app

# # Salin file hasil build dari tahap sebelumnya
# COPY --from=build /app/target/*.jar app.jar

# # Jalankan aplikasi
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
