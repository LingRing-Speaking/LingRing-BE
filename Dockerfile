FROM eclipse-temurin:25-jdk

COPY build/libs/*-SNAPSHOT.jar /app.jar

ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["java", "-jar", "/app.jar"]