package com.richmond423.loadbalancerpro.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

final class EvidencePolicyExampleService {
    static final String BEFORE_FILE = "before.json";
    static final String AFTER_FILE = "after.json";
    static final String EXPECTED_DECISION_FILE = "expected-decision.json";

    private static final String RESOURCE_ROOT = "evidence-policies/examples/";
    private static final List<PolicyExample> EXAMPLES = List.of(
            new PolicyExample(
                    "strict-zero-drift-pass",
                    "strict-zero-drift",
                    "PASS",
                    "Identical sender and receiver catalogs for a final zero-drift handoff.",
                    RESOURCE_ROOT + "strict-zero-drift-pass/"),
            new PolicyExample(
                    "strict-zero-drift-fail",
                    "strict-zero-drift",
                    "FAIL",
                    "A strict handoff where report checksum drift stops the transfer.",
                    RESOURCE_ROOT + "strict-zero-drift-fail/"),
            new PolicyExample(
                    "receiver-redaction-warn",
                    "receiver-redaction",
                    "WARN",
                    "Receiver-side redaction summary plus redacted evidence changes that need review.",
                    RESOURCE_ROOT + "receiver-redaction-warn/"),
            new PolicyExample(
                    "audit-append-warn",
                    "audit-append",
                    "WARN",
                    "Receiver audit log anchor advancement after local verification.",
                    RESOURCE_ROOT + "audit-append-warn/"),
            new PolicyExample(
                    "regulated-handoff-pass",
                    "regulated-handoff",
                    "PASS",
                    "Strict packaged review profile with no drift.",
                    RESOURCE_ROOT + "regulated-handoff-pass/"),
            new PolicyExample(
                    "regulated-handoff-fail",
                    "regulated-handoff",
                    "FAIL",
                    "Missing core bundle evidence under the regulated handoff profile.",
                    RESOURCE_ROOT + "regulated-handoff-fail/"),
            new PolicyExample(
                    "investigation-working-copy-warn",
                    "investigation-working-copy",
                    "WARN",
                    "Active investigation handoff with working notes and reviewed report edits.",
                    RESOURCE_ROOT + "investigation-working-copy-warn/"));

    List<PolicyExample> listExamples() {
        return EXAMPLES.stream()
                .sorted(Comparator.comparing(PolicyExample::name))
                .toList();
    }

    Optional<PolicyExample> findExample(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(name);
        return EXAMPLES.stream()
                .filter(example -> normalize(example.name()).equals(normalized))
                .findFirst();
    }

    String renderExampleList() {
        StringBuilder builder = new StringBuilder("Available evidence policy examples:")
                .append(System.lineSeparator());
        for (PolicyExample example : listExamples()) {
            builder.append("- ")
                    .append(example.name())
                    .append(": template=")
                    .append(example.templateName())
                    .append(", expectedDecision=")
                    .append(example.expectedDecision())
                    .append(" - ")
                    .append(example.description())
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    String renderExampleSummary(String name) {
        PolicyExample example = requireExample(name);
        return new StringBuilder("Evidence policy example: ")
                .append(example.name())
                .append(System.lineSeparator())
                .append("- template: ")
                .append(example.templateName())
                .append(System.lineSeparator())
                .append("- expectedDecision: ")
                .append(example.expectedDecision())
                .append(System.lineSeparator())
                .append("- before: ")
                .append(BEFORE_FILE)
                .append(System.lineSeparator())
                .append("- after: ")
                .append(AFTER_FILE)
                .append(System.lineSeparator())
                .append("- expectedDecisionMetadata: ")
                .append(EXPECTED_DECISION_FILE)
                .append(System.lineSeparator())
                .append("- description: ")
                .append(example.description())
                .append(System.lineSeparator())
                .toString();
    }

    ExportedPolicyExample exportExample(String name, Path outputDirectory, boolean force) throws IOException {
        Objects.requireNonNull(outputDirectory, "example output directory cannot be null");
        PolicyExample example = requireExample(name);
        Path normalizedDirectory = outputDirectory.toAbsolutePath().normalize();
        Files.createDirectories(normalizedDirectory);
        if (!force) {
            for (String fileName : List.of(BEFORE_FILE, AFTER_FILE, EXPECTED_DECISION_FILE)) {
                Path target = normalizedDirectory.resolve(fileName).normalize();
                if (!target.getParent().equals(normalizedDirectory)) {
                    throw new IllegalArgumentException("example file cannot escape output directory: " + fileName);
                }
                if (Files.exists(target)) {
                    throw new IllegalArgumentException("target file already exists: " + target);
                }
            }
        }

        Path before = copyExampleFile(example, BEFORE_FILE, normalizedDirectory, force);
        Path after = copyExampleFile(example, AFTER_FILE, normalizedDirectory, force);
        Path expected = copyExampleFile(example, EXPECTED_DECISION_FILE, normalizedDirectory, force);
        return new ExportedPolicyExample(example, normalizedDirectory, before, after, expected);
    }

    String exampleFileText(String name, String fileName) throws IOException {
        PolicyExample example = requireExample(name);
        try (InputStream input = resourceStream(example.resourceDirectory() + fileName)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Path copyExampleFile(
            PolicyExample example,
            String fileName,
            Path outputDirectory,
            boolean force) throws IOException {
        Path target = outputDirectory.resolve(fileName).normalize();
        if (!target.getParent().equals(outputDirectory)) {
            throw new IllegalArgumentException("example file cannot escape output directory: " + fileName);
        }
        if (Files.exists(target) && !force) {
            throw new IllegalArgumentException("target file already exists: " + target);
        }
        try (InputStream input = resourceStream(example.resourceDirectory() + fileName)) {
            Files.write(target, input.readAllBytes());
        }
        return target;
    }

    private PolicyExample requireExample(String name) {
        return findExample(name)
                .orElseThrow(() -> new IllegalArgumentException("unknown evidence policy example: " + name));
    }

    private static InputStream resourceStream(String resourcePath) throws IOException {
        InputStream input = EvidencePolicyExampleService.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IOException("missing evidence policy example resource: " + resourcePath);
        }
        return input;
    }

    private static String normalize(String value) {
        return Objects.requireNonNull(value, "example name cannot be null")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    record PolicyExample(
            String name,
            String templateName,
            String expectedDecision,
            String description,
            String resourceDirectory) {

        PolicyExample {
            Objects.requireNonNull(name, "example name cannot be null");
            Objects.requireNonNull(templateName, "template name cannot be null");
            Objects.requireNonNull(expectedDecision, "expected decision cannot be null");
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(resourceDirectory, "example resource directory cannot be null");
        }
    }

    record ExportedPolicyExample(
            PolicyExample example,
            Path directory,
            Path beforePath,
            Path afterPath,
            Path expectedDecisionPath) {
    }
}
