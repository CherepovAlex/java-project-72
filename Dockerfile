FROM eclipse-temurin:21-jre

WORKDIR /app

COPY app/gradle gradle
COPY app/build.gradle.kts .
COPY app/settings.gradle.kts .
COPY app/gradlew .

RUN chmod +x gradlew

RUN ./gradlew --no-daemon dependencies || true

COPY app/src src
COPY app/config config

RUN ./gradlew --no-daemon build

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/HexletJavalin-1.0-SNAPSHOT-all.jar .

ENV JAVA_OPTS="-Xmx512M -Xms512M"
EXPOSE 7070

CMD ["java", "-jar", "HexletJavalin-1.0-SNAPSHOT-all.jar"]