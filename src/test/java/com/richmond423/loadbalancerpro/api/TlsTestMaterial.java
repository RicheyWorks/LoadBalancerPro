package com.richmond423.loadbalancerpro.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslStoreBundle;

public final class TlsTestMaterial {
    public static final String PASSWORD = "changeit";
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(20);

    private TlsTestMaterial() {
    }

    public static Path directory(String name) {
        try {
            return Files.createTempDirectory(Path.of("target"), name + "-");
        } catch (IOException exception) {
            throw new IllegalStateException("could not create TLS test directory", exception);
        }
    }

    public static Path keyStore(Path directory, String name, String commonName, String san) {
        Path keyStore = directory.resolve(name + ".p12");
        runKeytool(
                "-genkeypair",
                "-alias", "key",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-storetype", "PKCS12",
                "-keystore", keyStore.toString(),
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-dname", "CN=" + commonName,
                "-validity", "3",
                "-ext", "SAN=" + san);
        return keyStore;
    }

    public static Path certificateAuthority(Path directory, String name) {
        Path keyStore = directory.resolve(name + ".p12");
        runKeytool(
                "-genkeypair",
                "-alias", "key",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-storetype", "PKCS12",
                "-keystore", keyStore.toString(),
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-dname", "CN=LoadBalancerPro test CA",
                "-validity", "3",
                "-ext", "BC=ca:true",
                "-ext", "KU=keyCertSign,cRLSign");
        return keyStore;
    }

    public static Path signedKeyStore(
            Path directory, String name, String commonName, String san, Path certificateAuthority) {
        Path keyStore = keyStore(directory, name, commonName, san);
        Path request = directory.resolve(name + ".csr");
        Path certificate = directory.resolve(name + "-signed.cer");
        Path authorityCertificate = directory.resolve(name + "-ca.cer");
        runKeytool(
                "-certreq",
                "-alias", "key",
                "-keystore", keyStore.toString(),
                "-storepass", PASSWORD,
                "-file", request.toString());
        runKeytool(
                "-gencert",
                "-alias", "key",
                "-keystore", certificateAuthority.toString(),
                "-storepass", PASSWORD,
                "-infile", request.toString(),
                "-outfile", certificate.toString(),
                "-validity", "3",
                "-ext", "BC=ca:false",
                "-ext", "KU=digitalSignature,keyEncipherment",
                "-ext", "EKU=serverAuth,clientAuth",
                "-ext", "SAN=" + san);
        runKeytool(
                "-exportcert",
                "-alias", "key",
                "-keystore", certificateAuthority.toString(),
                "-storepass", PASSWORD,
                "-file", authorityCertificate.toString());
        runKeytool(
                "-importcert",
                "-noprompt",
                "-alias", "ca",
                "-file", authorityCertificate.toString(),
                "-keystore", keyStore.toString(),
                "-storepass", PASSWORD);
        runKeytool(
                "-importcert",
                "-noprompt",
                "-alias", "key",
                "-file", certificate.toString(),
                "-keystore", keyStore.toString(),
                "-storepass", PASSWORD);
        return keyStore;
    }

    public static Path trustStore(Path directory, String name, Path... keyStores) {
        Path trustStore = directory.resolve(name + ".p12");
        for (int index = 0; index < keyStores.length; index++) {
            Path certificate = directory.resolve(name + "-" + index + ".cer");
            runKeytool(
                    "-exportcert",
                    "-alias", "key",
                    "-keystore", keyStores[index].toString(),
                    "-storepass", PASSWORD,
                    "-file", certificate.toString());
            runKeytool(
                    "-importcert",
                    "-noprompt",
                    "-alias", "certificate-" + index,
                    "-file", certificate.toString(),
                    "-storetype", "PKCS12",
                    "-keystore", trustStore.toString(),
                    "-storepass", PASSWORD);
        }
        return trustStore;
    }

    public static SSLContext sslContext(Path keyStorePath, Path trustStorePath) {
        try {
            KeyManagerFactory keyManagers = null;
            if (keyStorePath != null) {
                KeyStore keyStore = loadStore(keyStorePath);
                keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagers.init(keyStore, PASSWORD.toCharArray());
            }
            TrustManagerFactory trustManagers = null;
            if (trustStorePath != null) {
                KeyStore trustStore = loadStore(trustStorePath);
                trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagers.init(trustStore);
            }
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(
                    keyManagers == null ? null : keyManagers.getKeyManagers(),
                    trustManagers == null ? null : trustManagers.getTrustManagers(),
                    null);
            return context;
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("could not load TLS test material", exception);
        }
    }

    public static SslBundle sslBundle(Path keyStorePath, Path trustStorePath) {
        try {
            KeyStore keyStore = keyStorePath == null ? null : loadStore(keyStorePath);
            KeyStore trustStore = trustStorePath == null ? null : loadStore(trustStorePath);
            SslStoreBundle stores = SslStoreBundle.of(keyStore, PASSWORD, trustStore);
            SslBundleKey key = keyStore == null ? SslBundleKey.NONE : SslBundleKey.of(PASSWORD);
            return SslBundle.of(stores, key);
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("could not create TLS test bundle", exception);
        }
    }

    public static HandshakeResult handshake(SSLEngine client, SSLEngine server) throws SSLException {
        client.setUseClientMode(true);
        server.setUseClientMode(false);
        client.beginHandshake();
        server.beginHandshake();
        int packetSize = Math.max(
                client.getSession().getPacketBufferSize(), server.getSession().getPacketBufferSize());
        int applicationSize = Math.max(
                client.getSession().getApplicationBufferSize(), server.getSession().getApplicationBufferSize());
        ByteBuffer clientToServer = ByteBuffer.allocate(packetSize * 2);
        ByteBuffer serverToClient = ByteBuffer.allocate(packetSize * 2);
        ByteBuffer clientApplication = ByteBuffer.allocate(applicationSize * 2);
        ByteBuffer serverApplication = ByteBuffer.allocate(applicationSize * 2);
        ByteBuffer empty = ByteBuffer.allocate(0);

        for (int iteration = 0; iteration < 1_000; iteration++) {
            runTasks(client);
            runTasks(server);
            if (handshakeComplete(client) && handshakeComplete(server)) {
                return new HandshakeResult(
                        client.getSession().getPeerPrincipal(),
                        peerPrincipal(server));
            }
            boolean progressed = false;
            if (client.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                progressed |= transfer(client, server, empty, clientToServer, serverApplication);
            }
            if (server.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                progressed |= transfer(server, client, empty, serverToClient, clientApplication);
            }
            progressed |= unwrapAgain(client, clientApplication);
            progressed |= unwrapAgain(server, serverApplication);
            if (!progressed
                    && client.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NEED_TASK
                    && server.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NEED_TASK) {
                throw new SSLException("in-memory TLS handshake made no progress");
            }
        }
        throw new SSLException("in-memory TLS handshake did not converge");
    }

    private static boolean transfer(
            SSLEngine sender,
            SSLEngine receiver,
            ByteBuffer empty,
            ByteBuffer network,
            ByteBuffer application) throws SSLException {
        network.clear();
        SSLEngineResult wrapped = sender.wrap(empty, network);
        if (wrapped.getStatus() != SSLEngineResult.Status.OK) {
            throw new SSLException("TLS wrap failed: " + wrapped.getStatus());
        }
        network.flip();
        boolean progressed = wrapped.bytesProduced() > 0;
        while (network.hasRemaining()) {
            SSLEngineResult unwrapped = receiver.unwrap(network, application);
            if (unwrapped.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                throw new SSLException("TLS application buffer overflow");
            }
            if (unwrapped.getStatus() == SSLEngineResult.Status.CLOSED) {
                throw new SSLException("TLS peer closed during handshake");
            }
            progressed |= unwrapped.bytesConsumed() > 0;
            runTasks(receiver);
            if (unwrapped.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW
                    || unwrapped.bytesConsumed() == 0
                    || (receiver.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NEED_UNWRAP
                    && receiver.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN)) {
                break;
            }
        }
        return progressed;
    }

    private static boolean unwrapAgain(SSLEngine engine, ByteBuffer application) throws SSLException {
        if (engine.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN) {
            return false;
        }
        SSLEngineResult result = engine.unwrap(ByteBuffer.allocate(0), application);
        runTasks(engine);
        return result.bytesConsumed() > 0 || result.bytesProduced() > 0
                || result.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN;
    }

    private static void runTasks(SSLEngine engine) {
        while (engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            Runnable task = engine.getDelegatedTask();
            if (task == null) {
                break;
            }
            task.run();
        }
    }

    private static boolean handshakeComplete(SSLEngine engine) {
        return engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
                || engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED;
    }

    private static Principal peerPrincipal(SSLEngine engine) {
        try {
            return engine.getSession().getPeerPrincipal();
        } catch (SSLPeerUnverifiedException exception) {
            return null;
        }
    }

    public record HandshakeResult(Principal serverIdentity, Principal clientIdentity) {
    }

    private static KeyStore loadStore(Path path) throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(path)) {
            store.load(input, PASSWORD.toCharArray());
        }
        return store;
    }

    private static void runKeytool(String... arguments) {
        List<String> command = new ArrayList<>();
        command.add(keytool().toString());
        command.addAll(List.of(arguments));
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean exited = process.waitFor(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!exited) {
                process.destroyForcibly();
                throw new IllegalStateException("keytool timed out");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException("keytool failed: " + output);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("could not start keytool", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("keytool was interrupted", exception);
        }
    }

    private static Path keytool() {
        String executable = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "keytool.exe"
                : "keytool";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }
}
