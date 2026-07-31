package com.richmond423.loadbalancerpro.lab;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Objects;

/**
 * Shared parent-directory synchronization mechanics for Enterprise Lab local
 * storage. Unsupported providers are reported explicitly instead of being
 * represented as completed directory durability.
 */
final class EnterpriseLabStorageDurability {
    static final DirectorySyncer SYSTEM_DIRECTORY_SYNCER =
            EnterpriseLabStorageDurability::synchronizeWithSystemProvider;

    private EnterpriseLabStorageDurability() {
    }

    static EnterpriseLabDirectorySyncStatus synchronizeDirectory(
            Path directory,
            DirectorySyncer syncer) throws IOException {
        Path safeDirectory = Objects.requireNonNull(
                directory, "directory cannot be null")
                .toAbsolutePath()
                .normalize();
        return Objects.requireNonNull(
                syncer, "directory syncer cannot be null")
                .synchronize(safeDirectory);
    }

    static EnterpriseLabDirectorySyncStatus synchronizeMove(
            Path source,
            Path destination,
            DirectorySyncer syncer) throws IOException {
        Path sourceParent = controlledParent(source);
        Path destinationParent = controlledParent(destination);
        EnterpriseLabDirectorySyncStatus result =
                synchronizeDirectory(destinationParent, syncer);
        if (!sourceParent.equals(destinationParent)) {
            result = EnterpriseLabDirectorySyncStatus.combine(
                    result,
                    synchronizeDirectory(sourceParent, syncer));
        }
        return result;
    }

    private static EnterpriseLabDirectorySyncStatus synchronizeWithSystemProvider(
            Path directory) throws IOException {
        if (Files.getFileAttributeView(
                directory,
                PosixFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS) == null) {
            return EnterpriseLabDirectorySyncStatus
                    .UNSUPPORTED_ON_LOCAL_FILESYSTEM;
        }
        try (FileChannel channel = FileChannel.open(
                directory,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            channel.force(true);
            return EnterpriseLabDirectorySyncStatus.SYNCHRONIZED;
        } catch (UnsupportedOperationException exception) {
            throw new IOException(
                    "directory metadata synchronization is unavailable",
                    exception);
        }
    }

    private static Path controlledParent(Path value) {
        Path safe = Objects.requireNonNull(value, "path cannot be null")
                .toAbsolutePath()
                .normalize();
        Path parent = safe.getParent();
        if (parent == null) {
            throw new IllegalArgumentException(
                    "durable storage path must have a parent");
        }
        return parent;
    }

    @FunctionalInterface
    interface DirectorySyncer {
        EnterpriseLabDirectorySyncStatus synchronize(Path directory)
                throws IOException;
    }
}
