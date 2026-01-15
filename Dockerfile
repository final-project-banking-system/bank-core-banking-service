FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /opt/app
RUN useradd -r -u 10001 -m appuser
COPY --from=build /build/target/*.jar /opt/app/app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/opt/app/app.jar"]