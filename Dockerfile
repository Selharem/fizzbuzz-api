FROM eclipse-temurin:25-jdk-alpine@sha256:3fd2d245c4e0eba615fe366a71b8bd25f5db7104f53e4026b24bf508b880bd2a AS builder

WORKDIR /workspace

COPY gradle gradle
COPY gradlew build.gradle settings.gradle gradle.properties gradle.lockfile ./
RUN ./gradlew dependencies --no-daemon

COPY config config
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine@sha256:3c0a9084927a221ccd1d007fcaf614465672c0af37aaa834c5184483afe56d61

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
