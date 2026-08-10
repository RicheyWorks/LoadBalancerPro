package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class EngineeringTrustInterfaceTest {

    private static final Path REPOSITORY = Path.of("").toAbsolutePath().normalize();
    private static final List<Path> ENTRY_DOCS = List.of(
            Path.of("README.md"),
            Path.of("docs/REVIEWER_TRUST_MAP.md"));
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[[^]\\r\\n]+]\\(([^)\\r\\n]+)\\)");
    private static final Pattern UNSAFE_COMMAND = Pattern.compile(
            "(?im)^\\s*(?:\\$\\s*)?(?:gh\\s+release\\s+create|docker\\s+push|git\\s+push\\s+--force|"
                    + "terraform\\s+apply|kubectl\\s+apply|aws\\s+\\S+\\s+(?:create|delete|terminate|run|update|"
                    + "put|attach|detach))\\b");
    private static final Pattern AFFIRMATIVE_OVERCLAIM = Pattern.compile(
            "(?i)\\b(?:is|are|proves?|provides)\\s+(?:fully\\s+)?(?:production[- ]ready|production[- ]certified|"
                    + "production certification|live-cloud validated|real-tenant validated)\\b");

    @Test
    void entryDocumentsContainNoUnsafeMutationOrReleaseCommands() throws IOException {
        for (Path document : ENTRY_DOCS) {
            String content = Files.readString(document);
            assertFalse(UNSAFE_COMMAND.matcher(content).find(), () -> "Unsafe command in " + document);
        }
    }

    @Test
    void entryDocumentsContainNoObviousAffirmativeCertificationClaims() throws IOException {
        for (Path document : ENTRY_DOCS) {
            String content = Files.readString(document);
            assertFalse(AFFIRMATIVE_OVERCLAIM.matcher(content).find(), () -> "Unsupported claim in " + document);
        }
    }

    @Test
    void localLinksFromEntryDocumentsResolveInsideTheRepository() throws IOException {
        for (Path document : ENTRY_DOCS) {
            String content = Files.readString(document);
            Matcher links = MARKDOWN_LINK.matcher(content);
            while (links.find()) {
                String target = links.group(1).trim();
                if (isExternalOrPageTarget(target)) {
                    continue;
                }
                String pathPart = target.split("#", 2)[0].replace("%20", " ");
                Path resolved = document.toAbsolutePath().getParent().resolve(pathPart).normalize();
                assertTrue(resolved.startsWith(REPOSITORY), () -> "Link escapes repository: " + target);
                assertTrue(Files.exists(resolved), () -> "Broken link in " + document + ": " + target);
            }
        }
    }

    private static boolean isExternalOrPageTarget(String target) {
        String normalized = target.toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("mailto:")
                || normalized.startsWith("#")
                || normalized.startsWith("/");
    }
}
