FROM gradle:8.13.0-jdk21 as builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts /app/
RUN gradle --no-daemon downloadDependencies

COPY src /app/src
RUN gradle --no-daemon installDist

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    postgresql-client-16 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/install/app /app

RUN chmod +x /app/bin/app

ENV PORT=7070
ENV JDBC_DATABASE_URL="jdbc:postgresql://localhost:5432/postgres"
ENV DB_USER=default_user
ENV DB_PASSWORD=default_password

HEALTHCHECK --interval=30s --timeout=5s \
    CMD curl -f http://localhost:$PORT/health || exit 1

CMD ["./bin/app"]