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

# Copie le fichier pom.xml (gestion des dépendances)
# Permet à Docker d'utiliser le cache si ce fichier ne change pas
COPY pom.xml .

# Copie le code source (src/)
COPY src ./src

# Compile le projet :
# - clean      → supprime anciens fichiers compilés
# - package    → génère le .jar
# -DskipTests  → ne lance pas les tests (plus rapide)
# -B           → mode non interactif (CI) ; sans -q pour que les erreurs Maven s’affichent dans docker build
RUN mvn -B -DskipTests clean package

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