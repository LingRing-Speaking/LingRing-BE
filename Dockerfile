FROM eclipse-temurin:25-jdk

COPY build/libs/*-SNAPSHOT.jar /app.jar

# SPRING_PROFILES_ACTIVE는 외부에서 주입한다.
# - dev: docker-compose-dev.yml 또는 로컬 .env에서 SPRING_PROFILES_ACTIVE=dev
# - prod: prod EC2 user-data가 SSM Parameter Store에서 받아 .env로 주입

ENTRYPOINT ["java", "-jar", "/app.jar"]