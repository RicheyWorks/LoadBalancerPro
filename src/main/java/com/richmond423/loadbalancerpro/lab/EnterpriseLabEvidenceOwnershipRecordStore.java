package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipCodec.CodecException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.FailureInjector;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.FailurePoint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;

/** Fixed-path durable owner-record installation and verification. */
final class EnterpriseLabEvidenceOwnershipRecordStore {
    private final EnterpriseLabEvidenceOwnershipPaths paths;
    private final EnterpriseLabEvidenceOwnershipCodec codec;
    private final FailureInjector failureInjector;

    EnterpriseLabEvidenceOwnershipRecordStore(
            EnterpriseLabEvidenceOwnershipPaths paths,
            EnterpriseLabEvidenceOwnershipCodec codec,
            FailureInjector failureInjector) {
        this.paths = Objects.requireNonNull(paths, "paths cannot be null");
        this.codec = Objects.requireNonNull(codec, "codec cannot be null");
        this.failureInjector = Objects.requireNonNull(
                failureInjector, "failureInjector cannot be null");
    }

    Optional<OwnershipRecord> readIfPresent() {
        return readIfPresent(true);
    }

    Optional<OwnershipRecord> readIfPresentForTakeover() {
        return readIfPresent(false);
    }

    private Optional<OwnershipRecord> readIfPresent(boolean requireMatchingDirectory) {
        paths.verifyDirectoryIdentity();
        PathState state = inspectRecordPath();
        if (state == PathState.ABSENT) {
            return Optional.empty();
        }
        return Optional.of(readRequired(requireMatchingDirectory));
    }

    OwnershipRecord writeNewAndVerify(OwnershipRecord record) {
        if (readIfPresent().isPresent()) {
            throw failure(FailureClassification.RECORD_REPLACED,
                    "ownership record already exists and requires takeover evaluation");
        }
        return writeAndVerify(null, record, false, WritePurpose.ACQUISITION);
    }

    OwnershipRecord replaceAndVerify(
            OwnershipRecord expectedCurrent,
            OwnershipRecord replacement) {
        return replaceAndVerify(expectedCurrent, replacement, WritePurpose.RELEASE);
    }

    OwnershipRecord renewAndVerify(
            OwnershipRecord expectedCurrent,
            OwnershipRecord replacement) {
        return replaceAndVerify(expectedCurrent, replacement, WritePurpose.RENEWAL);
    }

    OwnershipRecord takeoverAndVerify(
            OwnershipRecord expectedCurrent,
            OwnershipRecord replacement) {
        return replaceAndVerify(expectedCurrent, replacement, WritePurpose.TAKEOVER);
    }

    void recoverInterruptedTakeoverTemporaries(OwnershipRecord expectedCurrent) {
        OwnershipRecord safe = Objects.requireNonNull(
                expectedCurrent, "expectedCurrent cannot be null");
        OwnershipRecord current = readRequired();
        if (!current.equals(safe)) {
            throw failure(FailureClassification.RECORD_REPLACED,
                    "ownership record changed before temporary recovery");
        }
        removeInterruptedTemporary(
                paths.temporaryRecordFile(), false, "ownership record temporary evidence");
        removeInterruptedTemporary(
                paths.historyTemporaryRecordFile(safe), true,
                "ownership history temporary evidence");
    }

    OwnershipRecord archivePriorAndVerify(OwnershipRecord prior) {
        OwnershipRecord safe = Objects.requireNonNull(prior, "prior cannot be null");
        requireDirectoryIdentity(safe);
        Path target = paths.historyRecordFile(safe);
        Path temporary = paths.historyTemporaryRecordFile(safe);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            OwnershipRecord existing = readHistoryRequired(target);
            if (!existing.equals(safe)) {
                throw failure(FailureClassification.RECORD_REPLACED,
                        "ownership history target contains different evidence");
            }
            return existing;
        }
        if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) {
            throw failure(FailureClassification.RECORD_REPLACED,
                    "ownership history temporary evidence already exists");
        }

        byte[] encoded = codec.encode(safe);
        try (FileChannel channel = paths.createHistoryTemporaryRecordChannel(safe)) {
            failureInjector.check(FailurePoint.DURING_TAKEOVER_HISTORY_WRITE);
            ByteBuffer buffer = ByteBuffer.wrap(encoded);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
            failureInjector.check(FailurePoint.AFTER_TAKEOVER_HISTORY_FORCE);
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership history synchronization permission was denied", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.IO_FAILURE,
                    "ownership history could not be force-synchronized", exception);
        }

        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            paths.restrictHistoryFilePermissions(target);
            forceInstalledHistory(target);
            paths.forceHistoryDirectoryMetadataIfSupported();
            failureInjector.check(FailurePoint.AFTER_TAKEOVER_HISTORY_INSTALL);
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership history installation permission was denied", exception);
        } catch (UnsupportedOperationException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "atomic ownership history installation is unsupported", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.IO_FAILURE,
                    "ownership history could not be atomically installed", exception);
        }

        OwnershipRecord archived = readHistoryRequired(target);
        if (!archived.equals(safe)) {
            throw failure(FailureClassification.RECORD_REPLACED,
                    "installed ownership history did not verify exactly");
        }
        return archived;
    }

    private OwnershipRecord replaceAndVerify(
            OwnershipRecord expectedCurrent,
            OwnershipRecord replacement,
            WritePurpose purpose) {
        OwnershipRecord safeCurrent = Objects.requireNonNull(
                expectedCurrent, "expectedCurrent cannot be null");
        OwnershipRecord current = readRequired();
        if (!current.recordFingerprint().equals(safeCurrent.recordFingerprint())) {
            throw failure(FailureClassification.RECORD_REPLACED,
                    "ownership record changed before " + purpose.description() + " publication");
        }
        return writeAndVerify(safeCurrent, replacement, true, purpose);
    }

    private OwnershipRecord writeAndVerify(
            OwnershipRecord expectedCurrent,
            OwnershipRecord record,
            boolean replaceExisting,
            WritePurpose purpose) {
        OwnershipRecord safe = Objects.requireNonNull(record, "record cannot be null");
        requireDirectoryIdentity(safe);
        byte[] encoded = codec.encode(safe);
        try (FileChannel channel = paths.createTemporaryRecordChannel()) {
            failureInjector.check(purpose.writeFailurePoint());
            ByteBuffer buffer = ByteBuffer.wrap(encoded);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
            failureInjector.check(purpose.forceFailurePoint());
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership record synchronization permission was denied", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.IO_FAILURE,
                    "ownership record could not be force-synchronized", exception);
        }

        paths.verifyDirectoryIdentity();
        if (replaceExisting) {
            OwnershipRecord current = readRequired();
            if (!current.recordFingerprint().equals(expectedCurrent.recordFingerprint())) {
                throw failure(FailureClassification.RECORD_REPLACED,
                        "ownership record changed during " + purpose.description() + " publication");
            }
        } else if (inspectRecordPath() != PathState.ABSENT) {
            throw failure(FailureClassification.RECORD_REPLACED,
                    "ownership record appeared during initial publication");
        }

        try {
            if (replaceExisting) {
                Files.move(paths.temporaryRecordFile(), paths.recordFile(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(paths.temporaryRecordFile(), paths.recordFile(),
                        StandardCopyOption.ATOMIC_MOVE);
            }
            paths.restrictControlledFilePermissions(paths.recordFile());
            forceInstalledRecord();
            paths.forceOwnershipDirectoryMetadataIfSupported();
            failureInjector.check(purpose.installFailurePoint());
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership record atomic installation permission was denied", exception);
        } catch (UnsupportedOperationException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "atomic ownership record installation is unsupported", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.IO_FAILURE,
                    "ownership record could not be atomically installed", exception);
        }

        OwnershipRecord verified = readRequired();
        if (!verified.equals(safe)) {
            throw failure(FailureClassification.RECORD_REPLACED,
                    "installed ownership record did not verify exactly");
        }
        return verified;
    }

    private void forceInstalledRecord() {
        try (FileChannel channel = FileChannel.open(paths.recordFile(),
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            channel.force(true);
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "installed ownership record cannot be synchronized", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.IO_FAILURE,
                    "installed ownership record synchronization failed", exception);
        }
    }

    private void removeInterruptedTemporary(
            Path temporary,
            boolean history,
            String subject) {
        if (!Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (history) {
            paths.validateControlledHistoryFile(temporary);
        } else {
            paths.validateControlledRecordFile(temporary);
        }
        try {
            Files.delete(temporary);
            if (history) {
                paths.forceHistoryDirectoryMetadataIfSupported();
            } else {
                paths.forceOwnershipDirectoryMetadataIfSupported();
            }
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    subject + " cannot be removed after interrupted publication", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.IO_FAILURE,
                    subject + " cleanup failed", exception);
        }
    }

    private void forceInstalledHistory(Path file) {
        try (FileChannel channel = FileChannel.open(
                file, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            channel.force(true);
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "installed ownership history cannot be synchronized", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.IO_FAILURE,
                    "installed ownership history synchronization failed", exception);
        }
    }

    private OwnershipRecord readHistoryRequired(Path file) {
        paths.verifyDirectoryIdentity();
        paths.validateControlledHistoryFile(file);
        try {
            long size = Files.size(file);
            if (size < 1 || size > EnterpriseLabEvidenceOwnershipCodec.HARD_MAX_RECORD_BYTES) {
                throw failure(FailureClassification.RECORD_MALFORMED,
                        "ownership history is outside bounded size limits");
            }
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length != size) {
                throw failure(FailureClassification.RECORD_REPLACED,
                        "ownership history changed while being read");
            }
            OwnershipRecord record = codec.decode(bytes);
            requireDirectoryIdentity(record);
            return record;
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (CodecException exception) {
            throw codecFailure(exception);
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership history cannot be read", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.IO_FAILURE,
                    "ownership history verification failed", exception);
        }
    }

    private OwnershipRecord readRequired() {
        return readRequired(true);
    }

    private OwnershipRecord readRequired(boolean requireMatchingDirectory) {
        paths.verifyDirectoryIdentity();
        if (inspectRecordPath() == PathState.ABSENT) {
            throw failure(FailureClassification.RECORD_REPLACED,
                    "required ownership record is absent");
        }
        paths.validateControlledRecordFile(paths.recordFile());
        try {
            long size = Files.size(paths.recordFile());
            if (size < 1 || size > EnterpriseLabEvidenceOwnershipCodec.HARD_MAX_RECORD_BYTES) {
                throw failure(FailureClassification.RECORD_MALFORMED,
                        "ownership record is outside bounded size limits");
            }
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            try (FileChannel channel = FileChannel.open(paths.recordFile(),
                    StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                while (buffer.hasRemaining()) {
                    if (channel.read(buffer) < 0) {
                        throw failure(FailureClassification.RECORD_REPLACED,
                                "ownership record was truncated during verification");
                    }
                }
                ByteBuffer extra = ByteBuffer.allocate(1);
                if (channel.read(extra) > 0) {
                    throw failure(FailureClassification.RECORD_REPLACED,
                            "ownership record grew during verification");
                }
            }
            paths.verifyDirectoryIdentity();
            OwnershipRecord record = codec.decode(buffer.array());
            if (requireMatchingDirectory) {
                requireDirectoryIdentity(record);
            }
            return record;
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (CodecException exception) {
            throw codecFailure(exception);
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership record cannot be read", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.IO_FAILURE,
                    "ownership record verification failed", exception);
        }
    }

    private PathState inspectRecordPath() {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    paths.recordFile(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile() || attributes.isSymbolicLink()) {
                throw failure(FailureClassification.UNSAFE_PATH,
                        "ownership record must be a non-symbolic-link regular file");
            }
            return PathState.REGULAR_FILE;
        } catch (NoSuchFileException exception) {
            return PathState.ABSENT;
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(FailureClassification.PERMISSION_DENIED,
                    "ownership record path cannot be inspected", exception);
        } catch (IOException exception) {
            throw failure(FailureClassification.STORAGE_UNAVAILABLE,
                    "ownership record path is unavailable", exception);
        }
    }

    private void requireDirectoryIdentity(OwnershipRecord record) {
        if (!paths.directoryIdentity().equals(record.directoryIdentity())) {
            throw failure(FailureClassification.DIRECTORY_IDENTITY_MISMATCH,
                    "ownership record identifies a different controlled directory");
        }
    }

    private static EnterpriseLabEvidenceOwnershipException codecFailure(CodecException exception) {
        FailureClassification classification = switch (exception.failure()) {
            case UNSUPPORTED_VERSION -> FailureClassification.UNSUPPORTED_RECORD_VERSION;
            case FINGERPRINT_MISMATCH -> FailureClassification.RECORD_FINGERPRINT_MISMATCH;
            case MALFORMED_RECORD, UNKNOWN_FIELD, EXCEEDED_BOUNDS ->
                    FailureClassification.RECORD_MALFORMED;
        };
        return failure(classification, "ownership record canonical verification failed", exception);
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

    private enum PathState {
        ABSENT,
        REGULAR_FILE
    }

    private enum WritePurpose {
        ACQUISITION(
                FailurePoint.DURING_RECORD_WRITE,
                FailurePoint.AFTER_RECORD_FORCE,
                FailurePoint.AFTER_RECORD_INSTALL,
                "acquisition"),
        RENEWAL(
                FailurePoint.DURING_RENEWAL_RECORD_WRITE,
                FailurePoint.AFTER_RENEWAL_RECORD_FORCE,
                FailurePoint.AFTER_RENEWAL_RECORD_INSTALL,
                "renewal"),
        TAKEOVER(
                FailurePoint.DURING_TAKEOVER_RECORD_WRITE,
                FailurePoint.AFTER_TAKEOVER_RECORD_FORCE,
                FailurePoint.AFTER_TAKEOVER_RECORD_INSTALL,
                "takeover"),
        RELEASE(
                FailurePoint.DURING_RELEASE_RECORD_WRITE,
                FailurePoint.AFTER_RELEASE_RECORD_FORCE,
                FailurePoint.AFTER_RELEASE_RECORD_INSTALL,
                "release");

        private final FailurePoint writeFailurePoint;
        private final FailurePoint forceFailurePoint;
        private final FailurePoint installFailurePoint;
        private final String description;

        WritePurpose(
                FailurePoint writeFailurePoint,
                FailurePoint forceFailurePoint,
                FailurePoint installFailurePoint,
                String description) {
            this.writeFailurePoint = writeFailurePoint;
            this.forceFailurePoint = forceFailurePoint;
            this.installFailurePoint = installFailurePoint;
            this.description = description;
        }

        FailurePoint writeFailurePoint() {
            return writeFailurePoint;
        }

        FailurePoint forceFailurePoint() {
            return forceFailurePoint;
        }

        FailurePoint installFailurePoint() {
            return installFailurePoint;
        }

        String description() {
            return description;
        }
    }
}
