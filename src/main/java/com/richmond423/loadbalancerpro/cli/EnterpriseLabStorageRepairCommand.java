package com.richmond423.loadbalancerpro.cli;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabStorageRepairService;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabStorageRepairService.RepairReport;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabStorageRepairService.StoreKind;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * Explicit offline command for planning or applying one bounded torn-tail
 * repair. Dry-run is the default; mutation requires the separate apply flag.
 */
public final class EnterpriseLabStorageRepairCommand {
    private static final String FLAG = "--enterprise-lab-storage-repair";
    private static final String APPLY_FLAG =
            "--enterprise-lab-storage-repair-apply";
    private static final String ROOT_FLAG =
            "--enterprise-lab-storage-repair-data-root=";
    private static final String STORE_FLAG =
            "--enterprise-lab-storage-repair-store=";

    private EnterpriseLabStorageRepairCommand() {
    }

    public static boolean isRequested(String[] args) {
        return args != null && Arrays.stream(args)
                .filter(Objects::nonNull)
                .anyMatch(FLAG::equals);
    }

    public static Result runIfRequested(
            String[] args,
            PrintStream out,
            PrintStream err) {
        if (!isRequested(args)) {
            return new Result(false, 0);
        }
        return run(args, out, err);
    }

    public static Result run(
            String[] args,
            PrintStream out,
            PrintStream err) {
        Objects.requireNonNull(args, "args cannot be null");
        Objects.requireNonNull(out, "out cannot be null");
        Objects.requireNonNull(err, "err cannot be null");
        try {
            Path root = Path.of(requiredValue(args, ROOT_FLAG));
            StoreKind store = StoreKind.fromWireValue(
                    requiredValue(args, STORE_FLAG));
            boolean apply = Arrays.stream(args)
                    .filter(Objects::nonNull)
                    .anyMatch(APPLY_FLAG::equals);
            RepairReport report =
                    new EnterpriseLabStorageRepairService()
                            .execute(root, store, apply);
            out.println("=== Enterprise Lab Storage Repair ===");
            out.println("Store: " + report.storeKind());
            out.println("Status: " + report.status());
            out.println("Applied: " + report.applied());
            out.println("Verified complete entries: "
                    + report.verifiedCompleteEntries());
            out.println("Verified bytes: " + report.verifiedBytes());
            out.println("Tail bytes: " + report.tailBytes());
            if (!report.quarantineFileName().isBlank()) {
                out.println("Controlled quarantine file: "
                        + report.quarantineFileName());
                out.println("Pre-repair SHA-256: "
                        + report.sourceFingerprint());
            }
            out.println("Exact post-repair verification: "
                    + report.exactPostRepairVerified());
            out.println("Safety: local fixed-store operation only; dry-run by default. "
                    + "Apply mode must be run with the application and supervisor stopped. "
                    + "Complete-frame corruption is refused, and original bytes are retained "
                    + "before a stable incomplete tail is removed.");
            return new Result(true, 0);
        } catch (RuntimeException exception) {
            err.println("Enterprise Lab storage repair failed safely: "
                    + safeMessage(exception));
            return new Result(true, 1);
        }
    }

    private static String requiredValue(String[] args, String prefix) {
        String selected = null;
        for (String arg : args) {
            if (arg == null || !arg.startsWith(prefix)) {
                continue;
            }
            String value = arg.substring(prefix.length());
            if (value.isBlank() || selected != null) {
                throw new IllegalArgumentException(
                        "repair command requires one non-empty " + prefix + " value");
            }
            selected = value;
        }
        if (selected == null) {
            throw new IllegalArgumentException(
                    "repair command requires " + prefix + "<value>");
        }
        return selected;
    }

    private static String safeMessage(RuntimeException exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null || message.isBlank()
                ? root.getClass().getSimpleName()
                : message;
    }

    public record Result(boolean requested, int exitCode) {
    }
}
