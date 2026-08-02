# TLS Deployment

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
