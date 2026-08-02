# Build and runtime images match the digest-pinned project baseline.
FROM maven:3-eclipse-temurin-26@sha256:1fc9415e0626a5893bbc352149d25a413e334a7ac5cd514bd99a2828fb082071 AS build
WORKDIR /source
COPY deploy/fixture/FixtureBackend.java .
RUN javac --release 17 -d /output FixtureBackend.java

FROM eclipse-temurin:17-jre-jammy@sha256:475d8e96b4b2bfe08999e5e854755c773af1581acdf959a4545d88f0696a2339
WORKDIR /app
RUN groupadd --gid 10001 fixture \
    && useradd --uid 10001 --gid fixture --no-create-home --shell /usr/sbin/nologin fixture
COPY --from=build --chown=fixture:fixture /output/FixtureBackend.class /app/FixtureBackend.class
USER fixture:fixture
EXPOSE 8080
ENTRYPOINT ["java", "--add-modules", "jdk.httpserver", "-cp", "/app", "FixtureBackend"]
