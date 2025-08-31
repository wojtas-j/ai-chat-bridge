FROM gradle:8.10-jdk21 AS builder
WORKDIR /app

COPY build.gradle settings.gradle gradle.properties ./
COPY src ./src

# Build jar without tests
RUN gradle clean build -x test --no-daemon

FROM openjdk:21-jdk-slim
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# Run the application with preview features enabled
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
