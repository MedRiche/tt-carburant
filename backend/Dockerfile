# syntax=docker/dockerfile:1.4
# ─────────────────────────────────────────────
# STAGE 1 — Build (Maven + JDK 17)
# ─────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom first
COPY pom.xml .
COPY src ./src


# Build directly — skip dependency:go-offline (it pulls way more
# plugins than needed for packaging and increases failure surface
# on unstable connections). Retry loop handles transient network drops.
RUN --mount=type=cache,target=/root/.m2 \
    for i in 1 2 3 4 5; do \
      mvn clean package -DskipTests -B \
        -Dmaven.wagon.http.retryHandler.count=5 \
        -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
        -Dmaven.wagon.http.pool=false \
        -Dhttp.keepAlive=false \
        -Dmaven.wagon.http.connectionTimeout=60000 \
        -Dmaven.wagon.http.readTimeout=60000 \
      && break || { echo "Build attempt $i failed, retrying..."; sleep 10; }; \
    done

# ─────────────────────────────────────────────
# STAGE 2 — Runtime (JRE 17)
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

USER appuser

COPY --from=build /app/target/tt-carburant-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]