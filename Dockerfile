# --- Build jar di stage lain atau di Jenkins (pipeline sudah mvn package) ---

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
