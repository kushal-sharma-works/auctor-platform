FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=build /workspace/target/definition-service-*.jar /app/app.jar

USER app

EXPOSE 8081 9090

HEALTHCHECK --interval=10s --timeout=3s --retries=5 CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
