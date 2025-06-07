# Этап сборки (Builder)
FROM gradle:8.13.0-jdk21 as builder

WORKDIR /app

# Копируем ВСЕ файлы из поддиректории app
COPY app .

# Устанавливаем рабочую директорию внутри app
WORKDIR /app/app

RUN gradle --no-daemon downloadDependencies
RUN gradle --no-daemon installDist

# Production-образ
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Устанавливаем PostgreSQL client
RUN apt-get update && apt-get install -y --no-install-recommends \
    postgresql-client-16 \
    && rm -rf /var/lib/apt/lists/*

# Копируем собранное приложение
COPY --from=builder /app/app/build/install/app /app

RUN chmod +x /app/bin/app

ENV PORT=7070
CMD ["./bin/app"]