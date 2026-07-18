package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabEvidenceOwnershipPathsTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preparesOnlyFixedControlledOwnershipPathsAndSafeSummary() {
        EnterpriseLabEvidenceOwnershipPaths paths = create();
        var summary = paths.summary();

        assertEquals("enterprise-lab-experiment-journals-v1", paths.namespace().getFileName().toString());
        assertEquals("ownership-v1", paths.ownershipDirectory().getFileName().toString());
        assertEquals("history", paths.historyDirectory().getFileName().toString());
        assertEquals("directory-identity-v1", paths.directoryIdentityFile().getFileName().toString());
        assertEquals("owner.lock", paths.lockFile().getFileName().toString());
        assertEquals("owner-record-v1.json", paths.recordFile().getFileName().toString());
        assertEquals("owner-record-v1.tmp", paths.temporaryRecordFile().getFileName().toString());
        assertEquals(paths.directoryIdentity(), summary.directoryIdentity());
        assertEquals(paths.logicalLockIdentity(), summary.logicalLockIdentity());
        assertTrue(summary.directoryIdentity().matches("[0-9a-f]{64}"));
        assertTrue(summary.logicalLockIdentity().matches("[0-9a-f]{64}"));
        assertTrue(Files.isRegularFile(paths.directoryIdentityFile(), LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.exists(paths.lockFile(), LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.exists(paths.recordFile(), LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    void identityIsStableForTheSameControlledDirectory() {
        EnterpriseLabEvidenceOwnershipPaths first = create();
        EnterpriseLabEvidenceOwnershipPaths second = create();

        assertEquals(first.directoryIdentity(), second.directoryIdentity());
        assertEquals(first.logicalLockIdentity(), second.logicalLockIdentity());
        first.verifyDirectoryIdentity();
        second.verifyDirectoryIdentity();
    }

    @Test
    void simultaneousInitializationConvergesOnOneForcedDirectoryMarker() throws Exception {
        int contenders = 8;
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(contenders);
        List<java.util.concurrent.Future<EnterpriseLabEvidenceOwnershipPaths>> futures =
                new ArrayList<>();
        try {
            for (int index = 0; index < contenders; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return create();
                }));
            }
            start.countDown();

            Set<String> identities = new java.util.HashSet<>();
            for (var future : futures) {
                EnterpriseLabEvidenceOwnershipPaths paths = future.get(5, TimeUnit.SECONDS);
                paths.verifyDirectoryIdentity();
                identities.add(paths.directoryIdentity());
            }
            assertEquals(1, identities.size());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void directoryReplacementIsDetectedBeforeOwnershipUse() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = create();
        String before = paths.directoryIdentity();

        Files.delete(paths.historyDirectory());
        Files.delete(paths.directoryIdentityFile());
        Files.delete(paths.ownershipDirectory());
        Files.createDirectory(paths.ownershipDirectory());
        Files.createDirectory(paths.historyDirectory());

        EnterpriseLabEvidenceOwnershipException exception = assertThrows(
                EnterpriseLabEvidenceOwnershipException.class, paths::verifyDirectoryIdentity);
        assertEquals(FailureClassification.DIRECTORY_IDENTITY_MISMATCH,
                exception.classification());
        assertNotEquals(before, EnterpriseLabEvidenceOwnershipPaths.create(
                temporaryDirectory.toAbsolutePath().normalize()).directoryIdentity());
    }

    @Test
    void relativeFilesystemRootAndRegularFileRootsAreRejected() throws Exception {
        assertPathFailure(FailureClassification.UNSAFE_PATH,
                () -> EnterpriseLabEvidenceOwnershipPaths.create(Path.of("target", "ownership")));
        assertPathFailure(FailureClassification.UNSAFE_PATH,
                () -> EnterpriseLabEvidenceOwnershipPaths.create(
                        temporaryDirectory.toAbsolutePath().getRoot()));

        Path file = Files.createFile(temporaryDirectory.resolve("not-a-directory"));
        assertPathFailure(FailureClassification.UNSAFE_PATH,
                () -> EnterpriseLabEvidenceOwnershipPaths.create(file.toAbsolutePath().normalize()));
    }

    @Test
    void symbolicLinkRootIsRejectedWhereSupported() throws Exception {
        Path actual = Files.createDirectory(temporaryDirectory.resolve("actual"));
        Path link = temporaryDirectory.resolve("root-link");
        if (!createSymbolicLink(link, actual)) {
            assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
            return;
        }

        assertPathFailure(FailureClassification.UNSAFE_PATH,
                () -> EnterpriseLabEvidenceOwnershipPaths.create(link.toAbsolutePath().normalize()));
    }

    @Test
    void symbolicLinkOwnershipNamespaceIsRejectedWhereSupported() throws Exception {
        Path namespace = Files.createDirectory(temporaryDirectory.resolve(
                EnterpriseLabExperimentJournalDirectory.NAMESPACE));
        Path outside = Files.createDirectory(temporaryDirectory.resolve("outside"));
        Path link = namespace.resolve(EnterpriseLabEvidenceOwnershipPaths.OWNERSHIP_DIRECTORY_NAME);
        if (!createSymbolicLink(link, outside)) {
            assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
            return;
        }

        assertPathFailure(FailureClassification.UNSAFE_PATH,
                () -> EnterpriseLabEvidenceOwnershipPaths.create(
                        temporaryDirectory.toAbsolutePath().normalize()));
    }

    @Test
    void fileIdentityAcceptsOnlyTheFixedControlledFileSet() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = create();
        Files.createFile(paths.lockFile());

        String identity = paths.identityOfControlledRegularFile(paths.lockFile());
        assertTrue(identity.matches("[0-9a-f]{64}"));

        Path arbitrary = Files.createFile(paths.ownershipDirectory().resolve("caller-selected.lock"));
        assertPathFailure(FailureClassification.UNSAFE_PATH,
                () -> paths.identityOfControlledRegularFile(arbitrary));
        assertPathFailure(FailureClassification.UNSAFE_PATH,
                () -> paths.identityOfControlledRegularFile(temporaryDirectory.resolve("outside.lock")));
    }

    @Test
    void ownershipNamespaceCoexistsWithExistingJournalNamespaces() {
        EnterpriseLabEvidenceOwnershipPaths paths = create();
        EnterpriseLabMutationTestAuthority.ownedDirectory(
                temporaryDirectory.toAbsolutePath().normalize());

        assertTrue(Files.isDirectory(paths.ownershipDirectory(), LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.isDirectory(paths.namespace().resolve("journals"), LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.isDirectory(paths.namespace().resolve("quarantine"), LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.isDirectory(paths.namespace().resolve("compacted"), LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    void restrictiveDirectoryPermissionsAreAppliedWherePosixIsSupported() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = create();
        if (Files.getFileAttributeView(
                paths.ownershipDirectory(), java.nio.file.attribute.PosixFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS) == null) {
            return;
        }

        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(
                paths.ownershipDirectory(), LinkOption.NOFOLLOW_LINKS);
        assertEquals(Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE), permissions);
    }

    private EnterpriseLabEvidenceOwnershipPaths create() {
        return EnterpriseLabEvidenceOwnershipPaths.create(
                temporaryDirectory.toAbsolutePath().normalize());
    }

    private static boolean createSymbolicLink(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);
            return true;
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException exception) {
            return false;
        }
    }

    private static void assertPathFailure(
            FailureClassification expected,
            org.junit.jupiter.api.function.Executable executable) {
        EnterpriseLabEvidenceOwnershipException exception = assertThrows(
                EnterpriseLabEvidenceOwnershipException.class, executable);
        assertEquals(expected, exception.classification());
    }
}
