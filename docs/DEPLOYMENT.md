# Proxy Production Profile

`proxy-prod` is an explicit deployment profile layered after `prod`; ordinary `prod` and the unqualified default still keep `loadbalancerpro.proxy.enabled=false`. The profile requires two upstream URLs, enables health checks, cooldown, bounded in-flight requests, graceful drain/slow-start settings, API-key authentication, and Prometheus exposure. Its Actuator surface is API-key protected. The Prometheus endpoint exposes JVM/HTTP metrics plus process-local `lbp.proxy.*` traffic, latency, in-flight, attempt/retry, byte, limit/shed, health, and cooldown series. Proxy series use fixed outcome/reason tags and validated configured route/upstream ids; they do not carry request paths, network coordinates, credentials, or request identity. The protected JSON status endpoint continues to report counters from the same proxy metrics component.

The local deployment example is [`../deploy/docker-compose.proxy-prod.yml`](../deploy/docker-compose.proxy-prod.yml). It builds the existing digest-pinned, non-root application image plus two source-built Java fixture backends. Only the application TLS port is published, on host loopback. All containers drop Linux capabilities, prohibit privilege escalation, use read-only root filesystems with bounded `/tmp` mounts, and receive SIGTERM with a 35-second stop window. The fixtures are local validation backends, not a production backend design.

Prepare runtime material outside the repository. `certificate.pem`, `private-key.pem`, and `ca.pem` must describe the same trusted server identity; never commit them:

```bash
runtime_dir="$(mktemp -d)"
mkdir -p "$runtime_dir"/{tls,trust,identity,config}
openssl rand -hex 32 > "$runtime_dir/api-key"
ca_private_key="$runtime_dir/ca-private-key.pem"
server_csr="$runtime_dir/server.csr"
server_extensions="$runtime_dir/server-extensions.cnf"
openssl req -x509 -newkey rsa:2048 -sha256 -days 1 -nodes \
  -subj "/CN=LoadBalancerPro Local Deployment CA" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical,keyCertSign,cRLSign" \
  -keyout "$ca_private_key" \
  -out "$runtime_dir/tls/ca.pem"
openssl req -newkey rsa:2048 -sha256 -nodes \
  -subj "/CN=lbp.local" \
  -keyout "$runtime_dir/tls/private-key.pem" \
  -out "$server_csr"
printf '%s\n' \
  'subjectAltName=DNS:lbp.local,IP:127.0.0.1' \
  'basicConstraints=critical,CA:FALSE' \
  'keyUsage=critical,digitalSignature,keyEncipherment' \
  'extendedKeyUsage=serverAuth' > "$server_extensions"
openssl x509 -req -sha256 -days 1 \
  -in "$server_csr" \
  -CA "$runtime_dir/tls/ca.pem" \
  -CAkey "$ca_private_key" \
  -set_serial 1 \
  -extfile "$server_extensions" \
  -out "$runtime_dir/tls/certificate.pem"
rm -f -- "$ca_private_key" "$server_csr" "$server_extensions"
chmod 0444 "$runtime_dir/api-key" "$runtime_dir/tls/"*.pem
chmod 0555 "$runtime_dir"/{tls,trust,identity,config}

export LBP_API_KEY_FILE="$runtime_dir/api-key"
export LBP_TLS_DIRECTORY="$runtime_dir/tls"
export LBP_TRUST_DIRECTORY="$runtime_dir/trust"
export LBP_IDENTITY_DIRECTORY="$runtime_dir/identity"
export LBP_CONFIG_DIRECTORY="$runtime_dir/config"
docker compose -f deploy/docker-compose.proxy-prod.yml up --build -d
```

Verify TLS, public health, authenticated proxying, and protected metrics:

```bash
curl --cacert "$LBP_TLS_DIRECTORY/ca.pem" --resolve lbp.local:18443:127.0.0.1 \
  https://lbp.local:18443/api/health
curl --cacert "$LBP_TLS_DIRECTORY/ca.pem" --resolve lbp.local:18443:127.0.0.1 \
  -H "X-API-Key: $(<"$LBP_API_KEY_FILE")" https://lbp.local:18443/proxy/example
curl --cacert "$LBP_TLS_DIRECTORY/ca.pem" --resolve lbp.local:18443:127.0.0.1 \
  -H "X-API-Key: $(<"$LBP_API_KEY_FILE")" https://lbp.local:18443/actuator/prometheus
docker compose -f deploy/docker-compose.proxy-prod.yml down
```

The API key is mounted read-only as `/run/secrets/loadbalancerpro.api.key` and imported through Spring config trees. The temporary example uses read-only files inside a private `mktemp` parent so the image's non-root user can read the mounts; for a durable host path, grant read access only to the runtime UID/GID through the host's ownership or ACL mechanism. TLS, trust, client identity, and additional configuration directories are separate read-only mounts. Define backend custom trust or mTLS bundles only in the external configuration directory, for example:

```properties
spring.ssl.bundle.pem.backendtrust.truststore.certificate=file:/run/trust/ca.pem
spring.ssl.bundle.pem.backendidentity.keystore.certificate=file:/run/identity/client-certificate.pem
spring.ssl.bundle.pem.backendidentity.keystore.private-key=file:/run/identity/client-private-key.pem
loadbalancerpro.proxy.backend-tls.truststore=backendtrust
loadbalancerpro.proxy.upstreams[0].tls.client-cert=backendidentity
```

Hostname verification remains mandatory; `tls.verify=false` is rejected. Server PEM reload is enabled, but replacement behavior depends on the mounted filesystem propagating atomic file updates. Run `bash scripts/smoke/proxy-prod-compose-smoke.sh` for the destructive local stack smoke; it creates ephemeral loopback-only material, verifies the image does not contain it, sends SIGTERM during an in-flight request, and removes its temporary project.

## Proxy Profile Configuration

| Environment variable | Property | Profile default |
| --- | --- | --- |
| `LBP_UPSTREAM_0_URL` | `loadbalancerpro.proxy.upstreams[0].url` | required |
| `LBP_UPSTREAM_1_URL` | `loadbalancerpro.proxy.upstreams[1].url` | required |
| `LBP_PROXY_STRATEGY` | `loadbalancerpro.proxy.strategy` | `ROUND_ROBIN` |
| `LBP_HTTP2_ENABLED` | `server.http2.enabled` | `true` |
| `LBP_WEBSOCKET_ENABLED` | `loadbalancerpro.proxy.websocket.enabled` | `false` |
| `LBP_WEBSOCKET_CONNECT_TIMEOUT` | `loadbalancerpro.proxy.websocket.connect-timeout` | `5s` |
| `LBP_WEBSOCKET_IDLE_TIMEOUT` | `loadbalancerpro.proxy.websocket.idle-timeout` | `5m` |
| `LBP_WEBSOCKET_SEND_TIMEOUT` | `loadbalancerpro.proxy.websocket.send-timeout` | `10s` |
| `LBP_WEBSOCKET_MAX_TEXT_MESSAGE_BYTES` | `loadbalancerpro.proxy.websocket.max-text-message-bytes` | `65536` |
| `LBP_WEBSOCKET_MAX_BINARY_MESSAGE_BYTES` | `loadbalancerpro.proxy.websocket.max-binary-message-bytes` | `65536` |
| `LBP_WEBSOCKET_SEND_BUFFER_BYTES` | `loadbalancerpro.proxy.websocket.send-buffer-bytes` | `262144` |
| `LBP_CONNECT_TIMEOUT` | `loadbalancerpro.proxy.connect-timeout` | `1s` |
| `LBP_REQUEST_TIMEOUT` | `loadbalancerpro.proxy.request-timeout` | `30s` |
| `LBP_MAX_REQUEST_BYTES` | `loadbalancerpro.proxy.max-request-bytes` | `65536` |
| `LBP_MAX_RESPONSE_BYTES` | `loadbalancerpro.proxy.max-response-bytes` | `0` (streaming/unbounded) |
| `LBP_MAX_IN_FLIGHT` | `loadbalancerpro.proxy.limits.max-in-flight` | `100` |
| `LBP_HEALTH_CHECK_PATH` | `loadbalancerpro.proxy.health-check.path` | `/health` |
| `LBP_HEALTH_CHECK_INTERVAL` | `loadbalancerpro.proxy.health-check.interval` | `5s` |
| `LBP_COOLDOWN_DURATION` | `loadbalancerpro.proxy.cooldown.duration` | `30s` |
| `LBP_DRAIN_TIMEOUT` | `loadbalancerpro.proxy.reload.drain-timeout` | `30s` |
| `LBP_SLOW_START_DURATION` | `loadbalancerpro.proxy.slow-start.duration` | `5s` |
| `LBP_BACKEND_TRUST_BUNDLE` | `loadbalancerpro.proxy.backend-tls.truststore` | blank |
| `LBP_UPSTREAM_0_CLIENT_CERT_BUNDLE` | `loadbalancerpro.proxy.upstreams[0].tls.client-cert` | blank |

[`../deploy/kubernetes-proxy-prod.yaml`](../deploy/kubernetes-proxy-prod.yaml) is the canonical deployment base. It
encodes two replicas, zero-unavailable rolling replacement, a two-domain zone-spread rule that permits a temporary
surge pod, preferred host spreading, a one-replica disruption budget, startup/readiness/liveness probes, a five-second
preStop delay, a 40-second termination window, a token-free service account, numeric non-root execution, and external
Secret/ConfigMap mounts. Its image remains a deliberately non-resolving digest placeholder. The disposable
[`../scripts/bench/proxy-kubernetes-topology.sh`](../scripts/bench/proxy-kubernetes-topology.sh) lane applies the
separate loopback qualification workload and proves a metadata-only content-distinct candidate rollout and baseline
rollback under continuous traffic, complete pod-UID turnover in both directions, runtime-image identity transition and
restoration, ready-endpoint continuity, two-zone Service distribution, planned worker removal, and
operator-remediated no-drain worker loss and recovery. The abrupt-loss exercise forcibly stops the kind worker,
confirms its container is down, applies the out-of-service `NoExecute` taint, and force-removes the three exact stateless
qualification pods from the API. The disposable cluster also pins immediate EndpointSlice-triggered iptables updates
and a one-second kube-proxy cleanup sync; operators must review the equivalent setting or managed-ingress
behavior for their environment. The candidate preserves the baseline application layers, so this does not prove
application-layer release compatibility or automatic deployment failure detection. The reviewed staging runner
separately validates the external target's digest, replicas, zones, resources,
configuration, ingress, metrics, drain, and transitions; local proof does not establish registry integrity, deployment
capacity, external ingress behavior, or production readiness.

After the staging failure matrix passes, use
[`../scripts/bench/proxy-staging-capacity-staircase.sh`](../scripts/bench/proxy-staging-capacity-staircase.sh) for the
deployment-equivalent rate ladder. It requires the same exact staging profile plus a hash-bound capacity profile and
external per-replica telemetry adapter, and always restores the prior digest before accepting an envelope.

HTTP/2 applies to ordinary inbound HTTP traffic. WebSocket passthrough uses an HTTP/1.1 Upgrade handshake, shares the
proxy's routing/concurrency/drain controls, and does not implement RFC 8441 extended CONNECT. Browser origin and
subprotocol allow-lists use indexed `loadbalancerpro.proxy.websocket.allowed-origins[...]` and
`loadbalancerpro.proxy.websocket.subprotocols[...]` properties. Remove inbound proxy credentials through route header
policy when they are not upstream credentials. See [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md) for the bounded
message, timeout, header, reload, and retry behavior.

## TLS Deployment

TLS material, DNS, firewall policy, secret delivery, and certificate issuance remain operator responsibilities. Spring SSL bundles support JKS or PEM (`spring.ssl.bundle.pem.<name>.keystore.{certificate,private-key}`); this JKS example enables HTTPS, SNI, and watched file reload, with `edge-api` defined from its own keystore by the same named-bundle pattern:

```properties
spring.ssl.bundle.jks.edge.keystore.location=file:/run/secrets/edge.p12
spring.ssl.bundle.jks.edge.keystore.password=${EDGE_STORE_PASSWORD}
spring.ssl.bundle.jks.edge.reload-on-update=true
server.ssl.enabled=true
server.ssl.bundle=edge
server.ssl.server-name-bundles[0].server-name=api.example.test
server.ssl.server-name-bundles[0].bundle=edge-api
```

For backend TLS, define named `spring.ssl.bundle.jks.*` or `.pem.*` bundles, set `loadbalancerpro.proxy.backend-tls.truststore=<trust-bundle>`, and optionally set each HTTPS target's `tls.client-cert=<identity-bundle>`; `tls.verify` defaults true and false is rejected.

## Graceful Lifecycle

All profiles inherit Spring's graceful server shutdown with a 30-second lifecycle phase timeout. An accepted proxy reload immediately stops selecting removed targets, retains their process-local configuration/runtime state until prior-generation requests drain or `loadbalancerpro.proxy.reload.drain-timeout` expires (default 30 seconds, maximum 10 minutes), and starts newly added targets through the configured slow-start ramp. A drain timeout bounds retained state; it does not claim to cancel an already-running upstream exchange.
