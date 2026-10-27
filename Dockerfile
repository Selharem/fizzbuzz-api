FROM eclipse-temurin:25-jdk-alpine@sha256:1e20ead9eec7234db293f1913438090d533ed47c0b9e12dc8c7627a802a44ed6 AS builder

WORKDIR /workspace

COPY gradle gradle
COPY gradlew build.gradle settings.gradle gradle.properties gradle.lockfile ./
RUN ./gradlew dependencies --no-daemon

COPY config config
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine@sha256:b7528962e65d43064d8a8b67ba175367896bc948363ea2d3fcd7f10d34e1f048

LABEL org.opencontainers.image.title="FizzBuzz API" \
      org.opencontainers.image.description="Production-ready configurable FizzBuzz REST API"

RUN addgroup --system app && adduser --system --ingroup app app

WORKDIR /app
COPY --from=builder --chown=app:app /workspace/build/libs/fizzbuzz-api-*.jar app.jar
RUN chmod 0444 /app/app.jar

ENV LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs

USER app
EXPOSE 8080 8081

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
    CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
