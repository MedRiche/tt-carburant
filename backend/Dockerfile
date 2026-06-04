# ─────────────────────────────────────────────
#  STAGE 1 — Build  (Maven + JDK 17)
# ─────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom first → cache dependencies layer
COPY pom.xml .


# Copy source and build JAR (skip tests → tests run in Jenkins stage)
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─────────────────────────────────────────────
#  STAGE 2 — Runtime  (JRE only, smaller image)
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/tt-carburant-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]