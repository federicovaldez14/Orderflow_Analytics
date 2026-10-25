# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilar el .jar con Maven (JDK 17) ----------
FROM maven:3.9.16-eclipse-temurin-17-noble AS build
WORKDIR /src

# Primero solo el pom.xml: mientras no cambie, Docker reutiliza la capa de dependencias.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q dependency:go-offline

# Después el código. Las pruebas corren en el CI (mvn verify), aquí no se compilan.
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q package -Dmaven.test.skip=true \
 && cp target/orderflow-analytics-*.jar /app.jar

# ---------- Etapa 2: imagen final solo con el JRE 17 y el .jar ----------
FROM eclipse-temurin:17.0.20.1_1-jre-noble

# Usuario sin privilegios; /app/data guarda la base H2 (se monta como volumen en Compose).
RUN groupadd --system app && useradd --system --gid app --no-create-home app \
 && mkdir -p /app/data && chown app:app /app/data
WORKDIR /app
COPY --from=build --chown=app:app /app.jar app.jar
USER app

# El contenedor no tiene pantalla: solo la web y la API (sin la ventana Swing).
ENV ORDERFLOW_UI_ENABLED=false
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
