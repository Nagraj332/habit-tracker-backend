FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY habit-tracker-backend/pom.xml .
RUN mvn dependency:go-offline
COPY habit-tracker-backend/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx300m", "-jar", "app.jar"]
