# Remediation Report CLI

LoadBalancerPro can generate deterministic remediation reports from saved API JSON without starting the API server:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --remediation-report \
  --input saved-evaluation.json \
  --format markdown \
  --output incident-report.md \
  --report-id incident-123
```

The same packaged JAR can write structured JSON for automation:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --remediation-report \
  --input saved-replay.json \
  --format json \
  --output incident-report.json
```

## Inputs

The CLI accepts:

- a raw `POST /api/allocate/evaluate` response;
- a raw `POST /api/scenarios/replay` response;
- a report request wrapper with either `evaluation` or `replay`.
- a saved remediation report response or structured report payload.

Save evaluation JSON:

```bash
curl -fsS -X POST http://127.0.0.1:8080/api/allocate/evaluate \
  -H "Content-Type: application/json" \
  -d @evaluation-request.json \
  > saved-evaluation.json
```

Save replay JSON:

```bash
curl -fsS -X POST http://127.0.0.1:8080/api/scenarios/replay \
  -H "Content-Type: application/json" \
  -d @scenario-replay.json \
  > saved-replay.json
```

## Options

| Option | Purpose |
| --- | --- |
| `--remediation-report` | Enables offline report mode. |
| `--remediation-report=<path>` | Shorthand for offline report mode with an input file. |
| `--input <path>` | Saved evaluation, replay, or report-request JSON. |
| `--format markdown\|json` | Output format. Defaults to `markdown`. |
| `--output <path>` | Optional output file. If omitted, output is written to stdout. |
| `--report-id <id>` | Optional deterministic report id. |
| `--title <title>` | Optional Markdown report title. |

## Output Semantics

Markdown output is intended for humans and incident tickets. JSON output is the structured report payload for automation. Both formats summarize:

- accepted/allocated load;
- rejected/unallocated load;
- scaling recommendation;
- load-shedding decision;
- ranked remediation actions;
- read-only, advisory-only, and cloud-mutation guarantees;
- warnings and limitations.

The CLI reuses `RemediationReportService`, so output semantics match `POST /api/remediation/report`.

## Safety

Offline report generation:

- does not start Spring Boot or the API server;
- does not contact networks or external services;
- does not construct or mutate `CloudManager`;
- does not call AWS;
- does not execute remediation actions;
- does not generate timestamps or random ids unless the caller supplies `--report-id`.

## Limitations

- The CLI formats saved results. It does not recompute allocation, replay, routing, health checks, or remediation.
- Live deployment state should still be verified before taking operator action.
- Invalid JSON or unsupported input shapes exit non-zero and print a safe error without stack traces.
