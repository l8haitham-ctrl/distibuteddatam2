FROM maven:3.9-eclipse-temurin-21 AS development
WORKDIR /workspace
CMD ["mvn", "spring-boot:run"]

FROM development AS build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B verify

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN apk add --no-cache curl && addgroup -S atlas && adduser -S atlas -G atlas
WORKDIR /app
COPY --from=build --chown=atlas:atlas /workspace/target/data-explorer-0.3.0.jar app.jar
USER atlas
EXPOSE 8000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
