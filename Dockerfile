# Base images are pinned by digest. Update the tag and digest together after review.
# maven:3-eclipse-temurin-26
FROM maven:3-eclipse-temurin-26@sha256:029a8e2838ae68238ffb8be407cddbb3f07d4d839c60c6f26c619a69fd184531 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY scripts/resolve-executable-jar.sh ./scripts/resolve-executable-jar.sh
COPY src ./src
RUN mvn -q -DskipTests package spring-boot:repackage \
    && JAR="$(bash scripts/resolve-executable-jar.sh)" \
    && cp "$JAR" /workspace/app.jar

# eclipse-temurin:17-jre-jammy
FROM eclipse-temurin:17-jre-jammy@sha256:475d8e96b4b2bfe08999e5e854755c773af1581acdf959a4545d88f0696a2339
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 loadbalancer \
    && useradd --system --uid 10001 --gid 10001 --home-dir /app --shell /usr/sbin/nologin loadbalancer

COPY --from=build --chown=loadbalancer:loadbalancer /workspace/app.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

USER 10001:10001

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 CMD curl -fsS -o /dev/null http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["--server.address=0.0.0.0"]
