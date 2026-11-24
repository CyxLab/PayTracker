# ===== Stage 1 : build =====
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /app

# 1. Copier en premier ce qui change rarement (cache Docker)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 2. Rendre le wrapper exécutable
RUN chmod +x mvnw

# 3. Télécharger les dépendances (couche cachée tant que pom.xml ne change pas)
RUN ./mvnw -B dependency:go-offline -DskipTests

# 4. Copier le code source et builder
COPY src src
RUN ./mvnw -B package -DskipTests

# ===== Stage 2 : runtime =====
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

# Utilisateur non-root (securite, NFR-08)
RUN groupadd -r paytrack && useradd -r -g paytrack paytrack

COPY --from=build /app/target/*.jar app.jar
RUN chown paytrack:paytrack app.jar
USER paytrack

EXPOSE 8080

# -Djava.security.egd evite les demarrages lents lies a SecureRandom sur Linux
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]