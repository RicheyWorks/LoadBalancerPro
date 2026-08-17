# Build and runtime images match the digest-pinned project baseline.
FROM maven:3-eclipse-temurin-26@sha256:1fc9415e0626a5893bbc352149d25a413e334a7ac5cd514bd99a2828fb082071 AS build
WORKDIR /source
COPY deploy/topology/TopologyIngress.java .
RUN javac --release 17 --add-modules jdk.httpserver -d /output TopologyIngress.java

FROM eclipse-temurin:17-jre-jammy@sha256:475d8e96b4b2bfe08999e5e854755c773af1581acdf959a4545d88f0696a2339
WORKDIR /app
RUN groupadd --gid 10002 topology \
    && useradd --uid 10002 --gid topology --no-create-home --shell /usr/sbin/nologin topology
COPY --from=build --chown=topology:topology /output/TopologyIngress*.class /app/
USER topology:topology
EXPOSE 8443
ENTRYPOINT ["java", "--add-modules", "jdk.httpserver", "-cp", "/app", "TopologyIngress"]
