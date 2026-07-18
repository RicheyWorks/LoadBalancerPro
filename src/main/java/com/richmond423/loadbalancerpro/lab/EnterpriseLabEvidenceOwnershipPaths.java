package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

/** Fixed, non-caller-directed ownership paths beneath the journal namespace. */
public final class EnterpriseLabEvidenceOwnershipPaths {
    public static final String PATH_SCHEMA_VERSION = "enterprise-lab-evidence-ownership-paths/v1";

    static final String OWNERSHIP_DIRECTORY_NAME = "ownership-v1";
    static final String HISTORY_DIRECTORY_NAME = "history";
    static final String DIRECTORY_IDENTITY_FILE_NAME = "directory-identity-v1";
    static final String LOCK_FILE_NAME = "owner.lock";
    static final String RECORD_FILE_NAME = "owner-record-v1.json";
    static final String TEMPORARY_RECORD_FILE_NAME = "owner-record-v1.tmp";

    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");
    private static final int DIRECTORY_IDENTITY_READ_ATTEMPTS = 8;
    private static final long DIRECTORY_IDENTITY_RETRY_NANOS = 1_000_000L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Path trustedRoot;
    private final Path namespace;
    private final Path ownershipDirectory;
    private final Path historyDirectory;
    private final Path directoryIdentityFile;
    private final Path lockFile;
    private final Path recordFile;
    private final Path temporaryRecordFile;
    private final String directoryIdentity;
    private final String directoryIdentityToken;
    private final String logicalLockIdentity;

    private EnterpriseLabEvidenceOwnershipPaths(Path trustedRoot) {
        this.trustedRoot = validateTrustedRoot(trustedRoot);
        this.namespace = controlledDirectory(
                this.trustedRoot, EnterpriseLabExperimentJournalDirectory.NAMESPACE);
        this.ownershipDirectory = controlledDirectory(namespace, OWNERSHIP_DIRECTORY_NAME);
        this.historyDirectory = controlledDirectory(ownershipDirectory, HISTORY_DIRECTORY_NAME);
        this.directoryIdentityFile = controlledFilePath(
                ownershipDirectory, DIRECTORY_IDENTITY_FILE_NAME);
        this.directoryIdentityToken = prepareDirectoryIdentityFile(directoryIdentityFile);
        this.lockFile = controlledFilePath(ownershipDirectory, LOCK_FILE_NAME);
        this.recordFile = controlledFilePath(ownershipDirectory, RECORD_FILE_NAME);
        this.temporaryRecordFile = controlledFilePath(
                ownershipDirectory, TEMPORARY_RECORD_FILE_NAME);
        this.directoryIdentity = directoryIdentity(ownershipDirectory, directoryIdentityToken);
        this.logicalLockIdentity = sha256(
                "enterprise-lab-owner-lock/v1\n" + directoryIdentity + "\n" + LOCK_FILE_NAME);
    }

    public static EnterpriseLabEvidenceOwnershipPaths create(Path trustedRoot) {
        return new EnterpriseLabEvidenceOwnershipPaths(trustedRoot);
    }

    public OwnershipPathSummary summary() {
        verifyDirectoryIdentity();
        return new OwnershipPathSummary(
                PATH_SCHEMA_VERSION,
                directoryIdentity,
                logicalLockIdentity,
                OWNERSHIP_DIRECTORY_NAME,
                DIRECTORY_IDENTITY_FILE_NAME,
                LOCK_FILE_NAME,
                RECORD_FILE_NAME,
                HISTORY_DIRECTORY_NAME);
    }

    public String directoryIdentity() {
        return directoryIdentity;
    }

    public String logicalLockIdentity() {
        return logicalLockIdentity;
    }

    public void verifyDirectoryIdentity() {
        String currentToken = readDirectoryIdentityForVerification(directoryIdentityFile);
        if (!MessageDigest.isEqual(
                directoryIdentityToken.getBytes(StandardCharsets.US_ASCII),
                currentToken.getBytes(StandardCharsets.US_ASCII))) {
            throw failure(FailureClassification.DIRECTORY_IDENTITY_MISMATCH,
                    "controlled ownership directory marker changed");
        }
        String current = directoryIdentity(ownershipDirectory, currentToken);
        if (!MessageDigest.isEqual(
                directoryIdentity.getBytes(StandardCharsets.US_ASCII),
                current.getBytes(StandardCharsets.US_ASCII))) {
            throw failure(FailureClassification.DIRECTORY_IDENTITY_MISMATCH,
                    "controlled ownership directory identity changed");
        }
        validateControlledDirectory(ownershipDirectory, namespace, "ownership directory");
        validateControlledDirectory(historyDirectory, ownershipDirectory, "ownership history directory");
    }

    Path trustedRoot() {
        return trustedRoot;
    }

    Path namespace() {
        return namespace;
    }

    Path ownershipDirectory() {
        return ownershipDirectory;
    }

    Path historyDirectory() {
        return historyDirectory;
    }

    Path directoryIdentityFile() {
        return directoryIdentityFile;
    }

    Path lockFile() {
        return lockFile;
    }

    Path recordFile() {
        return recordFile;
    }

    Path temporaryRecordFile() {
        return temporaryRecordFile;
    }

    String prepareLockFileIdentity() {
        prepareLockFile();
        return identityOfControlledRegularFile(lockFile);
    }

    FileChannel openLockChannel() {
        prepareLockFile();
        try {
            return FileChannel.open(lockFile,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership lock file permission was denied", exception);
        } catch (UnsupportedOperationException exception) {
            throw failure(FailureClassification.LOCK_UNSUPPORTED,
                    "ownership lock file options are unsupported", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "ownership lock file could not be opened", exception);
        }
    }

    private void prepareLockFile() {
        verifyDirectoryIdentity();
        try {
            if (!Files.exists(lockFile, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    createControlledFile(lockFile);
                } catch (FileAlreadyExistsException ignored) {
                    // A concurrent creator is accepted only after strict validation below.
                }
            }
            validateControlledRegularFile(lockFile, "ownership lock file");
            restrictPermissions(lockFile, FILE_PERMISSIONS);
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership lock file permission was denied", exception);
        } catch (UnsupportedOperationException exception) {
            throw failure(FailureClassification.LOCK_UNSUPPORTED,
                    "ownership lock file options are unsupported", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "ownership lock file could not be prepared", exception);
        }
    }

    FileChannel createTemporaryRecordChannel() {
        verifyDirectoryIdentity();
        try {
            if (Files.exists(temporaryRecordFile, LinkOption.NOFOLLOW_LINKS)) {
                throw failure(FailureClassification.RECORD_REPLACED,
                        "ownership temporary record already exists");
            }
            return openControlledFileForCreation(temporaryRecordFile);
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership temporary record permission was denied", exception);
        } catch (FileAlreadyExistsException exception) {
            throw failure(FailureClassification.RECORD_REPLACED,
                    "ownership temporary record appeared concurrently", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "ownership temporary record could not be created", exception);
        }
    }

    void validateControlledRecordFile(Path file) {
        Path safe = requireControlledFile(file);
        validateControlledRegularFile(safe, "ownership record file");
    }

    void restrictControlledFilePermissions(Path file) {
        Path safe = requireControlledFile(file);
        try {
            restrictPermissions(safe, FILE_PERMISSIONS);
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership file permission was denied", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "ownership file permissions could not be applied", exception);
        }
    }

    void forceOwnershipDirectoryMetadataIfSupported() {
        verifyDirectoryIdentity();
        forceDirectoryMetadataIfSupported(ownershipDirectory, "ownership directory");
    }

    void forceHistoryDirectoryMetadataIfSupported() {
        verifyDirectoryIdentity();
        forceDirectoryMetadataIfSupported(historyDirectory, "ownership history directory");
    }

    Path historyRecordFile(EnterpriseLabEvidenceOwnership.OwnershipRecord record) {
        return historyFile(record, ".json");
    }

    Path historyTemporaryRecordFile(EnterpriseLabEvidenceOwnership.OwnershipRecord record) {
        return historyFile(record, ".json.tmp");
    }

    FileChannel createHistoryTemporaryRecordChannel(
            EnterpriseLabEvidenceOwnership.OwnershipRecord record) {
        Path file = historyTemporaryRecordFile(record);
        verifyDirectoryIdentity();
        try {
            return openControlledFileForCreation(file);
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership history permission was denied", exception);
        } catch (FileAlreadyExistsException exception) {
            throw failure(FailureClassification.RECORD_REPLACED,
                    "ownership history temporary evidence already exists", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "ownership history temporary evidence could not be created", exception);
        }
    }

    void validateControlledHistoryFile(Path file) {
        Path safe = requireControlledHistoryFile(file);
        validateControlledRegularFile(safe, "ownership history file");
    }

    void restrictHistoryFilePermissions(Path file) {
        Path safe = requireControlledHistoryFile(file);
        try {
            restrictPermissions(safe, FILE_PERMISSIONS);
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership history permissions were denied", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "ownership history permissions could not be applied", exception);
        }
    }

    private void forceDirectoryMetadataIfSupported(Path directory, String subject) {
        if (Files.getFileAttributeView(
                directory,
                java.nio.file.attribute.PosixFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS) == null) {
            return;
        }
        try (FileChannel channel = FileChannel.open(
                directory, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            channel.force(true);
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    subject + " metadata cannot be synchronized", exception);
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    subject + " metadata synchronization is unavailable", exception);
        }
    }

    String identityOfControlledRegularFile(Path file) {
        verifyDirectoryIdentity();
        Path safe = requireControlledFile(file);
        return identity(safe, false);
    }

    private Path requireControlledFile(Path file) {
        Path safe = Objects.requireNonNull(file, "file cannot be null").toAbsolutePath().normalize();
        if (!safe.getParent().equals(ownershipDirectory)
                || !safe.startsWith(ownershipDirectory)
                || (!safe.equals(lockFile) && !safe.equals(recordFile)
                && !safe.equals(temporaryRecordFile))) {
            throw failure(FailureClassification.UNSAFE_PATH,
                    "ownership file is outside the fixed controlled set");
        }
        return safe;
    }

    private Path historyFile(
            EnterpriseLabEvidenceOwnership.OwnershipRecord record,
            String suffix) {
        EnterpriseLabEvidenceOwnership.OwnershipRecord safe = Objects.requireNonNull(
                record, "record cannot be null");
        String generation = String.format(Locale.ROOT, "%019d", safe.generation());
        String name = "owner-record-v1-g" + generation + "-"
                + safe.recordFingerprint() + suffix;
        return controlledFilePath(historyDirectory, name);
    }

    private Path requireControlledHistoryFile(Path file) {
        Path safe = Objects.requireNonNull(file, "file cannot be null")
                .toAbsolutePath().normalize();
        String name = safe.getFileName().toString();
        if (!safe.getParent().equals(historyDirectory)
                || !safe.startsWith(historyDirectory)
                || !name.matches("owner-record-v1-g[0-9]{19}-[0-9a-f]{64}\\.json(?:\\.tmp)?")) {
            throw failure(FailureClassification.UNSAFE_PATH,
                    "ownership history file is outside the derived controlled set");
        }
        return safe;
    }

    private static Path validateTrustedRoot(Path value) {
        if (value == null || !value.isAbsolute()) {
            throw failure(FailureClassification.UNSAFE_PATH,
                    "ownership data root must be an explicit absolute path");
        }
        Path normalized = value.normalize();
        String scheme = normalized.toUri().getScheme();
        if (scheme == null || !"file".equalsIgnoreCase(scheme)
                || normalized.toString().startsWith("\\\\")
                || normalized.getParent() == null) {
            throw failure(FailureClassification.UNSAFE_PATH,
                    "ownership data root must be a non-root local file path");
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    normalized, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isDirectory() || attributes.isSymbolicLink()) {
                throw failure(FailureClassification.UNSAFE_PATH,
                        "ownership data root must be an existing non-symbolic-link directory");
            }
            if (!normalized.toRealPath().equals(normalized)) {
                throw failure(FailureClassification.UNSAFE_PATH,
                        "ownership data root cannot traverse symbolic links");
            }
            return normalized;
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership data root is not accessible", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "ownership data root is unavailable", exception);
        }
    }

    private static Path controlledDirectory(Path parent, String name) {
        Path directory = parent.resolve(name).normalize();
        if (!directory.startsWith(parent) || !directory.getParent().equals(parent)) {
            throw failure(FailureClassification.UNSAFE_PATH,
                    "ownership directory escaped its controlled parent");
        }
        try {
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    createDirectory(directory);
                } catch (FileAlreadyExistsException ignored) {
                    // A concurrent creator is accepted only after strict validation below.
                }
            }
            validateControlledDirectory(directory, parent, "ownership namespace");
            restrictPermissions(directory, DIRECTORY_PERMISSIONS);
            return directory;
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership directory permission was denied", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "ownership directory could not be prepared", exception);
        }
    }

    private static void validateControlledDirectory(Path directory, Path parent, String subject) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isDirectory() || attributes.isSymbolicLink()) {
                throw failure(FailureClassification.UNSAFE_PATH,
                        subject + " must be a non-symbolic-link directory");
            }
            Path realParent = parent.toRealPath();
            Path realDirectory = directory.toRealPath();
            if (!realDirectory.getParent().equals(realParent) || !realDirectory.startsWith(realParent)) {
                throw failure(FailureClassification.UNSAFE_PATH,
                        subject + " escaped its controlled parent");
            }
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    subject + " cannot be inspected", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    subject + " is unavailable", exception);
        }
    }

    private static Path controlledFilePath(Path parent, String name) {
        Path candidate = parent.resolve(name).normalize();
        if (!candidate.startsWith(parent) || !candidate.getParent().equals(parent)) {
            throw failure(FailureClassification.UNSAFE_PATH,
                    "ownership file path escaped its controlled directory");
        }
        return candidate;
    }

    private static String prepareDirectoryIdentityFile(Path file) {
        try {
            if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                byte[] random = new byte[32];
                SECURE_RANDOM.nextBytes(random);
                byte[] encoded = (HexFormat.of().formatHex(random) + "\n")
                        .getBytes(StandardCharsets.US_ASCII);
                try (FileChannel channel = openIdentityFileForCreation(file)) {
                    if (channel != null) {
                        ByteBuffer buffer = ByteBuffer.wrap(encoded);
                        while (buffer.hasRemaining()) {
                            channel.write(buffer);
                        }
                        channel.force(true);
                    }
                }
            }
            restrictPermissions(file, FILE_PERMISSIONS);
            return readDirectoryIdentityAfterConcurrentCreation(file);
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership directory identity permission was denied", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "ownership directory identity could not be prepared", exception);
        }
    }

    private static FileChannel openIdentityFileForCreation(Path file) throws IOException {
        try {
            return FileChannel.open(file, Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
                    fileAttribute());
        } catch (UnsupportedOperationException exception) {
            try {
                return FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException ignored) {
                return null;
            }
        } catch (FileAlreadyExistsException ignored) {
            return null;
        }
    }

    private static FileChannel openControlledFileForCreation(Path file) throws IOException {
        try {
            return FileChannel.open(file,
                    Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                            LinkOption.NOFOLLOW_LINKS),
                    fileAttribute());
        } catch (UnsupportedOperationException exception) {
            return FileChannel.open(file,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
        }
    }

    private static void createControlledFile(Path file) throws IOException {
        try {
            Files.createFile(file, fileAttribute());
        } catch (UnsupportedOperationException exception) {
            Files.createFile(file);
        }
    }

    private static void validateControlledRegularFile(Path file, String subject) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile() || attributes.isSymbolicLink()) {
                throw failure(FailureClassification.UNSAFE_PATH,
                        subject + " must be a non-symbolic-link regular file");
            }
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    subject + " cannot be inspected", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    subject + " is unavailable", exception);
        }
    }

    private static void validateIdentityFile(Path file) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile() || attributes.isSymbolicLink() || attributes.size() > 65L) {
                throw failure(FailureClassification.UNSAFE_PATH,
                        "ownership directory identity must be a bounded regular file");
            }
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "ownership directory identity cannot be inspected", exception);
        }
    }

    private static String readDirectoryIdentity(Path file) throws IOException {
        String value = Files.readString(file, StandardCharsets.US_ASCII).stripTrailing();
        if (!value.matches("[0-9a-f]{64}")) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "ownership directory identity content is malformed");
        }
        return value;
    }

    private static String readDirectoryIdentityAfterConcurrentCreation(Path file) {
        EnterpriseLabEvidenceOwnershipException lastFailure = null;
        for (int attempt = 1; attempt <= DIRECTORY_IDENTITY_READ_ATTEMPTS; attempt++) {
            try {
                validateIdentityFile(file);
                return readDirectoryIdentity(file);
            } catch (EnterpriseLabEvidenceOwnershipException exception) {
                if (exception.classification() != FailureClassification.STORAGE_UNAVAILABLE) {
                    throw exception;
                }
                lastFailure = exception;
            } catch (IOException exception) {
                lastFailure = failure(FailureClassification.STORAGE_UNAVAILABLE,
                        "ownership directory identity cannot be read", exception);
            }
            if (attempt < DIRECTORY_IDENTITY_READ_ATTEMPTS) {
                LockSupport.parkNanos(DIRECTORY_IDENTITY_RETRY_NANOS);
            }
        }
        throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                "ownership directory identity remained unavailable after bounded initialization",
                lastFailure);
    }

    private static String readDirectoryIdentityForVerification(Path file) {
        try {
            validateIdentityFile(file);
            return readDirectoryIdentity(file);
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            if (exception.classification() == FailureClassification.STORAGE_UNAVAILABLE
                    || exception.classification() == FailureClassification.UNSAFE_PATH) {
                throw failure(FailureClassification.DIRECTORY_IDENTITY_MISMATCH,
                        "controlled ownership directory marker is missing or invalid", exception);
            }
            throw exception;
        } catch (IOException exception) {
            throw failure(FailureClassification.DIRECTORY_IDENTITY_MISMATCH,
                    "controlled ownership directory marker cannot be read", exception);
        }
    }

    private static String directoryIdentity(Path directory, String token) {
        return sha256(identitySeed(directory, true) + "\n" + token);
    }

    private static String identity(Path path, boolean directory) {
        return sha256(identitySeed(path, directory));
    }

    private static String identitySeed(Path path, boolean directory) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink()
                    || (directory && !attributes.isDirectory())
                    || (!directory && !attributes.isRegularFile())) {
                throw failure(FailureClassification.UNSAFE_PATH,
                        "controlled ownership storage type changed");
            }
            String fileKey = attributes.fileKey() == null
                    ? "NO_FILE_KEY"
                    : attributes.fileKey().toString();
            return "enterprise-lab-storage-identity/v1\n"
                    + path.toRealPath() + "\n"
                    + fileKey + "\n"
                    + attributes.creationTime().toInstant();
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "controlled ownership storage cannot be inspected", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "controlled ownership storage is unavailable", exception);
        }
    }

    private static void createDirectory(Path directory) throws IOException {
        try {
            Files.createDirectory(directory, directoryAttribute());
        } catch (UnsupportedOperationException exception) {
            Files.createDirectory(directory);
        }
    }

    private static FileAttribute<Set<PosixFilePermission>> directoryAttribute() {
        return PosixFilePermissions.asFileAttribute(DIRECTORY_PERMISSIONS);
    }

    private static FileAttribute<Set<PosixFilePermission>> fileAttribute() {
        return PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS);
    }

    private static void restrictPermissions(Path path, Set<PosixFilePermission> permissions)
            throws IOException {
        if (Files.getFileAttributeView(path, java.nio.file.attribute.PosixFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS) != null) {
            Files.setPosixFilePermissions(path, permissions);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static EnterpriseLabEvidenceOwnershipException failure(
            FailureClassification classification,
            String message) {
        return new EnterpriseLabEvidenceOwnershipException(classification, message);
    }

    private static EnterpriseLabEvidenceOwnershipException failure(
            FailureClassification classification,
            String message,
            Throwable cause) {
        return new EnterpriseLabEvidenceOwnershipException(classification, message, cause);
    }

    public record OwnershipPathSummary(
            String schemaVersion,
            String directoryIdentity,
            String logicalLockIdentity,
            String ownershipNamespace,
            String directoryIdentityFileName,
            String lockFileName,
            String recordFileName,
            String historyNamespace) {
        public OwnershipPathSummary {
            if (!PATH_SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException("unsupported ownership path summary version");
            }
            EnterpriseLabEvidenceOwnership.requireSha(directoryIdentity, "directoryIdentity");
            EnterpriseLabEvidenceOwnership.requireSha(logicalLockIdentity, "logicalLockIdentity");
            if (!OWNERSHIP_DIRECTORY_NAME.equals(ownershipNamespace)
                    || !LOCK_FILE_NAME.equals(lockFileName)
                    || !DIRECTORY_IDENTITY_FILE_NAME.equals(directoryIdentityFileName)
                    || !RECORD_FILE_NAME.equals(recordFileName)
                    || !HISTORY_DIRECTORY_NAME.equals(historyNamespace)) {
                throw new IllegalArgumentException("ownership path summary must use fixed controlled names");
            }
        }
    }
}
