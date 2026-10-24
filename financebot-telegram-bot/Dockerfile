# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress -Dmaven.test.skip=true clean package

FROM eclipse-temurin:21-jre AS runtime

RUN groupadd --gid 10001 financebot \
    && useradd --uid 10001 --gid 10001 --no-create-home --home-dir /app --shell /usr/sbin/nologin financebot

WORKDIR /app
COPY --from=build /app/target/financebot-telegram-bot-0.0.1-SNAPSHOT.jar /app/app.jar

USER 10001:10001

EXPOSE 8081

CMD ["java", "-jar", "/app/app.jar"]
