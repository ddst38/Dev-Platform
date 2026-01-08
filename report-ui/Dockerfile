FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copier le JAR déjà compilé
COPY report-ui-1.0.0-SNAPSHOT.jar report-ui-1.0.0-SNAPSHOT.jar

# Exposer les deux ports utilisés par l'application
EXPOSE 8090

# Lancer l'application
ENTRYPOINT ["java","-jar","report-ui-1.0.0-SNAPSHOT.jar"]