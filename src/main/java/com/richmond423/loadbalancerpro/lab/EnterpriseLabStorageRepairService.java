package com.richmond423.loadbalancerpro.lab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Explicit offline repair for one stable incomplete tail.
 *
 * <p>The service never guesses through a corrupt complete frame. It first
 * verifies every archived segment and every complete current frame with the
 * owning store's codec and chain rules. Apply mode then preserves the exact
 * pre-repair current bytes in the controlled repair quarantine before
 * truncating only the incomplete tail under the store file's exclusive lock.</p>
 */
public final class EnterpriseLabStorageRepairService {

    public RepairReport execute(
            Path trustedRoot,
            StoreKind storeKind,
            boolean apply) {
        Path root = validateTrustedRoot(trustedRoot);
        StoreKind safeKind = Objects.requireNonNull(
                storeKind, "storeKind cannot be null");
        return switch (safeKind) {
            case APPLICATION_LEDGER -> application(root, apply);
            case SUPERVISOR_LEDGER -> supervisor(root, apply);
            case ALLOCATION_STATE -> allocation(root, apply);
        };
    }

    private static RepairReport application(Path root, boolean apply) {
        EnterpriseLabCommandLedgerEventCodec codec =
                new EnterpriseLabCommandLedgerEventCodec();
        return execute(
                root,
                root.resolve(EnterpriseLabExperimentJournalDirectory.NAMESPACE)
                        .resolve(EnterpriseLabApplicationCommandLedger.DIRECTORY_NAME)
                        .resolve(EnterpriseLabApplicationCommandLedger.FILE_NAME),
                StoreKind.APPLICATION_LEDGER,
                EnterpriseLabApplicationCommandLedger.HARD_MAX_LEDGER_BYTES,
                EnterpriseLabApplicationCommandLedger.HARD_MAX_EVENTS,
                EnterpriseLabCommandLedgerEvent.HARD_MAX_EVENT_BYTES,
                new ChainedJsonlStore.FrameCodec<EnterpriseLabCommandLedgerEvent>() {
                    @Override
                    public EnterpriseLabCommandLedgerEvent decode(byte[] encoded) {
                        return codec.decode(encoded);
                    }

                    @Override
                    public byte[] encode(EnterpriseLabCommandLedgerEvent value) {
                        return codec.encode(value);
                    }
                },
                EnterpriseLabApplicationCommandLedger::validateNext,
                apply);
    }

    private static RepairReport supervisor(Path root, boolean apply) {
        EnterpriseLabCommandLedgerEventCodec codec =
                new EnterpriseLabCommandLedgerEventCodec();
        return execute(
                root,
                root.resolve(EnterpriseLabSupervisorOwnership.DIRECTORY_NAME)
                        .resolve(EnterpriseLabSupervisorCommandLedger.DIRECTORY_NAME)
                        .resolve(EnterpriseLabSupervisorCommandLedger.FILE_NAME),
                StoreKind.SUPERVISOR_LEDGER,
                EnterpriseLabSupervisorCommandLedger.HARD_MAX_LEDGER_BYTES,
                EnterpriseLabSupervisorCommandLedger.HARD_MAX_EVENTS,
                EnterpriseLabCommandLedgerEvent.HARD_MAX_EVENT_BYTES,
                new ChainedJsonlStore.FrameCodec<EnterpriseLabCommandLedgerEvent>() {
                    @Override
                    public EnterpriseLabCommandLedgerEvent decode(byte[] encoded) {
                        return codec.decode(encoded);
                    }

                    @Override
                    public byte[] encode(EnterpriseLabCommandLedgerEvent value) {
                        return codec.encode(value);
                    }
                },
                EnterpriseLabSupervisorCommandLedger::validateNext,
                apply);
    }

    private static RepairReport allocation(Path root, boolean apply) {
        EnterpriseLabAllocationStateCodec codec =
                new EnterpriseLabAllocationStateCodec(
                        EnterpriseLabSupervisorConfiguration.approvedTargets());
        return execute(
                root,
                root.resolve(EnterpriseLabExperimentJournalDirectory.NAMESPACE)
                        .resolve(EnterpriseLabAllocationStateStore.DIRECTORY_NAME)
                        .resolve(EnterpriseLabAllocationStateStore.FILE_NAME),
                StoreKind.ALLOCATION_STATE,
                EnterpriseLabAllocationStateStore.HARD_MAX_STORE_BYTES,
                EnterpriseLabAllocationStateStore.HARD_MAX_RECORDS,
                EnterpriseLabAllocationStateCodec.HARD_MAX_RECORD_BYTES,
                new ChainedJsonlStore.FrameCodec<EnterpriseLabAllocationState>() {
                    @Override
                    public EnterpriseLabAllocationState decode(byte[] encoded) {
                        return codec.decode(encoded);
                    }

                    @Override
                    public byte[] encode(EnterpriseLabAllocationState value) {
                        return codec.encode(value);
                    }
                },
                EnterpriseLabAllocationStateStore::validateNext,
                apply);
    }

    private static <T> RepairReport execute(
            Path root,
            Path file,
            StoreKind storeKind,
            long maxBytes,
            int maxEntries,
            int maxFrameBytes,
            ChainedJsonlStore.FrameCodec<T> codec,
            ChainedJsonlStore.ChainValidator<T> validator,
            boolean apply) {
        validateStoreFile(root, file);
        ChainedJsonlStore store = new ChainedJsonlStore(file, maxBytes);
        try {
            store.validateRepairQuarantine();
            ChainedJsonlStore.SegmentedChainReplay<T> plan =
                    store.replaySegmentedChain(
                            codec,
                            validator,
                            maxEntries,
                            maxFrameBytes,
                            ChainedJsonlStore.TailPolicy.ALLOW);
            if (plan.currentTailBytes() == 0L) {
                return new RepairReport(
                        storeKind,
                        RepairStatus.HEALTHY,
                        false,
                        plan.entries().size(),
                        plan.totalBytes(),
                        0L,
                        "",
                        "",
                        true);
            }
            if (!apply) {
                return new RepairReport(
                        storeKind,
                        RepairStatus.REPAIR_AVAILABLE,
                        false,
                        plan.entries().size(),
                        plan.totalBytes(),
                        plan.currentTailBytes(),
                        "",
                        "",
                        false);
            }

            ChainedJsonlStore.TailRepairReceipt receipt =
                    store.repairTruncatedCurrentTail(
                            plan.currentCompleteBytes(),
                            plan.currentTailBytes(),
                            plan.version(),
                            () -> { });
            ChainedJsonlStore.SegmentedChainReplay<T> verified =
                    new ChainedJsonlStore(file, maxBytes)
                            .replaySegmentedChain(
                                    codec,
                                    validator,
                                    maxEntries,
                                    maxFrameBytes,
                                    ChainedJsonlStore.TailPolicy.REJECT);
            return new RepairReport(
                    storeKind,
                    RepairStatus.REPAIRED,
                    true,
                    verified.entries().size(),
                    verified.totalBytes(),
                    receipt.removedTailBytes(),
                    receipt.sourceFingerprint(),
                    receipt.quarantineFileName(),
                    true);
        } catch (ChainedJsonlStore.StoreIOException | RuntimeException exception) {
            throw new RepairException(
                    "storage repair refused because the complete chain was not safely repairable",
                    exception);
        }
    }

    private static Path validateTrustedRoot(Path value) {
        if (value == null || !value.isAbsolute()) {
            throw new IllegalArgumentException(
                    "repair data root must be an explicit absolute path");
        }
        Path root = value.toAbsolutePath().normalize();
        if (root.getParent() == null
                || root.toString().startsWith("\\\\")
                || !"file".equalsIgnoreCase(root.toUri().getScheme())) {
            throw new IllegalArgumentException(
                    "repair data root must be a non-root local filesystem path");
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    root,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink()
                    || !attributes.isDirectory()
                    || !root.toRealPath().equals(root)) {
                throw new IllegalArgumentException(
                        "repair data root identity is unsafe");
            }
            return root;
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "repair data root could not be validated",
                    exception);
        }
    }

    private static void validateStoreFile(Path root, Path file) {
        Path safeRoot = Objects.requireNonNull(root, "root cannot be null")
                .toAbsolutePath()
                .normalize();
        Path safeFile = Objects.requireNonNull(file, "file cannot be null")
                .toAbsolutePath()
                .normalize();
        if (!safeFile.startsWith(safeRoot)
                || safeFile.equals(safeRoot)
                || safeRoot.relativize(safeFile).getNameCount() < 2) {
            throw new RepairException(
                    "repair target escaped its explicit data root");
        }
        try {
            Path parent = safeRoot;
            Path relative = safeRoot.relativize(safeFile);
            for (int index = 0; index < relative.getNameCount() - 1; index++) {
                Path directory = parent.resolve(relative.getName(index))
                        .toAbsolutePath()
                        .normalize();
                BasicFileAttributes directoryAttributes = Files.readAttributes(
                        directory,
                        BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS);
                if (directoryAttributes.isSymbolicLink()
                        || !directoryAttributes.isDirectory()
                        || !directory.toRealPath().getParent()
                        .equals(parent.toRealPath())) {
                    throw new RepairException(
                            "repair target hierarchy contains an unsafe directory");
                }
                parent = directory;
            }
            BasicFileAttributes attributes = Files.readAttributes(
                    safeFile,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink()
                    || !attributes.isRegularFile()
                    || !safeFile.getParent().equals(parent)
                    || !parent.toRealPath()
                    .equals(safeFile.toRealPath(LinkOption.NOFOLLOW_LINKS)
                            .getParent())) {
                throw new RepairException(
                        "repair target is not the fixed controlled regular file");
            }
        } catch (IOException exception) {
            throw new RepairException(
                    "repair target could not be validated",
                    exception);
        }
    }

    public enum StoreKind {
        APPLICATION_LEDGER,
        SUPERVISOR_LEDGER,
        ALLOCATION_STATE;

        public static StoreKind fromWireValue(String value) {
            if (value == null) {
                throw new IllegalArgumentException(
                        "repair store kind cannot be null");
            }
            return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "application-ledger" -> APPLICATION_LEDGER;
                case "supervisor-ledger" -> SUPERVISOR_LEDGER;
                case "allocation-state" -> ALLOCATION_STATE;
                default -> throw new IllegalArgumentException(
                        "repair store must be application-ledger, supervisor-ledger, or allocation-state");
            };
        }
    }

    public enum RepairStatus {
        HEALTHY,
        REPAIR_AVAILABLE,
        REPAIRED
    }

    public record RepairReport(
            StoreKind storeKind,
            RepairStatus status,
            boolean applied,
            int verifiedCompleteEntries,
            long verifiedBytes,
            long tailBytes,
            String sourceFingerprint,
            String quarantineFileName,
            boolean exactPostRepairVerified) {
        public RepairReport {
            storeKind = Objects.requireNonNull(
                    storeKind, "storeKind cannot be null");
            status = Objects.requireNonNull(status, "status cannot be null");
            sourceFingerprint = Objects.requireNonNull(
                    sourceFingerprint, "sourceFingerprint cannot be null");
            quarantineFileName = Objects.requireNonNull(
                    quarantineFileName, "quarantineFileName cannot be null");
            if (verifiedCompleteEntries < 0
                    || verifiedBytes < 0L
                    || tailBytes < 0L
                    || !isConsistent(
                    status,
                    applied,
                    tailBytes,
                    sourceFingerprint,
                    quarantineFileName,
                    exactPostRepairVerified)) {
                throw new IllegalArgumentException(
                        "storage repair report is inconsistent");
            }
        }

        private static boolean isConsistent(
                RepairStatus status,
                boolean applied,
                long tailBytes,
                String sourceFingerprint,
                String quarantineFileName,
                boolean exactPostRepairVerified) {
            return switch (status) {
                case HEALTHY -> !applied
                        && tailBytes == 0L
                        && sourceFingerprint.isEmpty()
                        && quarantineFileName.isEmpty()
                        && exactPostRepairVerified;
                case REPAIR_AVAILABLE -> !applied
                        && tailBytes > 0L
                        && sourceFingerprint.isEmpty()
                        && quarantineFileName.isEmpty()
                        && !exactPostRepairVerified;
                case REPAIRED -> applied
                        && tailBytes > 0L
                        && sourceFingerprint.matches("[0-9a-f]{64}")
                        && quarantineFileName.equals(
                        "repair-v1-" + sourceFingerprint + ".jsonl")
                        && exactPostRepairVerified;
            };
        }
    }

    public static final class RepairException extends IllegalStateException {
        private RepairException(String message) {
            super(message);
        }

        private RepairException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
