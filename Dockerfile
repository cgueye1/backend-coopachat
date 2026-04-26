# ════════════════════════════════════════════════════════════════════════════
# DOCKERFILE – APPLICATION SPRING BOOT (JAVA 17)
# ════════════════════════════════════════════════════════════════════════════

# Étape 1 : BUILD  → Compile l’application et génère le fichier .jar
# Étape 2 : RUN    → Lance l’application avec une image légère
# ════════════════════════════════════════════════════════════════════════════

# ═══════════════════════════════════
# ÉTAPE 1 : BUILD (COMPILATION)
# ═══════════════════════════════════
# Utilise une image contenant Maven + Java 17
# Objectif : compiler le projet et générer le fichier JAR

FROM maven:3.9.9-eclipse-temurin-17 AS build

# Définit le dossier de travail dans le conteneur
WORKDIR /app

# 1. On copie UNIQUEMENT le fichier de dépendances (pom.xml)
# Cela permet à Docker de mettre en cache cette couche tant que le fichier ne change pas.
COPY pom.xml .

# 2. On télécharge les dépendances en avance (avec options de secours SSL/TLS)
RUN mvn dependency:go-offline

# 3. On copie ensuite le code source (src/)
COPY src ./src

# 4. On compile le projet :
RUN mvn clean package -DskipTests

# Résultat : fichier .jar généré dans /app/target/
# ═══════════════════════════════════
# ÉTAPE 2 : RUN (EXÉCUTION)
# ═══════════════════════════════════
# Utilise une image plus légère contenant seulement Java 17
# Objectif : exécuter le .jar sans inclure Maven ni le code source(On veut lancer l’application sans garder les outils qui ont servi à la fabriquer.)

FROM eclipse-temurin:17-jre

# Dossier de travail
WORKDIR /app

# Copie le fichier .jar depuis l'étape "build"
COPY --from=build /app/target/*.jar /app/app.jar

# Indique que l'application utilise le port 8082
# (info interne pour Docker )
EXPOSE 8082

# Commande exécutée au démarrage du conteneur :
# - Lance le fichier app.jar
# - Force Spring Boot à utiliser application-docker.properties
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.config.location=classpath:/application-docker.properties"]