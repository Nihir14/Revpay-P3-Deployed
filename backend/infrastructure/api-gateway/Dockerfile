FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
# --- THE FIX IS IN THIS LINE BELOW ---
ENTRYPOINT ["java", "--add-opens", "java.base/java.net=ALL-UNNAMED", "-jar", "app.jar"]