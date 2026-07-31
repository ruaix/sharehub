FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S sharehub && adduser -S sharehub -G sharehub
COPY --from=build /workspace/target/sharehub-backend-*.jar app.jar
USER sharehub
EXPOSE 8787
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-XX:InitialRAMPercentage=20","-jar","/app/app.jar"]
