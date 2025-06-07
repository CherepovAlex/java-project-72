# Этап сборки (Builder)
FROM gradle:8.13.0-jdk21 as builder

# Создаем рабочую директорию /app (не /app/app)
WORKDIR /app

# Копируем ВСЕ содержимое папки app из хоста в /app контейнера
COPY app .

# Теперь Gradle файлы находятся в /app/
RUN gradle --no-daemon installDist

# Production-образ
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Устанавливаем PostgreSQL client (опционально)
RUN apt-get update && \
    apt-get install -y gnupg2 && \
    echo "deb http://apt.postgresql.org/pub/repos/apt jammy-pgdg main" > /etc/apt/sources.list.d/pgdg.list && \
    curl https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add - && \
    apt-get update && \
    apt-get install -y --no-install-recommends postgresql-client-16 && \
    rm -rf /var/lib/apt/lists/*

# Копируем собранное приложение из builder
COPY --from=builder /app/build/install/app /app

RUN chmod +x /app/bin/app

ENV PORT=7070
CMD ["./bin/app"]