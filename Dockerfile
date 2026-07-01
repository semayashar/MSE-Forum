FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace/app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/

RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /workspace/app/target/*.jar /app/app.jar

USER app

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
