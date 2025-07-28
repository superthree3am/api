FROM openjdk:21-jdk-slim
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=default", "-jar", "/app.jar"]
