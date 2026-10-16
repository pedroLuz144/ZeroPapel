# syntax=docker/dockerfile:1.7

FROM node:22-bookworm-slim AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk AS backend
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw -q -B dependency:go-offline
COPY src/ src/
COPY --from=frontend /build/src/main/resources/static/ src/main/resources/static/
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw -q -B package -DskipTests \
    && cp target/ZeroPapel-*.jar /build/zeropapel.jar

FROM eclipse-temurin:21-jre
ENV TZ=America/Sao_Paulo \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
RUN groupadd --system zeropapel && useradd --system --gid zeropapel --home /app zeropapel
WORKDIR /app
COPY --from=backend /build/zeropapel.jar ./zeropapel.jar
USER zeropapel
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/zeropapel.jar"]
