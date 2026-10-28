# CampusDesk backend: build with Maven, run on a JRE 21 (no build tools in the final image).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Dependencies first, so they stay cached while only the code changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S campusdesk && adduser -S campusdesk -G campusdesk
COPY --from=build /app/target/campusdesk-backend-*.jar app.jar
USER campusdesk
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
