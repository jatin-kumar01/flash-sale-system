# =============================================================================
# Stage 1: Extract Spring Boot Layered JAR Components
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS extractor

ARG SERVICE_NAME
WORKDIR /extracted

# Copy the built fat JAR produced by spring-boot-maven-plugin
COPY ${SERVICE_NAME}/target/${SERVICE_NAME}-*.jar app.jar

# Deconstruct the fat JAR into optimized Docker image cache layers
RUN java -Djarmode=layertools -jar app.jar extract

# =============================================================================
# Stage 2: Hardened, Non-Root Alpine Runtime Image
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create an unprivileged system user and group to run the microservice safely
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copy layers sequentially: least-frequently changed to most-frequently changed
COPY --from=extractor --chown=spring:spring /extracted/dependencies/ ./
COPY --from=extractor --chown=spring:spring /extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=spring:spring /extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=spring:spring /extracted/application/ ./

# Expose JVM memory & GC ergonomics for containerized cgroups
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
