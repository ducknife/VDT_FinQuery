# syntax=docker/dockerfile:1
# FinQuery core (Spring Boot): build bằng Maven trong Docker, chạy bằng JRE 21.
# .env KHÔNG chép vào image: docker-compose.yml gắn file vào /app/.env lúc chạy.

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
# tải thư viện trước (lớp này dùng lại khi chỉ sửa code); bộ đệm Maven giữ giữa các lần build
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline -DskipTests || true
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM eclipse-temurin:21.0.5_11-jre-jammy
# PDFBox vẽ ảnh trang PDF (khung trích dẫn) cần fontconfig và font cơ bản
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /src/target/finq-*.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
