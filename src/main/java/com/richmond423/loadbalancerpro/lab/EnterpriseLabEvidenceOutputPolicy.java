package com.richmond423.loadbalancerpro.lab;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

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
        try {
            if (!Files.exists(targetRoot, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    Files.createDirectory(targetRoot);
                } catch (FileAlreadyExistsException ignored) {
                    // A concurrent creator is accepted only after strict validation below.
                }
            }
            requireDirectoryWithoutLink(targetRoot, targetRoot.toRealPath());
            Path realTargetRoot = targetRoot.toRealPath();
            Path current = targetRoot;
            for (Path segment : targetRoot.relativize(resolvedOutput)) {
                current = current.resolve(segment);
                if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                    break;
                }
                requireDirectoryWithoutLink(current, realTargetRoot);
            }
            return resolvedOutput;
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Enterprise Lab evidence output cannot be safely inspected", exception);
        }
    }

    private static void requireDirectoryWithoutLink(Path directory, Path realTargetRoot)
            throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(
                directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        Path realDirectory = directory.toRealPath();
        if (!attributes.isDirectory() || attributes.isSymbolicLink()
                || !realDirectory.startsWith(realTargetRoot)) {
            throw new IllegalArgumentException(
                    "Enterprise Lab evidence output cannot traverse symbolic links or non-directories");
        }
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
