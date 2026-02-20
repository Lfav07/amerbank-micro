FROM maven:3.9-amazoncorretto-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

FROM amazoncorretto:21-alpine

WORKDIR /app

RUN addgroup spring && adduser -S spring -G spring

RUN apk add --no-cache curl

COPY --from=build /app/target/*.jar app.jar

RUN chown spring:spring app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
