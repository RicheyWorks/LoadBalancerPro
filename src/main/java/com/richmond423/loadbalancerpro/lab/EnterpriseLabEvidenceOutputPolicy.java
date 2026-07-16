package com.richmond423.loadbalancerpro.lab;

import java.nio.file.Path;

final class EnterpriseLabEvidenceOutputPolicy {
    private EnterpriseLabEvidenceOutputPolicy() {
    }

    static Path requireTargetPath(Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Enterprise Lab evidence output cannot be null");
        }
        Path targetRoot = Path.of("target").toAbsolutePath().normalize();
        Path resolvedOutput = outputDirectory.toAbsolutePath().normalize();
        if (!resolvedOutput.startsWith(targetRoot)) {
            throw new IllegalArgumentException("Enterprise Lab evidence output must stay under target/: "
                    + outputDirectory);
        }
        return resolvedOutput;
    }

    static void assertNoSecretLikeText(String text) {
        String normalized = text.toLowerCase();
        if (normalized.contains("bearer ")
                || normalized.contains("x-api-key")
                || normalized.contains("change_me_local_api_key")
                || normalized.matches("(?s).*(password|secret|credential|token)\\s*[:=]\\s*[a-z0-9._~+/-]{8,}.*")) {
            throw new IllegalArgumentException("Refusing to write Enterprise Lab evidence that looks secret-bearing");
        }
    }
}
