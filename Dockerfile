# 1단계: 빌드
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# 2단계: 실행 (빌드 산물만 가져와서 이미지 용량 줄임)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]