FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -g 10001 -S app \
    && adduser -u 10001 -S app -G app \
    && mkdir -p /data \
    && chown app:app /data
WORKDIR /app

COPY --from=build /app/target/wazzup-backend-*.jar app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
