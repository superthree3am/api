# Gunakan base image yang sesuai JDK 21
FROM eclipse-temurin:21-jdk AS build

# Set direktori kerja di dalam container
WORKDIR /app

# Copy file Maven dan project
COPY . .

# Build aplikasi
RUN ./mvnw clean package -DskipTests

# Gunakan JDK ringan untuk running
FROM eclipse-temurin:21-jre

WORKDIR /app

# Salin file hasil build dari tahap sebelumnya
COPY --from=build /app/target/*.jar app.jar

# Jalankan aplikasi
ENTRYPOINT ["java", "-jar", "app.jar"]
