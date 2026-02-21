FROM gradle:8.8-jdk21 AS build

WORKDIR /home/gradle/src
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src

RUN chmod +x gradlew

RUN ./gradlew clean installDist -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=build /home/gradle/src/build/install/execution-service/ /app/

USER app

EXPOSE 8082

HEALTHCHECK --interval=10s --timeout=3s --retries=5 CMD wget -qO- http://localhost:8082/health || exit 1

CMD ["./bin/execution-service"]
