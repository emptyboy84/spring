# 1. 빌드 단계: Maven을 이용해 자바 코드를 압축(빌드)합니다.
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 2. 실행 단계: 가벼운 자바 환경을 가져와서 빌드된 서버를 실행합니다.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 위 1단계에서 만들어진 jar 파일만 쏙 빼옵니다.
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
