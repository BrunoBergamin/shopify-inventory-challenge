# Build stage: compile with the Maven wrapper so the image does not depend on a local Maven.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Runtime stage: only the JRE and the jar.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/target/inventory-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
