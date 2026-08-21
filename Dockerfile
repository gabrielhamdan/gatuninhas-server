# syntax=docker/dockerfile:1

# ---------- Etapa de build (producao) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Etapa de desenvolvimento (hot reload + debug remoto) ----------
FROM maven:3.9-eclipse-temurin-21 AS dev
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
EXPOSE 8080 5005
ENTRYPOINT ["mvn", "spring-boot:run", "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"]

# ---------- Etapa de runtime (producao) ----------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]