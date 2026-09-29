FROM gradle:8.8-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle .
COPY settings.gradle .
RUN gradle dependencies --no-daemon
COPY src src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
