# syntax=docker/dockerfile:1

# ---- Build stage: compiles the application with the Maven Wrapper ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Dependencies first, so they are cached while only the source code changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests run in CI and locally (they need Docker for Testcontainers), not inside the image build.
RUN ./mvnw -B -q package -DskipTests \
	&& java -Djarmode=tools -jar target/accounts-service-*.jar extract --layers --launcher --destination target/extracted

# ---- Runtime stage: minimal JRE image, non-root user ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

# Layers ordered from least to most frequently changed to maximise Docker layer reuse.
COPY --from=build --chown=app:app /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/application/ ./

USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
