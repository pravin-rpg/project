FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Create user to run the app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
