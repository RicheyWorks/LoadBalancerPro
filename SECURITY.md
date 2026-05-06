# Security Policy

LoadBalancerPro is an enterprise-demo and portfolio project. This policy explains how to report security issues in the repository without claiming production readiness.

## Supported Versions

| Version | Supported |
| --- | --- |
| `v2.4.x` | Yes |
| Older tags | Historical unless explicitly supported later |

## Reporting A Vulnerability

If GitHub Security Advisories are enabled for this repository, please use them for private vulnerability reporting.

If GitHub Security Advisories or private vulnerability reporting are not available, do not publish exploit details in a public issue.
Open a minimal public issue asking for a private security contact path, or contact the repository owner through GitHub profile or organization channels before disclosure.

Reports are reviewed as project capacity allows. This project does not promise a fixed response SLA. Accepted fixes may ship in a future patch release.

## What Not To Include

Do not include:

- real AWS credentials,
- API keys,
- OAuth tokens,
- private account IDs,
- customer data,
- sensitive logs,
- exploit traffic against infrastructure you do not own or have permission to test.

Use minimal reproduction details and sanitized examples wherever possible.

## Security Scope

In scope:

- Spring Boot API behavior,
- routing comparison API safety boundaries,
- Docker packaging and runtime hardening,
- CloudManager/AWS guardrails,
- dependency vulnerabilities,
- unsafe or misleading documentation that could cause unsafe operation.

Out of scope:

- social engineering,
- physical attacks,
- denial-of-service against public infrastructure,
- attacks against live AWS resources not owned by the repository owner,
- vulnerability claims requiring unsafe real-world exploitation.

## Safety Boundary

Default tests and demos are designed to avoid live AWS mutation. Cloud behavior is dry-run by default unless explicit live-mode guardrails are configured. Please do not test against live cloud resources unless you own them and have permission to do so.
