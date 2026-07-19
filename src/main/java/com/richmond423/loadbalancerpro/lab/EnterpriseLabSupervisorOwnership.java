package com.richmond423.loadbalancerpro.lab;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;

/** OS-backed single-owner capability for one local supervisor data directory. */
public final class EnterpriseLabSupervisorOwnership implements AutoCloseable {
    static final String DIRECTORY_NAME = "enterprise-lab-supervisor-v1";
    static final String LOCK_FILE_NAME = "supervisor.lock";

    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");

    private final Path trustedRoot;
    private final Path supervisorDirectory;
    private final Path lockFile;
    private final FileChannel channel;
    private final FileLock lock;
    private boolean closed;

    private EnterpriseLabSupervisorOwnership(
            Path trustedRoot,
            Path supervisorDirectory,
            Path lockFile,
            FileChannel channel,
            FileLock lock) {
        this.trustedRoot = trustedRoot;
        this.supervisorDirectory = supervisorDirectory;
        this.lockFile = lockFile;
        this.channel = channel;
        this.lock = lock;
    }

    public static EnterpriseLabSupervisorOwnership acquire(Path trustedRoot) {
        Path root = validateTrustedRoot(trustedRoot);
        Path directory = controlledDirectory(root, DIRECTORY_NAME);
        Path lockPath = controlledPath(directory, LOCK_FILE_NAME);
        FileChannel channel = null;
        try {
            channel = openLockChannel(lockPath);
            FileLock lock;
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException exception) {
                lock = null;
            }
            if (lock == null) {
                channel.close();
                throw new OwnershipException(
                        Failure.LIVE_COMPETING_SUPERVISOR,
                        "another supervisor owns the controlled local lock");
            }
            return new EnterpriseLabSupervisorOwnership(
                    root, directory, lockPath, channel, lock);
        } catch (OwnershipException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException ignored) {
                    // The original bounded acquisition failure remains authoritative.
                }
            }
            throw new OwnershipException(
                    Failure.LOCK_UNAVAILABLE,
                    "supervisor ownership lock could not be acquired", exception);
        }
    }

    public boolean held() {
        return !closed && channel.isOpen() && lock.isValid();
    }

    void requireHeld() {
        if (!held()) {
            throw new OwnershipException(
                    Failure.LOCK_LOST,
                    "supervisor ownership lock is no longer held");
        }
        validateControlledDirectory(supervisorDirectory, trustedRoot);
        validateControlledFile(lockFile, supervisorDirectory);
    }

    Path supervisorDirectory() {
        requireHeld();
        return supervisorDirectory;
    }

    Path trustedRoot() {
        return trustedRoot;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            lock.release();
        } catch (IOException ignored) {
            // Closing the channel still asks the operating system to release the lock.
        }
        try {
            channel.close();
        } catch (IOException ignored) {
            // The capability is failed closed in this process regardless.
        }
    }

    private static Path validateTrustedRoot(Path value) {
        Path root = Objects.requireNonNull(value, "trustedRoot cannot be null")
                .toAbsolutePath().normalize();
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    root, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isDirectory() || attributes.isSymbolicLink()) {
                throw new OwnershipException(
                        Failure.UNSAFE_PATH,
                        "supervisor trusted root must be a real local directory");
            }
            return root;
        } catch (OwnershipException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new OwnershipException(
                    Failure.UNSAFE_PATH,
                    "supervisor trusted root is unavailable", exception);
        }
    }

    private static Path controlledDirectory(Path parent, String name) {
        Path directory = controlledPath(parent, name);
        try {
            try {
                Files.createDirectory(directory, directoryAttribute());
            } catch (FileAlreadyExistsException ignored) {
                // Validate the exact existing path below.
            } catch (UnsupportedOperationException exception) {
                try {
                    Files.createDirectory(directory);
                } catch (FileAlreadyExistsException ignored) {
                    // Validate the exact existing path below.
                }
            }
            validateControlledDirectory(directory, parent);
            restrictPermissions(directory, DIRECTORY_PERMISSIONS);
            return directory;
        } catch (OwnershipException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new OwnershipException(
                    Failure.STORAGE_UNAVAILABLE,
                    "supervisor directory could not be prepared", exception);
        }
    }

    static Path controlledPath(Path parent, String name) {
        Path safeParent = parent.toAbsolutePath().normalize();
        Path path = safeParent.resolve(name).toAbsolutePath().normalize();
        if (!path.getParent().equals(safeParent) || !path.startsWith(safeParent)) {
            throw new OwnershipException(
                    Failure.UNSAFE_PATH,
                    "supervisor path escaped the controlled directory");
        }
        return path;
    }

    private static FileChannel openLockChannel(Path lockFile) throws IOException {
        if (!Files.exists(lockFile, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.createFile(lockFile, fileAttribute());
            } catch (FileAlreadyExistsException ignored) {
                // A competing creator is accepted only after exact validation below.
            } catch (UnsupportedOperationException exception) {
                try {
                    Files.createFile(lockFile);
                } catch (FileAlreadyExistsException ignored) {
                    // A competing creator is accepted only after exact validation below.
                }
            }
        }
        validateControlledFile(lockFile, lockFile.getParent());
        restrictPermissions(lockFile, FILE_PERMISSIONS);
        return FileChannel.open(
                lockFile,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS);
    }

    static void validateControlledDirectory(Path directory, Path parent) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isDirectory() || attributes.isSymbolicLink()
                    || !directory.toAbsolutePath().normalize().getParent()
                    .equals(parent.toAbsolutePath().normalize())) {
                throw new OwnershipException(
                        Failure.UNSAFE_PATH,
                        "supervisor directory identity is unsafe");
            }
        } catch (OwnershipException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new OwnershipException(
                    Failure.STORAGE_UNAVAILABLE,
                    "supervisor directory could not be validated", exception);
        }
    }

    static void validateControlledFile(Path file, Path parent) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile() || attributes.isSymbolicLink()
                    || !file.toAbsolutePath().normalize().getParent()
                    .equals(parent.toAbsolutePath().normalize())) {
                throw new OwnershipException(
                        Failure.UNSAFE_PATH,
                        "supervisor controlled file identity is unsafe");
            }
        } catch (OwnershipException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new OwnershipException(
                    Failure.STORAGE_UNAVAILABLE,
                    "supervisor controlled file could not be validated", exception);
        }
    }

    static void restrictFilePermissions(Path path) {
        restrictPermissions(path, FILE_PERMISSIONS);
    }

    private static void restrictPermissions(
            Path path, Set<PosixFilePermission> permissions) {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException ignored) {
            // Windows ACL inheritance remains the platform boundary.
        } catch (IOException exception) {
            throw new OwnershipException(
                    Failure.PERMISSION_DENIED,
                    "supervisor path permissions could not be restricted", exception);
        }
    }

    private static FileAttribute<Set<PosixFilePermission>> directoryAttribute() {
        return PosixFilePermissions.asFileAttribute(DIRECTORY_PERMISSIONS);
    }

    private static FileAttribute<Set<PosixFilePermission>> fileAttribute() {
        return PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS);
    }

    public enum Failure {
        UNSAFE_PATH,
        STORAGE_UNAVAILABLE,
        PERMISSION_DENIED,
        LOCK_UNAVAILABLE,
        LIVE_COMPETING_SUPERVISOR,
        LOCK_LOST
    }

    public static final class OwnershipException extends IllegalStateException {
        private final Failure failure;

        private OwnershipException(Failure failure, String message) {
            super(message);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        private OwnershipException(Failure failure, String message, Throwable cause) {
            super(message, cause);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        public Failure failure() {
            return failure;
        }
    }
}
