# Une seule image contenant TOUT : l'API Spring Boot et l'application Angular.
# Trois étapes de construction ; seule la dernière devient le conteneur qui tourne.

# ---------- Étape 1 : compiler l'application Angular ----------
FROM node:24-slim AS frontend
WORKDIR /frontend
ENV NG_CLI_ANALYTICS=false
# package*.json d'abord : les dépendances ne sont retéléchargées que si elles changent
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npx ng build
# Résultat : /frontend/dist/frontend/browser (HTML, JS, CSS, images)

# ---------- Étape 2 : compiler Spring Boot, avec Angular à l'intérieur du jar ----------
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
# Spring Boot sert tout ce qui est dans src/main/resources/static
COPY --from=frontend /frontend/dist/frontend/browser ./src/main/resources/static
# -DskipTests : le test de démarrage complet a besoin d'une base MySQL, absente pendant le build
RUN mvn -B clean package -DskipTests

# ---------- Étape 3 : image finale, la plus petite possible ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=backend /app/target/*.jar app.jar
USER app

# Le service gratuit de Render a 512 Mo de mémoire : on limite le tas Java.
# SerialGC : le ramasse-miettes le plus économe. TieredStopAtLevel=1 : démarrage plus rapide sur un petit CPU.
ENV SHOW_SQL=false
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx300m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-Duser.timezone=Africa/Tunis", "-jar", "app.jar"]
