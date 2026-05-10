package com.richmond423.loadbalancerpro.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

final class EvidencePolicyTemplateService {
    private static final String RESOURCE_ROOT = "evidence-policies/templates/";
    private static final List<PolicyTemplate> TEMPLATES = List.of(
            new PolicyTemplate(
                    "strict-zero-drift",
                    "Final sender/receiver equality check; any unclassified drift fails.",
                    RESOURCE_ROOT + "strict-zero-drift.json"),
            new PolicyTemplate(
                    "receiver-redaction",
                    "Receiver-side redaction handoff; redaction summaries are expected and redacted outputs warn.",
                    RESOURCE_ROOT + "receiver-redaction.json"),
            new PolicyTemplate(
                    "audit-append",
                    "Receiver verification flow where audit log anchors may advance after handoff.",
                    RESOURCE_ROOT + "audit-append.json"),
            new PolicyTemplate(
                    "regulated-handoff",
                    "Strict regulated evidence handoff profile with only documented summaries allowed.",
                    RESOURCE_ROOT + "regulated-handoff.json"),
            new PolicyTemplate(
                    "investigation-working-copy",
                    "Active investigation profile that permits working notes while preserving core evidence failures.",
                    RESOURCE_ROOT + "investigation-working-copy.json"));

    List<PolicyTemplate> listTemplates() {
        return TEMPLATES.stream()
                .sorted(Comparator.comparing(PolicyTemplate::name))
                .toList();
    }

    Optional<PolicyTemplate> findTemplate(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(name);
        return TEMPLATES.stream()
                .filter(template -> normalize(template.name()).equals(normalized))
                .findFirst();
    }

    String templateJson(String name) throws IOException {
        PolicyTemplate template = findTemplate(name)
                .orElseThrow(() -> new IllegalArgumentException("unknown policy template: " + name));
        try (InputStream input = EvidencePolicyTemplateService.class.getClassLoader()
                .getResourceAsStream(template.resourcePath())) {
            if (input == null) {
                throw new IOException("missing policy template resource: " + template.resourcePath());
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    EvidenceHandoffPolicyService.HandoffPolicy validatePolicy(Path policyPath) throws IOException {
        return new EvidenceHandoffPolicyService().readPolicy(policyPath);
    }

    String renderTemplateList() {
        StringBuilder builder = new StringBuilder("Available evidence handoff policy templates:")
                .append(System.lineSeparator());
        for (PolicyTemplate template : listTemplates()) {
            builder.append("- ")
                    .append(template.name())
                    .append(": ")
                    .append(template.description())
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private static String normalize(String value) {
        return Objects.requireNonNull(value, "template name cannot be null")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    record PolicyTemplate(
            String name,
            String description,
            String resourcePath) {

        PolicyTemplate {
            Objects.requireNonNull(name, "template name cannot be null");
            Objects.requireNonNull(description, "template description cannot be null");
            Objects.requireNonNull(resourcePath, "template resource path cannot be null");
        }
    }
}
