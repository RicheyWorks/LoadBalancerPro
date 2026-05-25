package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class AgentEvidenceAuditMavenDependencyPostureAuditDocumentationTest {
    private static final Path AUDIT = Path.of(
            "docs/agent/EVIDENCE_AUDIT_MAVEN_DEPENDENCY_POSTURE_AUDIT.md");
    private static final Path POM = Path.of("pom.xml");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path EVIDENCE_MAP = Path.of("docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md");
    private static final Path BOARD = Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentEvidenceAuditMavenDependencyPostureAuditDocumentationTest.java");

    @Test
    void auditExistsAndNamesSlotSixScope() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "slot 6",
                "maven dependency posture",
                "documentation/test-only",
                "pom.xml",
                "codex/evidence-audit-maven-dependency-posture",
                "a58d61511d84b8d9013d5a2652dc696fb555e83c",
                "without changing maven configuration",
                "not a dependency upgrade",
                "not a maven behavior change")) {
            assertTrue(audit.contains(expected), "Missing slot 6 audit scope: " + expected);
        }
    }

    @Test
    void auditCoversRequiredMavenPosture() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "java release property",
                "java.version",
                "17",
                "spring boot",
                "3.5.14",
                "tomcat",
                "10.1.55",
                "netty-bom",
                "4.2.13.final",
                "aws sdk v2",
                "2.44.4",
                "javafx",
                "17.0.19",
                "log4j",
                "org.json",
                "gson",
                "test dependency",
                "jacoco",
                "spring boot main class",
                "com.richmond423.loadbalancerpro.api.loadbalancerapiapplication",
                "reviewer questions",
                "remaining limits")) {
            assertTrue(audit.contains(expected), "Missing Maven posture wording: " + expected);
        }
    }

    @Test
    void pomStillDeclaresAuditedPropertiesDependenciesAndPlugins()
            throws IOException, ParserConfigurationException, SAXException {
        Document pom = parsePom();

        assertEquals("17", property(pom, "java.version"));
        assertEquals("3.5.14", property(pom, "spring-boot.version"));
        assertEquals("10.1.55", property(pom, "tomcat.version"));
        assertEquals("4.2.13.Final", property(pom, "netty.version"));
        assertEquals("2.44.4", property(pom, "aws-sdk-v2.version"));
        assertEquals("17.0.19", property(pom, "javafx.version"));
        assertEquals("0.8.13", property(pom, "jacoco.version"));

        for (String dependency : List.of(
                "org.springframework.boot:spring-boot-starter-web:",
                "org.springframework.boot:spring-boot-starter-actuator:",
                "org.springframework.boot:spring-boot-starter-security:",
                "org.springframework.boot:spring-boot-starter-oauth2-resource-server:",
                "org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17",
                "io.micrometer:micrometer-registry-prometheus:",
                "io.micrometer:micrometer-registry-otlp:",
                "org.openjfx:javafx-controls:${javafx.version}",
                "org.apache.logging.log4j:log4j-api:",
                "org.apache.logging.log4j:log4j-core:",
                "org.json:json:20251224",
                "com.google.code.gson:gson:2.14.0",
                "com.github.ben-manes.caffeine:caffeine:3.2.4",
                "software.amazon.awssdk:autoscaling:",
                "software.amazon.awssdk:cloudwatch:",
                "software.amazon.awssdk:ec2:",
                "org.springframework.boot:spring-boot-starter-test:",
                "org.springframework.security:spring-security-test:")) {
            assertTrue(dependencies(pom).contains(dependency), "Missing dependency declaration: " + dependency);
        }

        for (String managed : List.of(
                "io.netty:netty-bom:${netty.version}",
                "org.springframework.boot:spring-boot-dependencies:${spring-boot.version}",
                "org.apache.tomcat.embed:tomcat-embed-core:${tomcat.version}",
                "org.apache.tomcat.embed:tomcat-embed-el:${tomcat.version}",
                "org.apache.tomcat.embed:tomcat-embed-websocket:${tomcat.version}",
                "software.amazon.awssdk:bom:${aws-sdk-v2.version}")) {
            assertTrue(dependencyManagement(pom).contains(managed),
                    "Missing dependency management declaration: " + managed);
        }

        for (String plugin : List.of(
                "org.apache.maven.plugins:maven-compiler-plugin:3.15.0",
                "org.apache.maven.plugins:maven-surefire-plugin:3.5.5",
                "org.codehaus.mojo:exec-maven-plugin:3.5.0",
                "org.jacoco:jacoco-maven-plugin:${jacoco.version}",
                "org.apache.maven.plugins:maven-jar-plugin:3.5.0",
                "org.springframework.boot:spring-boot-maven-plugin:${spring-boot.version}")) {
            assertTrue(plugins(pom).contains(plugin), "Missing Maven plugin declaration: " + plugin);
        }

        String pomText = read(POM).replace("\r\n", "\n").toLowerCase(Locale.ROOT);
        assertTrue(pomText.contains("<release>${java.version}</release>"),
                "compiler plugin should use java.version release");
        assertTrue(pomText.contains("<mainclass>com.richmond423.loadbalancerpro.api.loadbalancerapiapplication</mainclass>"),
                "Spring Boot plugin should preserve the application main class");
    }

    @Test
    void navigationAndCampaignStateReferenceMavenAudit() throws IOException {
        String readme = read(README).toLowerCase(Locale.ROOT);
        String trustMap = read(TRUST_MAP).toLowerCase(Locale.ROOT);
        String evidenceMap = read(EVIDENCE_MAP).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION).toLowerCase(Locale.ROOT);

        assertTrue(readme.contains("docs/agent/evidence_audit_maven_dependency_posture_audit.md"),
                "README should link to the Maven dependency posture audit");
        assertTrue(trustMap.contains("agent/evidence_audit_maven_dependency_posture_audit.md"),
                "Reviewer Trust Map should link to the Maven dependency posture audit");
        assertTrue(evidenceMap.contains("evidence_audit_maven_dependency_posture_audit.md"),
                "repository evidence map should link to the Maven dependency posture audit");

        for (String expected : List.of(
                "completed campaign prs: 5 / 20",
                "current pr slot: 6",
                "codex/evidence-audit-maven-dependency-posture",
                "pr #320 merged",
                "a58d61511d84b8d9013d5a2652dc696fb555e83c",
                "post-merge main ci and codeql were green",
                "slot 6 branch created")) {
            assertTrue(board.contains(expected) || session.contains(expected),
                    "Missing slot 6 campaign checkpoint: " + expected);
        }
    }

    @Test
    void auditPreservesNotProvenBoundaries() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not prove production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "registry publication",
                "container signing",
                "production telemetry",
                "production monitoring",
                "full vulnerability management",
                "incident response readiness",
                "remediation sla compliance",
                "broader automation")) {
            assertTrue(audit.contains(expected), "Missing Maven audit boundary: " + expected);
        }
    }

    @Test
    void guardTestOnlyReadsTrackedFiles() throws IOException {
        String source = read(SOURCE);

        for (String forbidden : List.of(
                "Files." + "write",
                "Files." + "create",
                "Files." + "delete",
                "Process" + "Builder",
                "Runtime." + "getRuntime",
                ".ex" + "ec(",
                "Http" + "Client",
                "URL" + "Connection",
                "Socket" + "(")) {
            assertFalse(source.contains(forbidden), "guard test must not use " + forbidden);
        }
    }

    private static Document parsePom() throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (InputStream input = Files.newInputStream(POM)) {
            return factory.newDocumentBuilder().parse(input);
        }
    }

    private static String property(Document document, String name) {
        return text(document.getElementsByTagName(name).item(0));
    }

    private static List<String> dependencyManagement(Document document) {
        Element management = directChild(document.getDocumentElement(), "dependencyManagement");
        Element dependencies = directChild(management, "dependencies");
        return coordinates(dependencies.getElementsByTagName("dependency"));
    }

    private static List<String> dependencies(Document document) {
        Element section = directChild(document.getDocumentElement(), "dependencies");
        return coordinates(section.getElementsByTagName("dependency"));
    }

    private static List<String> plugins(Document document) {
        Element build = directChild(document.getDocumentElement(), "build");
        Element section = directChild(build, "plugins");
        return coordinates(section.getElementsByTagName("plugin"));
    }

    private static List<String> coordinates(NodeList nodes) {
        return java.util.stream.IntStream.range(0, nodes.getLength())
                .mapToObj(nodes::item)
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .map(element -> childText(element, "groupId")
                        + ":" + childText(element, "artifactId")
                        + ":" + childText(element, "version"))
                .toList();
    }

    private static Element directChild(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element element && tagName.equals(element.getTagName())) {
                return element;
            }
        }
        throw new AssertionError(tagName + " should exist under " + parent.getTagName());
    }

    private static String childText(Element element, String tagName) {
        Node node = element.getElementsByTagName(tagName).item(0);
        return node == null ? "" : text(node);
    }

    private static String text(Node node) {
        return node == null ? "" : node.getTextContent().trim();
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
