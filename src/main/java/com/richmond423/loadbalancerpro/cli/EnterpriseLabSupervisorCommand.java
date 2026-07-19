package com.richmond423.loadbalancerpro.cli;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentTargetCatalog;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorConfiguration;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorOwnership;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorServer;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorService;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

/** Separately executable, bounded single-host Enterprise Lab supervisor mode. */
public final class EnterpriseLabSupervisorCommand {
    private static final String FLAG = "--enterprise-lab-supervisor";
    private static final String DATA_DIRECTORY_FLAG =
            "--enterprise-lab-supervisor-data-directory=";
    private static final String PORT_FLAG = "--enterprise-lab-supervisor-port=";
    private static final Path DEFAULT_DATA_DIRECTORY =
            Path.of("target", "enterprise-lab-supervisor");
    private static final int MAX_PATH_TEXT_LENGTH = 1_024;

    private EnterpriseLabSupervisorCommand() {
    }

    public static boolean isRequested(String[] args) {
        if (args == null) {
            return false;
        }
        for (String argument : args) {
            if (FLAG.equals(argument)) {
                return true;
            }
        }
        return false;
    }

    public static Result runIfRequested(
            String[] args, PrintStream out, PrintStream err) {
        if (!isRequested(args)) {
            return new Result(false, 0);
        }
        PrintStream safeOut = Objects.requireNonNull(out, "out cannot be null");
        PrintStream safeErr = Objects.requireNonNull(err, "err cannot be null");
        try {
            Options options = parse(args);
            prepareRoot(options.dataDirectory());
            EnterpriseLabExperimentTargetCatalog targets =
                    EnterpriseLabSupervisorConfiguration.approvedTargets();
            try (EnterpriseLabSupervisorOwnership ownership =
                         EnterpriseLabSupervisorOwnership.acquire(options.dataDirectory())) {
                EnterpriseLabSupervisorService service = EnterpriseLabSupervisorService.start(
                        ownership, targets, Clock.systemUTC());
                safeOut.println("enterprise-lab-supervisor-starting");
                try (EnterpriseLabSupervisorServer server =
                             new EnterpriseLabSupervisorServer(
                                     ownership,
                                     service,
                                     targets,
                                     Clock.systemUTC(),
                                     options.port())) {
                    EnterpriseLabSupervisorServer.RunResult result = server.run();
                    safeOut.println("enterprise-lab-supervisor-stopped"
                            + " reason=" + result.exitReason()
                            + " requests=" + result.requestCount()
                            + " generation=" + result.supervisorGeneration());
                    return new Result(true,
                            result.exitReason()
                                    == EnterpriseLabSupervisorServer.ExitReason.CLEAN_SHUTDOWN
                                    ? 0 : 2);
                }
            }
        } catch (EnterpriseLabSupervisorOwnership.OwnershipException exception) {
            safeErr.println("enterprise-lab supervisor ownership failed safely: "
                    + exception.failure());
            return new Result(true, 2);
        } catch (EnterpriseLabSupervisorServer.ServerException exception) {
            safeErr.println("enterprise-lab supervisor server failed safely: "
                    + exception.failure());
            return new Result(true, 2);
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            safeErr.println("enterprise-lab supervisor failed safely");
            return new Result(true, 2);
        }
    }

    private static Options parse(String[] args) {
        Path dataDirectory = DEFAULT_DATA_DIRECTORY;
        int port = 0;
        boolean dataSeen = false;
        boolean portSeen = false;
        for (String argument : args) {
            if (FLAG.equals(argument)) {
                continue;
            }
            if (argument != null && argument.startsWith(DATA_DIRECTORY_FLAG)) {
                if (dataSeen) {
                    throw new IllegalArgumentException(
                            "supervisor data directory can be supplied once");
                }
                dataSeen = true;
                String value = argument.substring(DATA_DIRECTORY_FLAG.length());
                if (value.isBlank() || value.length() > MAX_PATH_TEXT_LENGTH) {
                    throw new IllegalArgumentException(
                            "supervisor data directory text is outside hard bounds");
                }
                dataDirectory = Path.of(value);
                continue;
            }
            if (argument != null && argument.startsWith(PORT_FLAG)) {
                if (portSeen) {
                    throw new IllegalArgumentException(
                            "supervisor port can be supplied once");
                }
                portSeen = true;
                String value = argument.substring(PORT_FLAG.length());
                if (!value.matches("[0-9]{1,5}")) {
                    throw new IllegalArgumentException(
                            "supervisor port must be a bounded decimal integer");
                }
                port = EnterpriseLabSupervisorConfiguration.requireConfiguredPort(
                        Integer.parseInt(value));
                continue;
            }
            if (argument != null && argument.startsWith("--enterprise-lab-supervisor")) {
                throw new IllegalArgumentException("unknown supervisor option");
            }
        }
        return new Options(dataDirectory.toAbsolutePath().normalize(), port);
    }

    private static void prepareRoot(Path root) throws IOException {
        Files.createDirectories(root);
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(root)) {
            throw new IllegalArgumentException(
                    "supervisor data root must be a real local directory");
        }
    }

    private record Options(Path dataDirectory, int port) {
    }

    public record Result(boolean requested, int exitCode) {
    }
}
