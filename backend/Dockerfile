# Build:  docker build -t loan-backend .
# Run:    docker run -p 8080:8080 --env-file .env loan-backend
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home loan
USER loan
WORKDIR /app
COPY --from=build /src/target/loan-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
