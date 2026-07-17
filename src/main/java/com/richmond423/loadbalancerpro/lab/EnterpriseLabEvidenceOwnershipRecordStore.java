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
        paths.verifyDirectoryIdentity();
        PathState state = inspectRecordPath();
        if (state == PathState.ABSENT) {
            return Optional.empty();
        }
        return Optional.of(readRequired());
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
        OwnershipRecord safeCurrent = Objects.requireNonNull(
                expectedCurrent, "expectedCurrent cannot be null");
        OwnershipRecord current = readRequired();
        if (!current.recordFingerprint().equals(safeCurrent.recordFingerprint())) {
            throw failure(FailureClassification.RECORD_REPLACED,
                    "ownership record changed before release publication");
        }
        return writeAndVerify(safeCurrent, replacement, true, WritePurpose.RELEASE);
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
                        "ownership record changed during release publication");
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

    private OwnershipRecord readRequired() {
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
            requireDirectoryIdentity(record);
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
                FailurePoint.AFTER_RECORD_INSTALL),
        RELEASE(
                FailurePoint.DURING_RELEASE_RECORD_WRITE,
                FailurePoint.AFTER_RELEASE_RECORD_FORCE,
                FailurePoint.AFTER_RELEASE_RECORD_INSTALL);

        private final FailurePoint writeFailurePoint;
        private final FailurePoint forceFailurePoint;
        private final FailurePoint installFailurePoint;

        WritePurpose(
                FailurePoint writeFailurePoint,
                FailurePoint forceFailurePoint,
                FailurePoint installFailurePoint) {
            this.writeFailurePoint = writeFailurePoint;
            this.forceFailurePoint = forceFailurePoint;
            this.installFailurePoint = installFailurePoint;
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
    }
}
