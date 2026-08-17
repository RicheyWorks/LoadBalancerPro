#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
compose_base="$repo_root/deploy/docker-compose.proxy-prod.yml"
compose_override="$script_dir/docker-compose.bench.yml"
example_profile="$script_dir/capacity-profile.example.json"
target_renderer="$script_dir/render-capacity-targets.jq"

mode="validate"
profile="$example_profile"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --mode)
            [[ $# -ge 2 ]] || { echo "--mode requires validate or run" >&2; exit 2; }
            mode="$2"
            shift 2
            ;;
        --profile)
            [[ $# -ge 2 ]] || { echo "--profile requires a JSON file" >&2; exit 2; }
            profile="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 --mode validate|run [--profile capacity-profile.json]"
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            exit 2
            ;;
    esac
done

case "$mode" in
    validate|run) ;;
    *) echo "Mode must be validate or run" >&2; exit 2 ;;
esac

for required_file in "$compose_base" "$compose_override" "$profile" "$target_renderer"; do
    [[ -f "$required_file" ]] || { echo "Missing required file: $required_file" >&2; exit 2; }
done
command -v jq >/dev/null 2>&1 || { echo "jq is required" >&2; exit 2; }

validate_profile() {
    jq -e '
      .schemaVersion == 1
      and (.profileId | type == "string" and test("^[a-z0-9][a-z0-9._-]{0,62}$"))
      and (.review.status | type == "string" and length > 0)
      and (.workload.requestRate.normalPerSecond | type == "number" and . > 0 and floor == .)
      and (.workload.requestRate.peakPerSecond | type == "number" and . > 0 and floor == .)
      and (.workload.requestRate.burstPerSecond | type == "number" and . > 0 and floor == .)
      and (.workload.requestRate.burstDurationSeconds | type == "number" and . > 0 and floor == .)
      and (.workload.requestRate.expectedGrowthPercent | type == "number" and . >= 0 and floor == .)
      and (.workload.requestRate.normalPerSecond <= .workload.requestRate.peakPerSecond
          and .workload.requestRate.peakPerSecond <= .workload.requestRate.burstPerSecond)
      and (.workload.concurrency.clientConnections | type == "number" and . > 0 and floor == .)
      and (.workload.concurrency.http2Streams | type == "number" and . >= 0 and floor == .)
      and (.workload.concurrency.webSockets | type == "number" and . >= 0 and floor == .)
      and (.workload.concurrency.keepAlive | type == "boolean")
      and .workload.concurrency.connectionChurn == "vegeta-default"
      and (.workload.routeMix | type == "array" and length > 0)
      and (([.workload.routeMix[].percent] | add) == 100)
      and (all(.workload.routeMix[];
          (.path | type == "string" and startswith("/proxy/"))
          and (.method | IN("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"))
          and (.retryEligible | type == "boolean")
          and (.retryEligible == ((.method == "GET") or (.method == "HEAD")))
          and (.affinity == false)
          and (.strategy | IN("TAIL_LATENCY_POWER_OF_TWO", "WEIGHTED_LEAST_LOAD",
              "WEIGHTED_LEAST_CONNECTIONS", "WEIGHTED_ROUND_ROBIN", "ROUND_ROBIN", "CONSISTENT_HASH"))
          and (.percent | type == "number" and . > 0 and floor == .)))
      and (([.workload.routeMix[].strategy] | unique | length) == 1)
      and (.workload.payload | type == "object")
      and (all([
          .workload.payload.requestBytes.p50, .workload.payload.requestBytes.p95,
          .workload.payload.requestBytes.p99, .workload.payload.responseBytes.p50,
          .workload.payload.responseBytes.p95, .workload.payload.responseBytes.p99][];
          type == "number" and . >= 0 and floor == .))
      and (.workload.payload.requestBytes.p50 <= .workload.payload.requestBytes.p95
          and .workload.payload.requestBytes.p95 <= .workload.payload.requestBytes.p99
          and .workload.payload.requestBytes.p99 <= 65536)
      and (.workload.payload.responseBytes.p50 <= .workload.payload.responseBytes.p95
          and .workload.payload.responseBytes.p95 <= .workload.payload.responseBytes.p99
          and .workload.payload.responseBytes.p99 <= 1048576)
      and (.workload.payload.streamingResponses | type == "boolean")
      and (.workload.payload.serverSentEvents | type == "boolean")
      and (.workload.payload.uploads | type == "boolean")
      and .workload.upstreams.count == 2
      and (.workload.upstreams.connectionLimitPerUpstream | type == "number" and . > 0 and floor == .)
      and (.workload.upstreams.latencyModel | type == "string" and length > 0)
      and (.workload.upstreams.errorModes | type == "array" and length > 0)
      and (.workload.upstreams.healthEndpointCost | type == "string" and length > 0)
      and (.workload.upstreams.transport | type == "string" and length > 0)
      and (.workload.objectives.minimumSuccessRatio | type == "number" and . > 0 and . <= 1)
      and (.workload.objectives.normalP99Millis | type == "number" and . > 0)
      and (.workload.objectives.slowP99Millis | type == "number" and . > 0)
      and (.workload.objectives.failureP99Millis | type == "number" and . > 0)
      and (.workload.failureModel | type == "array"
          and (index("slow") != null) and (index("refusal") != null)
          and (index("drain") != null) and (index("recovery") != null))
      and (.workload.topology.proxyReplicas | type == "number" and . > 0 and floor == .)
      and (.workload.topology.zones | type == "number" and . > 0 and floor == .)
      and (.workload.topology.ingress | type == "string" and length > 0)
      and (.workload.topology.resourceClass | type == "string" and length > 0)
      and (.workload.topology.observability | type == "string" and length > 0)
      and (.capacity.ratesPerSecond | type == "array" and length >= 2)
      and (all(.capacity.ratesPerSecond[]; type == "number" and . > 0 and floor == .))
      and (.capacity.ratesPerSecond == (.capacity.ratesPerSecond | sort | unique))
      and (.capacity.repeatsPerStep | type == "number" and . >= 3 and floor == .)
      and (.capacity.warmupSeconds | type == "number" and . >= 10 and floor == .)
      and (.capacity.measurementSeconds | type == "number" and . >= 30 and floor == .)
      and (.capacity.measurementSeconds >= .workload.requestRate.burstDurationSeconds)
      and (.capacity.cooldownSeconds | type == "number" and . >= 5 and floor == .)
      and (.capacity.sampleIntervalSeconds | type == "number" and . > 0 and floor == .)
      and (.capacity.sampleIntervalSeconds <= .capacity.measurementSeconds)
      and (.capacity.slowDelayMillis | type == "number" and . > 0 and . <= 10000 and floor == .)
      and (.capacity.minimumThroughputRatio | type == "number" and . > 0 and . <= 1)
      and (.capacity.failureCaseMinimumSuccessRatio | type == "number" and . > 0 and . <= 1)
      and (.capacity.maximumNonInjectedRetryRatio | type == "number" and . >= 0 and . <= 1)
      and (.capacity.headroomPercent | type == "number" and . > 0 and . < 100 and floor == .)
      and (.capacity.heapGrowthBudgetBytes | type == "number" and . > 0 and floor == .)
      and (.capacity.maxInFlight | type == "number" and . > 0 and floor == .)
      and (.capacity.maxInFlight <=
          (.workload.upstreams.count * .workload.upstreams.connectionLimitPerUpstream))
    ' "$profile" >/dev/null || {
        echo "Capacity profile does not satisfy the executable workload contract" >&2
        exit 2
    }

    local peak growth burst headroom grown_peak target maximum
    peak="$(jq -r '.workload.requestRate.peakPerSecond' "$profile")"
    growth="$(jq -r '.workload.requestRate.expectedGrowthPercent' "$profile")"
    burst="$(jq -r '.workload.requestRate.burstPerSecond' "$profile")"
    headroom="$(jq -r '.capacity.headroomPercent' "$profile")"
    grown_peak=$(( (peak * (100 + growth) + 99) / 100 ))
    target=$(( (grown_peak * (100 + headroom) + 99) / 100 ))
    (( target >= burst )) || target="$burst"
    maximum="$(jq -r '.capacity.ratesPerSecond | max' "$profile")"
    (( maximum >= target )) || {
        echo "Rate ladder must include forecast growth, configured headroom, and burst load (${target}/s)" >&2
        exit 2
    }
}

validate_profile
scenario_names=(equal slow failing draining recovering)
if [[ "$mode" == "validate" ]]; then
    jq -c --arg baseUrl 'https://127.0.0.1:18445' --arg apiKey validation-only \
        -f "$target_renderer" "$profile" \
        | jq -s -e --argjson expectsBody "$(jq '.workload.payload.requestBytes.p99 > 0' "$profile")" '
          length == 100 and all(.[];
            (.url | startswith("https://127.0.0.1:18445/proxy/"))
            and (.header["X-API-Key"] == ["validation-only"]))
            and (($expectsBody | not) or any(.[]; has("body")))' >/dev/null
    printf 'Validated capacity profile %s.\n' "$(jq -r '.profileId' "$profile")"
    printf 'Validated loopback-only capacity cases: %s\n' "${scenario_names[*]}"
    printf 'Execution remains disabled until review.status is reviewed and approval fields are populated.\n'
    exit 0
fi

jq -e '
  .review.status == "reviewed"
  and (.review.approvedBy | type == "string" and length > 0)
  and (.review.approvedAt | fromdateiso8601 > 0)
' "$profile" >/dev/null || {
    echo "Run mode requires a reviewed profile with approvedBy and approvedAt" >&2
    exit 2
}
jq -e '
  .workload.concurrency.http2Streams == 0
  and .workload.concurrency.webSockets == 0
  and (.workload.payload.streamingResponses | not)
  and (.workload.payload.serverSentEvents | not)
' "$profile" >/dev/null || {
    echo "This HTTP request-response staircase cannot qualify HTTP/2 streams, WebSockets, streaming responses, or SSE" >&2
    exit 2
}
jq -e '
  .workload.topology.proxyReplicas == 1
  and .workload.topology.zones == 1
  and .workload.topology.ingress == "127.0.0.1 only"
' "$profile" >/dev/null || {
    echo "This runner qualifies only the declared single-process loopback topology; use the topology phase for replicas, zones, or external ingress" >&2
    exit 2
}

for command_name in awk curl docker find jq openssl sha256sum vegeta; do
    command -v "$command_name" >/dev/null 2>&1 || {
        echo "$command_name is required" >&2
        exit 2
    }
done
docker compose version >/dev/null
git_revision="$(git -C "$repo_root" rev-parse HEAD 2>/dev/null || echo unknown)"
git_dirty=false
[[ -z "$(git -C "$repo_root" status --porcelain 2>/dev/null)" ]] || git_dirty=true
if [[ "$git_dirty" == "true" ]]; then
    echo "Run mode requires a clean checkout so evidence maps to an exact source revision" >&2
    exit 2
fi

profile_id="$(jq -r '.profileId' "$profile")"
mapfile -t rates < <(jq -r '.capacity.ratesPerSecond[]' "$profile")
repeats="$(jq -r '.capacity.repeatsPerStep' "$profile")"
warmup_seconds="$(jq -r '.capacity.warmupSeconds' "$profile")"
measurement_seconds="$(jq -r '.capacity.measurementSeconds' "$profile")"
cooldown_seconds="$(jq -r '.capacity.cooldownSeconds' "$profile")"
sample_interval_seconds="$(jq -r '.capacity.sampleIntervalSeconds' "$profile")"
slow_delay_ms="$(jq -r '.capacity.slowDelayMillis' "$profile")"
minimum_throughput_ratio="$(jq -r '.capacity.minimumThroughputRatio' "$profile")"
minimum_success_ratio="$(jq -r '.workload.objectives.minimumSuccessRatio' "$profile")"
failure_minimum_success_ratio="$(jq -r '.capacity.failureCaseMinimumSuccessRatio' "$profile")"
maximum_retry_ratio="$(jq -r '.capacity.maximumNonInjectedRetryRatio' "$profile")"
normal_p99_ms="$(jq -r '.workload.objectives.normalP99Millis' "$profile")"
slow_p99_ms="$(jq -r '.workload.objectives.slowP99Millis' "$profile")"
failure_p99_ms="$(jq -r '.workload.objectives.failureP99Millis' "$profile")"
headroom_percent="$(jq -r '.capacity.headroomPercent' "$profile")"
heap_growth_budget_bytes="$(jq -r '.capacity.heapGrowthBudgetBytes' "$profile")"
max_inflight="$(jq -r '.capacity.maxInFlight' "$profile")"
forecast_peak="$(jq -r '.workload.requestRate.peakPerSecond' "$profile")"
forecast_growth_percent="$(jq -r '.workload.requestRate.expectedGrowthPercent' "$profile")"
normal_rate="$(jq -r '.workload.requestRate.normalPerSecond' "$profile")"
burst_rate="$(jq -r '.workload.requestRate.burstPerSecond' "$profile")"
burst_duration_seconds="$(jq -r '.workload.requestRate.burstDurationSeconds' "$profile")"
routing_strategy="$(jq -r '.workload.routeMix[0].strategy' "$profile")"
client_connections="$(jq -r '.workload.concurrency.clientConnections' "$profile")"
keep_alive="$(jq -r '.workload.concurrency.keepAlive' "$profile")"
connection_churn="$(jq -r '.workload.concurrency.connectionChurn' "$profile")"
connection_limit_per_upstream="$(jq -r '.workload.upstreams.connectionLimitPerUpstream' "$profile")"
request_p95_bytes="$(jq -r '.workload.payload.requestBytes.p95' "$profile")"
response_p95_bytes="$(jq -r '.workload.payload.responseBytes.p95' "$profile")"

proxy_port="${LBP_CAPACITY_PORT:-18445}"
[[ "$proxy_port" =~ ^[1-9][0-9]*$ ]] && (( proxy_port >= 1024 && proxy_port <= 65535 )) || {
    echo "LBP_CAPACITY_PORT must be between 1024 and 65535" >&2
    exit 2
}
project_name="${LBP_CAPACITY_PROJECT:-lbp-capacity-${GITHUB_RUN_ID:-local-${BASHPID}}-${GITHUB_RUN_ATTEMPT:-1}}"
[[ "$project_name" =~ ^[a-z0-9][a-z0-9_-]{0,62}$ ]] || {
    echo "LBP_CAPACITY_PROJECT must contain only lower-case letters, digits, underscores, or hyphens" >&2
    exit 2
}

default_run_id="${GITHUB_RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)-${BASHPID}}-${GITHUB_RUN_ATTEMPT:-1}"
output_dir="${LBP_CAPACITY_OUTPUT_DIR:-$repo_root/target/capacity/$profile_id/$default_run_id}"
mkdir -p "$repo_root/target"
mkdir -p "$output_dir"
resolved_target_dir="$(cd "$repo_root/target" && pwd -P)"
resolved_output_dir="$(cd "$output_dir" && pwd -P)"
case "$resolved_output_dir/" in
    "$resolved_target_dir"/*) ;;
    *) echo "LBP_CAPACITY_OUTPUT_DIR must remain beneath the repository target directory" >&2; exit 2 ;;
esac
output_dir="$resolved_output_dir"
if [[ -n "$(find "$output_dir" -mindepth 1 -print -quit)" ]]; then
    echo "Capacity output directory must be empty to prevent mixed evidence" >&2
    exit 2
fi
measurements_file="$output_dir/measurements.jsonl"
: > "$measurements_file"

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-proxy-capacity.XXXXXX")"
api_key_file="$work_dir/loadbalancerpro-api-key"
tls_dir="$work_dir/tls"
trust_dir="$work_dir/trust"
identity_dir="$work_dir/identity"
config_dir="$work_dir/config"
reload_payload="$work_dir/reload.json"
compose=()
attack_pid=""
sampler_pid=""
sampler_stop_file=""

cleanup() {
    local status=$?
    trap - EXIT
    if [[ -n "$attack_pid" ]]; then
        kill "$attack_pid" >/dev/null 2>&1 || true
        wait "$attack_pid" >/dev/null 2>&1 || true
    fi
    if [[ -n "$sampler_pid" ]]; then
        [[ -n "$sampler_stop_file" ]] && : > "$sampler_stop_file"
        kill "$sampler_pid" >/dev/null 2>&1 || true
        wait "$sampler_pid" >/dev/null 2>&1 || true
    fi
    if [[ ${#compose[@]} -gt 0 ]]; then
        "${compose[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true
    fi
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-proxy-capacity.*)
            chmod -R u+w -- "$work_dir" >/dev/null 2>&1 || true
            rm -rf -- "$work_dir"
            ;;
        *) echo "Refusing to remove unexpected temporary path: $work_dir" >&2 ;;
    esac
    exit "$status"
}
trap cleanup EXIT

mkdir -p "$tls_dir" "$trust_dir" "$identity_dir" "$config_dir"
openssl rand -hex 24 > "$api_key_file"
chmod 0444 "$api_key_file"
api_key="$(<"$api_key_file")"
tls_hostname="lbp.local"
ca_private_key="$work_dir/ca-private-key.pem"
server_csr="$work_dir/server.csr"
server_extensions="$work_dir/server-extensions.cnf"
openssl req -x509 -newkey rsa:2048 -sha256 -days 1 -nodes \
    -subj "/CN=LoadBalancerPro Local Capacity CA" \
    -addext "basicConstraints=critical,CA:TRUE" \
    -addext "keyUsage=critical,keyCertSign,cRLSign" \
    -keyout "$ca_private_key" -out "$tls_dir/ca.pem" >/dev/null 2>&1
openssl req -newkey rsa:2048 -sha256 -nodes \
    -subj "/CN=$tls_hostname" \
    -keyout "$tls_dir/private-key.pem" -out "$server_csr" >/dev/null 2>&1
printf '%s\n' \
    "subjectAltName=DNS:$tls_hostname,IP:127.0.0.1" \
    "basicConstraints=critical,CA:FALSE" \
    "keyUsage=critical,digitalSignature,keyEncipherment" \
    "extendedKeyUsage=serverAuth" > "$server_extensions"
openssl x509 -req -sha256 -days 1 -in "$server_csr" \
    -CA "$tls_dir/ca.pem" -CAkey "$ca_private_key" -set_serial 1 \
    -extfile "$server_extensions" -out "$tls_dir/certificate.pem" >/dev/null 2>&1
chmod 0444 "$tls_dir/private-key.pem" "$tls_dir/certificate.pem" "$tls_dir/ca.pem"
chmod 0555 "$tls_dir" "$trust_dir" "$identity_dir" "$config_dir"

export LBP_API_KEY_FILE="$api_key_file"
export LBP_TLS_DIRECTORY="$tls_dir"
export LBP_TRUST_DIRECTORY="$trust_dir"
export LBP_IDENTITY_DIRECTORY="$identity_dir"
export LBP_CONFIG_DIRECTORY="$config_dir"
export LBP_TLS_HOSTNAME="$tls_hostname"
export LBP_PROXY_PROD_PORT="$proxy_port"
export LBP_PROXY_STRATEGY="$routing_strategy"
export LBP_PROXY_PROD_IMAGE="${LBP_PROXY_PROD_IMAGE:-loadbalancerpro:${project_name}}"
export LBP_PROXY_PROD_FIXTURE_IMAGE="${LBP_PROXY_PROD_FIXTURE_IMAGE:-loadbalancerpro:${project_name}-fixture}"

compose=(docker compose -p "$project_name" -f "$compose_base" -f "$compose_override")
"${compose[@]}" config --quiet
# Build the proxy from this clean revision. The fixture services share one tag, so build it once.
"${compose[@]}" build loadbalancerpro backend-a

base_url="https://127.0.0.1:$proxy_port"
curl_tls=(--silent --show-error --cacert "$tls_dir/ca.pem" --connect-timeout 3 --max-time 10)

authenticated_curl() {
    curl "${curl_tls[@]}" --header "X-API-Key: $api_key" "$@"
}

fetch_metrics() {
    authenticated_curl --fail "$base_url/actuator/prometheus"
}

write_mixed_targets() {
    local destination="$1"
    jq -c --arg baseUrl "$base_url" --arg apiKey "$api_key" \
        -f "$target_renderer" "$profile" > "$destination"
}

write_single_target() {
    local destination="$1"
    local path="$2"
    local separator='?'
    [[ "$path" == *'?'* ]] && separator='&'
    jq -cn \
      --arg url "$base_url$path${separator}lbpResponseBytes=$response_p95_bytes" \
      --arg apiKey "$api_key" --argjson requestBytes "$request_p95_bytes" '
      {method:"GET",url:$url,header:{"X-API-Key":[$apiKey]}}
      + (if $requestBytes > 0
         then {body:(("x" * $requestBytes) | @base64)}
         else {} end)
    ' > "$destination"
}

wait_for_stack() {
    local attempt
    for attempt in $(seq 1 120); do
        if authenticated_curl --fail --output /dev/null "$base_url/actuator/health"; then
            return
        fi
        sleep 1
    done
    "${compose[@]}" ps
    "${compose[@]}" logs loadbalancerpro
    echo "Capacity stack did not become healthy" >&2
    exit 1
}

wait_for_healthy_count() {
    local expected="$1"
    local status_file="$work_dir/health-count-status.json"
    local attempt
    for attempt in $(seq 1 30); do
        authenticated_curl --fail --output "$status_file" "$base_url/api/proxy/status"
        if jq -e --argjson expected "$expected" \
            '.observability.effectiveHealthyBackendCount == $expected' "$status_file" >/dev/null 2>&1; then
            return
        fi
        sleep 1
    done
    echo "Expected $expected healthy fixture backends" >&2
    exit 1
}

wait_for_inflight_zero() {
    local attempt value
    for attempt in $(seq 1 30); do
        value="$(fetch_metrics | awk '$1 ~ /^lbp_proxy_inflight(\{|$)/ { total += $NF } END { printf "%.0f", total + 0 }')"
        [[ "$value" == "0" ]] && return 0
        sleep 1
    done
    return 1
}

reload_baseline() {
    local response_file="$work_dir/reload-response.json"
    authenticated_curl --fail --header 'Content-Type: application/json' \
        --data-binary "@$reload_payload" --output "$response_file" "$base_url/api/proxy/reload"
    jq -e '.success == true and .status == "success"' "$response_file" >/dev/null
    wait_for_healthy_count 2
}

jq -n --arg routing_strategy "$routing_strategy" --argjson max_inflight "$max_inflight" \
  --argjson connection_limit_per_upstream "$connection_limit_per_upstream" '{
  enabled: true,
  strategy: $routing_strategy,
  connectTimeout: "PT1S",
  requestTimeout: "PT5S",
  maxRequestBytes: 65536,
  maxResponseBytes: 0,
  healthCheck: {enabled: true, path: "/health", timeout: "PT1S", interval: "PT1S", healthyThreshold: 1, unhealthyThreshold: 2},
  retry: {enabled: true, maxAttempts: 2, budgetPercent: 100, backoff: {base: "PT0.01S", max: "PT0.05S"}, retryNonIdempotent: false, methods: ["GET", "HEAD"], retryStatuses: [502, 503, 504]},
  reload: {drainTimeout: "PT10S"},
  cooldown: {enabled: true, consecutiveFailureThreshold: 2, duration: "PT1S", recoverOnSuccessfulHealthCheck: true},
  slowStart: {duration: "PT0S"},
  forwarded: {mode: "strip-and-set", trustedProxies: []},
  limits: {maxInFlight: $max_inflight, adaptive: false},
  shedding: {enabled: false},
  accessLog: {enabled: false, format: "JSON", path: "/tmp/loadbalancerpro/proxy-access.log", sampleRate: 1.0},
  upstreams: [
    {id: "backend-a", url: "http://backend-a:8080", healthy: true, maxInFlight: $connection_limit_per_upstream, weight: 1.0},
    {id: "backend-b", url: "http://backend-b:8080", healthy: true, maxInFlight: $connection_limit_per_upstream, weight: 1.0}
  ]
}' > "$reload_payload"

metric_sum() {
    local file="$1"
    local metric="$2"
    local required_label="${3:-}"
    awk -v metric="$metric" -v required_label="$required_label" '
      ($1 == metric || index($1, metric "{") == 1) &&
      (required_label == "" || index($1, required_label) > 0) { total += $NF }
      END { print total + 0 }
    ' "$file"
}

metric_delta() {
    local before after
    before="$(metric_sum "$1" "$3" "${4:-}")"
    after="$(metric_sum "$2" "$3" "${4:-}")"
    awk -v before="$before" -v after="$after" 'BEGIN { printf "%.9f", after - before }'
}

json_number() {
    local value="$1"
    if [[ "$value" =~ ^-?[0-9]+([.][0-9]+)?([eE][+-]?[0-9]+)?$ ]]; then
        printf '%s' "$value"
    else
        printf '0'
    fi
}

read_container_memory_bytes() {
    docker exec "$1" sh -c '
      if [ -r /sys/fs/cgroup/memory.current ]; then
        cat /sys/fs/cgroup/memory.current
      elif [ -r /sys/fs/cgroup/memory/memory.usage_in_bytes ]; then
        cat /sys/fs/cgroup/memory/memory.usage_in_bytes
      else
        echo 0
      fi
    ' 2>/dev/null || echo 0
}

read_open_connections() {
    docker exec "$1" sh -c \
      'awk '\''NR > 1 && $4 == "01" { count++ } END { print count + 0 }'\'' /proc/1/net/tcp /proc/1/net/tcp6 2>/dev/null' \
      2>/dev/null || echo 0
}

read_process_threads() {
    docker exec "$1" sh -c 'ls -1 /proc/1/task 2>/dev/null | wc -l' 2>/dev/null || echo 0
}

read_process_memory_bytes() {
    docker exec "$1" sh -c \
      'awk '\''$1 == "VmRSS:" { print $2 * 1024; found=1; exit } END { if (!found) print 0 }'\'' /proc/1/status' \
      2>/dev/null || echo 0
}

sample_resources() {
    local destination="$1"
    local stop_file="$2"
    local container_id="$3"
    local sample_metrics="$work_dir/resource-sample.prom"
    : > "$destination"
    while [[ ! -f "$stop_file" ]]; do
        local docker_stats heap cpu_usage process_memory gc_count gc_sum inflight jvm_threads
        local container_memory open_connections process_threads
        docker_stats="$(docker stats --no-stream --format '{{json .}}' "$container_id" 2>/dev/null || echo '{}')"
        fetch_metrics > "$sample_metrics"
        heap="$(json_number "$(metric_sum "$sample_metrics" jvm_memory_used_bytes 'area="heap"')")"
        cpu_usage="$(json_number "$(metric_sum "$sample_metrics" process_cpu_usage)")"
        process_memory="$(json_number "$(read_process_memory_bytes "$container_id")")"
        gc_count="$(json_number "$(metric_sum "$sample_metrics" jvm_gc_pause_seconds_count)")"
        gc_sum="$(json_number "$(metric_sum "$sample_metrics" jvm_gc_pause_seconds_sum)")"
        inflight="$(json_number "$(metric_sum "$sample_metrics" lbp_proxy_inflight)")"
        jvm_threads="$(json_number "$(metric_sum "$sample_metrics" jvm_threads_live_threads)")"
        container_memory="$(json_number "$(read_container_memory_bytes "$container_id")")"
        open_connections="$(json_number "$(read_open_connections "$container_id")")"
        process_threads="$(json_number "$(read_process_threads "$container_id")")"
        jq -cn \
          --argjson epochSeconds "$(date +%s)" \
          --argjson heapUsedBytes "$heap" \
          --argjson processCpuUsage "$cpu_usage" \
          --argjson processMemoryBytes "$process_memory" \
          --argjson gcPauseCount "$gc_count" \
          --argjson gcPauseSeconds "$gc_sum" \
          --argjson inflight "$inflight" \
          --argjson jvmLiveThreads "$jvm_threads" \
          --argjson containerMemoryBytes "$container_memory" \
          --argjson openConnections "$open_connections" \
          --argjson processThreads "$process_threads" \
          --argjson docker "$docker_stats" \
          '{epochSeconds:$epochSeconds,heapUsedBytes:$heapUsedBytes,processCpuUsage:$processCpuUsage,
            processMemoryBytes:$processMemoryBytes,gcPauseCount:$gcPauseCount,
            gcPauseSeconds:$gcPauseSeconds,inflight:$inflight,jvmLiveThreads:$jvmLiveThreads,
            containerMemoryBytes:$containerMemoryBytes,openConnections:$openConnections,
            processThreads:$processThreads,docker:$docker}' >> "$destination"
        local waited
        for (( waited = 0; waited < sample_interval_seconds; waited++ )); do
            [[ -f "$stop_file" ]] && break
            sleep 1
        done
    done
}

action_stop_backend_a() {
    "${compose[@]}" stop --timeout 2 backend-a
}

action_start_backend_a() {
    "${compose[@]}" start backend-a
}

after_failure() {
    action_start_backend_a
    wait_for_healthy_count 2
}

action_drain_backend_a() {
    local config_file="$work_dir/admin-config.json"
    local response_file="$work_dir/drain-response.json"
    local generation
    authenticated_curl --fail --output "$config_file" "$base_url/api/proxy/config"
    generation="$(jq -r '.generation' "$config_file")"
    authenticated_curl --fail --request DELETE --output "$response_file" \
        "$base_url/api/proxy/upstreams/backend-a?expectedGeneration=$generation"
    jq -e '.success == true and .action == "delete"' "$response_file" >/dev/null
}

after_drain() {
    local config_file="$work_dir/drain-config.json"
    local attempt
    for attempt in $(seq 1 30); do
        authenticated_curl --fail --output "$config_file" "$base_url/api/proxy/config"
        if jq -e '.drainingUpstreamIds | length == 0' "$config_file" >/dev/null; then
            reload_baseline
            return
        fi
        sleep 1
    done
    echo "Drained upstream did not converge" >&2
    exit 1
}

after_recovery() {
    wait_for_healthy_count 2
}

run_warmup() {
    local rate="$1"
    local repeat="$2"
    local run_dir="$3"
    local targets_file="$work_dir/warmup.targets"
    local results_file="$run_dir/warmup.bin"
    write_mixed_targets "$targets_file"
    vegeta attack -duration="${warmup_seconds}s" -rate="${rate}/s" -timeout=6s \
        -connections="$client_connections" -max-connections="$client_connections" -keepalive="$keep_alive" \
        -root-certs="$tls_dir/ca.pem" -format=json -targets="$targets_file" > "$results_file"
    vegeta report -type=json "$results_file" > "$run_dir/warmup.json"
    jq -n --argjson rate "$rate" --argjson repeat "$repeat" \
        --argjson durationSeconds "$warmup_seconds" \
        '{rate:$rate,repeat:$repeat,durationSeconds:$durationSeconds}' > "$run_dir/warmup-metadata.json"
    wait_for_inflight_zero || { echo "Warm-up in-flight work did not quiesce" >&2; exit 1; }
}

run_scenario() {
    local scenario="$1"
    local path="$2"
    local rate="$3"
    local repeat="$4"
    local p99_budget_ms="$5"
    local injected="$6"
    local during_action="${7:-}"
    local after_action="${8:-}"
    local scenario_dir="$9"
    local targets_file="$work_dir/${scenario}.targets"
    local results_file="$scenario_dir/client.bin"
    local report_file="$scenario_dir/client.json"
    local resources_file="$scenario_dir/resources.jsonl"
    local stop_file="$scenario_dir/stop-sampler"
    local container_id before_metrics after_metrics quiesced
    mkdir -p "$scenario_dir"
    container_id="$("${compose[@]}" ps -q loadbalancerpro)"
    before_metrics="$scenario_dir/metrics-before.prom"
    after_metrics="$scenario_dir/metrics-after.prom"
    authenticated_curl --fail --output "$scenario_dir/status-before.json" "$base_url/api/proxy/status"
    fetch_metrics > "$before_metrics"
    rm -f -- "$stop_file"
    sampler_stop_file="$stop_file"
    sample_resources "$resources_file" "$stop_file" "$container_id" &
    sampler_pid=$!
    if [[ "$scenario" == "equal" ]]; then
        write_mixed_targets "$targets_file"
    else
        write_single_target "$targets_file" "$path"
    fi
    vegeta attack -duration="${measurement_seconds}s" -rate="${rate}/s" -timeout=6s \
        -connections="$client_connections" -max-connections="$client_connections" -keepalive="$keep_alive" \
        -root-certs="$tls_dir/ca.pem" -format=json -targets="$targets_file" > "$results_file" &
    attack_pid=$!
    if [[ -n "$during_action" ]]; then
        sleep $(( measurement_seconds / 3 > 0 ? measurement_seconds / 3 : 1 ))
        "$during_action"
    fi
    if ! wait "$attack_pid"; then
        attack_pid=""
        echo "$scenario Vegeta attack failed" >&2
        exit 1
    fi
    attack_pid=""
    [[ -z "$after_action" ]] || "$after_action"
    vegeta report -type=json "$results_file" > "$report_file"
    vegeta report -type=text "$results_file" > "$scenario_dir/client.txt"
    quiesced=true
    wait_for_inflight_zero || quiesced=false
    : > "$stop_file"
    if ! wait "$sampler_pid"; then
        sampler_pid=""
        echo "$scenario resource sampler failed" >&2
        exit 1
    fi
    sampler_pid=""
    sampler_stop_file=""
    fetch_metrics > "$after_metrics"
    authenticated_curl --fail --output "$scenario_dir/status-after.json" "$base_url/api/proxy/status"

    local requests throughput success p50_ms p95_ms p99_ms throughput_ratio completion_ratio
    local retries_delta sheds_delta limit_delta attempts_delta requests_delta retry_ratio
    local backend_a_delta backend_b_delta gc_pause_count_delta gc_pause_seconds_delta
    local resource_summary heap_growth max_observed_inflight case_pass saturated scenario_minimum_success_ratio
    requests="$(jq -r '.requests' "$report_file")"
    throughput="$(jq -r '.throughput' "$report_file")"
    success="$(jq -r '.success' "$report_file")"
    p50_ms="$(jq -r '.latencies["50th"] / 1000000' "$report_file")"
    p95_ms="$(jq -r '.latencies["95th"] / 1000000' "$report_file")"
    p99_ms="$(jq -r '.latencies["99th"] / 1000000' "$report_file")"
    throughput_ratio="$(awk -v actual="$throughput" -v offered="$rate" 'BEGIN { printf "%.9f", actual / offered }')"
    completion_ratio="$(awk -v requests="$requests" -v rate="$rate" -v seconds="$measurement_seconds" \
        'BEGIN { printf "%.9f", requests / (rate * seconds) }')"
    retries_delta="$(metric_delta "$before_metrics" "$after_metrics" lbp_proxy_retries_total)"
    sheds_delta="$(metric_delta "$before_metrics" "$after_metrics" lbp_proxy_sheds_total)"
    limit_delta="$(metric_delta "$before_metrics" "$after_metrics" lbp_proxy_limit_rejections_total)"
    attempts_delta="$(metric_delta "$before_metrics" "$after_metrics" lbp_proxy_attempts_total)"
    requests_delta="$(metric_delta "$before_metrics" "$after_metrics" lbp_proxy_requests_total)"
    retry_ratio="$(awk -v retries="$retries_delta" -v requests="$requests" \
        'BEGIN { printf "%.9f", requests > 0 ? retries / requests : 1 }')"
    backend_a_delta="$(metric_delta "$before_metrics" "$after_metrics" lbp_proxy_requests_total 'upstream="backend-a"')"
    backend_b_delta="$(metric_delta "$before_metrics" "$after_metrics" lbp_proxy_requests_total 'upstream="backend-b"')"
    gc_pause_count_delta="$(metric_delta "$before_metrics" "$after_metrics" jvm_gc_pause_seconds_count)"
    gc_pause_seconds_delta="$(metric_delta "$before_metrics" "$after_metrics" jvm_gc_pause_seconds_sum)"
    resource_summary="$(jq -s '{
      samples:length,
      heapStartBytes:(.[0].heapUsedBytes // 0),
      heapEndBytes:(.[-1].heapUsedBytes // 0),
      maxHeapUsedBytes:([.[].heapUsedBytes] | max // 0),
      maxProcessCpuUsage:([.[].processCpuUsage] | max // 0),
      maxProcessMemoryBytes:([.[].processMemoryBytes] | max // 0),
      maxContainerMemoryBytes:([.[].containerMemoryBytes] | max // 0),
      maxInflight:([.[].inflight] | max // 0),
      maxOpenConnections:([.[].openConnections] | max // 0),
      maxJvmLiveThreads:([.[].jvmLiveThreads] | max // 0),
      maxProcessThreads:([.[].processThreads] | max // 0),
      dockerSamples:[.[].docker]
    }' "$resources_file")"
    heap_growth="$(jq -r '.maxHeapUsedBytes - .heapStartBytes' <<<"$resource_summary")"
    max_observed_inflight="$(jq -r '.maxInflight' <<<"$resource_summary")"
    scenario_minimum_success_ratio="$minimum_success_ratio"
    [[ "$injected" == "false" ]] || scenario_minimum_success_ratio="$failure_minimum_success_ratio"

    case_pass="$(jq -nr \
      --argjson injected "$injected" --argjson throughputRatio "$throughput_ratio" \
      --argjson completionRatio "$completion_ratio" \
      --argjson minimumThroughputRatio "$minimum_throughput_ratio" \
      --argjson success "$success" --argjson minimumSuccessRatio "$scenario_minimum_success_ratio" \
      --argjson p99Millis "$p99_ms" --argjson p99BudgetMillis "$p99_budget_ms" \
      --argjson retryRatio "$retry_ratio" --argjson maximumRetryRatio "$maximum_retry_ratio" \
      --argjson sheds "$sheds_delta" --argjson limits "$limit_delta" \
      --argjson quiesced "$quiesced" --argjson heapGrowthBytes "$heap_growth" \
      --argjson heapBudgetBytes "$heap_growth_budget_bytes" \
      --argjson maxObservedInflight "$max_observed_inflight" --argjson maxInFlight "$max_inflight" '
      ((if $injected then $completionRatio else $throughputRatio end) >= $minimumThroughputRatio)
      and ($success >= $minimumSuccessRatio)
      and ($p99Millis <= $p99BudgetMillis)
      and ($injected or $retryRatio <= $maximumRetryRatio)
      and ($sheds == 0) and ($limits == 0) and $quiesced
      and ($heapGrowthBytes <= $heapBudgetBytes)
      and ($maxObservedInflight < $maxInFlight)
    ')"
    saturated=false
    if [[ "$injected" == "false" && "$case_pass" != "true" ]]; then
        saturated=true
    fi

    jq -n \
      --arg scenario "$scenario" --argjson rate "$rate" --argjson repeat "$repeat" \
      --argjson injected "$injected" --argjson casePassed "$case_pass" \
      --argjson saturated "$saturated" --argjson offeredThroughput "$rate" \
      --argjson achievedThroughput "$throughput" --argjson throughputRatio "$throughput_ratio" \
      --argjson completionRatio "$completion_ratio" \
      --argjson requests "$requests" --argjson successRatio "$success" \
      --argjson p50Millis "$p50_ms" --argjson p95Millis "$p95_ms" \
      --argjson p99Millis "$p99_ms" --argjson p99BudgetMillis "$p99_budget_ms" \
      --argjson statusCodes "$(jq '.status_codes' "$report_file")" \
      --argjson errors "$(jq '.errors' "$report_file")" \
      --argjson attemptsDelta "$attempts_delta" --argjson requestMetricDelta "$requests_delta" \
      --argjson retriesDelta "$retries_delta" --argjson retryRatio "$retry_ratio" \
      --argjson shedsDelta "$sheds_delta" --argjson limitRejectionsDelta "$limit_delta" \
      --argjson backendARequests "$backend_a_delta" --argjson backendBRequests "$backend_b_delta" \
      --argjson gcPauseCountDelta "$gc_pause_count_delta" \
      --argjson gcPauseSecondsDelta "$gc_pause_seconds_delta" \
      --argjson quiesced "$quiesced" --argjson heapGrowthBytes "$heap_growth" \
      --argjson resources "$resource_summary" \
      --argjson minimumThroughputRatio "$minimum_throughput_ratio" \
      --argjson minimumSuccessRatio "$scenario_minimum_success_ratio" \
      --argjson maximumRetryRatio "$maximum_retry_ratio" \
      --argjson heapBudgetBytes "$heap_growth_budget_bytes" --argjson maxInFlight "$max_inflight" '
      {
        scenario:$scenario,rate:$rate,repeat:$repeat,injectedFailure:$injected,
        casePassed:$casePassed,saturated:$saturated,
        client:{offeredThroughput:$offeredThroughput,achievedThroughput:$achievedThroughput,
          throughputRatio:$throughputRatio,completionRatio:$completionRatio,
          requests:$requests,successRatio:$successRatio,
          p50Millis:$p50Millis,p95Millis:$p95Millis,p99Millis:$p99Millis,
          p99BudgetMillis:$p99BudgetMillis,statusCodes:$statusCodes,errors:$errors},
        proxy:{attemptsDelta:$attemptsDelta,requestMetricDelta:$requestMetricDelta,
          retriesDelta:$retriesDelta,retryRatio:$retryRatio,shedsDelta:$shedsDelta,
          limitRejectionsDelta:$limitRejectionsDelta,quiesced:$quiesced},
        upstreamDistribution:{backendARequests:$backendARequests,backendBRequests:$backendBRequests},
        runtime:{gcPauseCountDelta:$gcPauseCountDelta,gcPauseSecondsDelta:$gcPauseSecondsDelta,
          heapGrowthBytes:$heapGrowthBytes,resources:$resources},
        thresholds:{minimumThroughputRatio:$minimumThroughputRatio,
          minimumSuccessRatio:$minimumSuccessRatio,maximumNonInjectedRetryRatio:$maximumRetryRatio,
          heapGrowthBudgetBytes:$heapBudgetBytes,maxInFlight:$maxInFlight},
        saturationSignals:[
          if (if $injected then $completionRatio else $throughputRatio end) < $minimumThroughputRatio
            then "throughput" else empty end,
          if $successRatio < $minimumSuccessRatio then "success" else empty end,
          if $p99Millis > $p99BudgetMillis then "p99" else empty end,
          if ($injected | not) and $retryRatio > $maximumRetryRatio then "retries" else empty end,
          if $shedsDelta > 0 then "sheds" else empty end,
          if $limitRejectionsDelta > 0 then "safety-limit" else empty end,
          if ($quiesced | not) then "inflight-not-quiesced" else empty end,
          if $heapGrowthBytes > $heapBudgetBytes then "heap-growth" else empty end,
          if $resources.maxInflight >= $maxInFlight then "max-inflight" else empty end
        ]
      }
    ' > "$scenario_dir/result.json"
    jq -c . "$scenario_dir/result.json" >> "$measurements_file"
}

fresh_proxy_id=""
fresh_proxy_process() {
    local run_dir="$1"
    local previous_id="$2"
    "${compose[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true
    "${compose[@]}" up --no-build --detach
    wait_for_stack
    local container_id
    container_id="$("${compose[@]}" ps -q loadbalancerpro)"
    [[ -n "$container_id" && "$container_id" != "$previous_id" ]] || {
        echo "Each capacity repeat requires a fresh proxy container" >&2
        exit 1
    }
    docker inspect "$container_id" > "$run_dir/proxy-container.json"
    reload_baseline
    fresh_proxy_id="$container_id"
}

profile_sha256="$(sha256sum "$profile" | awk '{print $1}')"
docker info --format '{{json .}}' > "$output_dir/docker-info.json"
docker image inspect "$LBP_PROXY_PROD_IMAGE" > "$output_dir/proxy-image.json"
docker image inspect "$LBP_PROXY_PROD_FIXTURE_IMAGE" > "$output_dir/fixture-image.json"
jq -n \
  --arg profileId "$profile_id" --arg profileSha256 "$profile_sha256" \
  --arg sourceRevision "$git_revision" --argjson sourceDirty "$git_dirty" \
  --arg routingStrategy "$routing_strategy" --arg target "$base_url" \
  --arg connectionChurn "$connection_churn" \
  --argjson clientConnections "$client_connections" --argjson keepAlive "$keep_alive" \
  --argjson connectionLimitPerUpstream "$connection_limit_per_upstream" \
  --argjson rates "$(jq '.capacity.ratesPerSecond' "$profile")" \
  --argjson repeatsPerStep "$repeats" --argjson normalRate "$normal_rate" \
  --argjson forecastPeak "$forecast_peak" --argjson forecastGrowthPercent "$forecast_growth_percent" \
  --argjson burstRate "$burst_rate" --argjson burstDurationSeconds "$burst_duration_seconds" \
  --argjson headroomPercent "$headroom_percent" \
  '{schemaVersion:1,profileId:$profileId,profileSha256:$profileSha256,
    sourceRevision:$sourceRevision,sourceDirty:$sourceDirty,routingStrategy:$routingStrategy,
    target:$target,targetBoundary:"fixed loopback TLS endpoint",ratesPerSecond:$rates,
    topologyBoundary:"single proxy process in one local zone",
    clientConnections:$clientConnections,keepAlive:$keepAlive,connectionChurn:$connectionChurn,
    connectionLimitPerUpstream:$connectionLimitPerUpstream,repeatsPerStep:$repeats,
    normalRatePerSecond:$normalRate,forecastPeakPerSecond:$forecastPeak,
    forecastGrowthPercent:$forecastGrowthPercent,burstRatePerSecond:$burstRate,
    burstDurationSeconds:$burstDurationSeconds,headroomPercent:$headroomPercent}' \
  > "$output_dir/run-metadata.json"

executed_rates=()
previous_proxy_id=""
stop_ladder=false
for rate in "${rates[@]}"; do
    executed_rates+=("$rate")
    rate_saturated_count=0
    for repeat in $(seq 1 "$repeats"); do
        run_dir="$output_dir/rate-${rate}/repeat-${repeat}"
        mkdir -p "$run_dir"
        fresh_proxy_process "$run_dir" "$previous_proxy_id"
        previous_proxy_id="$fresh_proxy_id"
        run_warmup "$rate" "$repeat" "$run_dir"

        reload_baseline
        run_scenario equal '/proxy/capacity/equal' "$rate" "$repeat" "$normal_p99_ms" false '' '' \
            "$run_dir/equal"

        reload_baseline
        run_scenario slow "/proxy/slow?millis=$slow_delay_ms&slowBackend=backend-a" \
            "$rate" "$repeat" "$slow_p99_ms" false '' '' "$run_dir/slow"

        reload_baseline
        run_scenario failing '/proxy/capacity/failing' "$rate" "$repeat" "$failure_p99_ms" true \
            action_stop_backend_a after_failure "$run_dir/failing"

        reload_baseline
        run_scenario draining '/proxy/capacity/draining' "$rate" "$repeat" "$normal_p99_ms" false \
            action_drain_backend_a after_drain "$run_dir/draining"

        reload_baseline
        action_stop_backend_a
        wait_for_healthy_count 1
        run_scenario recovering '/proxy/capacity/recovering' "$rate" "$repeat" "$normal_p99_ms" false \
            action_start_backend_a after_recovery "$run_dir/recovering"

        wait_for_inflight_zero || { echo "Cooldown started with in-flight work" >&2; exit 1; }
        sleep "$cooldown_seconds"
        jq -n --argjson seconds "$cooldown_seconds" --arg completedAt "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
            '{seconds:$seconds,completedAt:$completedAt,inflightQuiesced:true}' > "$run_dir/cooldown.json"

        repeat_saturated="$(jq -s --argjson rate "$rate" --argjson repeat "$repeat" \
            'any(.[]; .rate == $rate and .repeat == $repeat and .saturated)' "$measurements_file")"
        [[ "$repeat_saturated" == "true" ]] && rate_saturated_count=$(( rate_saturated_count + 1 ))
    done
    if (( rate_saturated_count > 0 )); then
        stop_ladder=true
    fi
    [[ "$stop_ladder" == "false" ]] || break
done

jq -s 'group_by([.rate,.repeat]) | map({
  rate:.[0].rate,
  repeat:.[0].repeat,
  saturated:any(.[]; .saturated),
  casesPassed:all(.[]; .casePassed),
  saturationSignals:([.[].saturationSignals[]] | unique)
})' "$measurements_file" > "$output_dir/repeat-summary.json"

rate_summary_jsonl="$work_dir/rate-summary.jsonl"
: > "$rate_summary_jsonl"
majority=$(( repeats / 2 + 1 ))
for rate in "${executed_rates[@]}"; do
    saturated_count="$(jq --argjson rate "$rate" '[.[] | select(.rate == $rate and .saturated)] | length' \
        "$output_dir/repeat-summary.json")"
    case_failure_count="$(jq --argjson rate "$rate" '[.[] | select(.rate == $rate and (.casesPassed | not))] | length' \
        "$output_dir/repeat-summary.json")"
    classification=pass
    if (( saturated_count >= majority )); then
        classification=saturation
    elif (( saturated_count > 0 )); then
        classification=unstable
    fi
    jq -n --argjson rate "$rate" --arg classification "$classification" \
      --argjson repeats "$repeats" --argjson saturatedRepeats "$saturated_count" \
      --argjson caseFailureRepeats "$case_failure_count" \
      '{rate:$rate,classification:$classification,repeats:$repeats,
        saturatedRepeats:$saturatedRepeats,caseFailureRepeats:$caseFailureRepeats}' >> "$rate_summary_jsonl"
done
jq -s . "$rate_summary_jsonl" > "$output_dir/rate-summary.json"

first_saturation="$(jq -r '[.[] | select(.classification == "saturation")][0].rate // 0' "$output_dir/rate-summary.json")"
unstable_steps="$(jq '[.[] | select(.classification == "unstable")] | length' "$output_dir/rate-summary.json")"
highest_passing="$(jq '[.[] | select(.classification == "pass") | .rate] | max // 0' "$output_dir/rate-summary.json")"
operating_envelope=0
if (( first_saturation > 0 )); then
    reserved_envelope=$(( first_saturation * (100 - headroom_percent) / 100 ))
    operating_envelope=$(( highest_passing < reserved_envelope ? highest_passing : reserved_envelope ))
fi
grown_peak=$(( (forecast_peak * (100 + forecast_growth_percent) + 99) / 100 ))
headroom_target=$(( (grown_peak * (100 + headroom_percent) + 99) / 100 ))
required_qualification_rate=$(( headroom_target > burst_rate ? headroom_target : burst_rate ))
qualification_rate="$(jq --argjson target "$required_qualification_rate" \
    '[.[] | select(.classification == "pass" and .rate >= $target) | .rate] | min // 0' \
    "$output_dir/rate-summary.json")"
qualification_case_failures=1
if (( qualification_rate > 0 )); then
    qualification_case_failures="$(jq -s --argjson rate "$qualification_rate" \
        '[.[] | select(.rate <= $rate and (.casePassed | not))] | length' "$measurements_file")"
fi
accepted=false
if (( first_saturation > 0 && unstable_steps == 0 && qualification_rate > 0 \
      && qualification_case_failures == 0 && operating_envelope >= required_qualification_rate )); then
    accepted=true
fi

jq -n \
  --argjson accepted "$accepted" --argjson firstSaturationRate "$first_saturation" \
  --argjson highestPassingRate "$highest_passing" --argjson operatingEnvelopeRate "$operating_envelope" \
  --argjson headroomPercent "$headroom_percent" --argjson forecastPeakRate "$forecast_peak" \
  --argjson forecastGrowthPercent "$forecast_growth_percent" --argjson grownPeakRate "$grown_peak" \
  --argjson forecastPeakPlusHeadroomRate "$headroom_target" \
  --argjson burstRate "$burst_rate" --argjson requiredQualificationRate "$required_qualification_rate" \
  --argjson qualificationRate "$qualification_rate" --argjson unstableSteps "$unstable_steps" \
  --argjson qualificationCaseFailures "$qualification_case_failures" '
  {
    accepted:$accepted,
    firstReproducibleSaturationRate:$firstSaturationRate,
    highestFullyPassingRate:$highestPassingRate,
    recommendedOperatingEnvelopeRate:$operatingEnvelopeRate,
    reservedHeadroomPercent:$headroomPercent,
    forecastPeakRate:$forecastPeakRate,
    forecastGrowthPercent:$forecastGrowthPercent,
    grownForecastPeakRate:$grownPeakRate,
    forecastPeakPlusHeadroomRate:$forecastPeakPlusHeadroomRate,
    burstRate:$burstRate,
    requiredQualificationRate:$requiredQualificationRate,
    passingQualificationStepAtOrAboveHeadroomTarget:$qualificationRate,
    unstableStepCount:$unstableSteps,
    qualificationCaseFailureCount:$qualificationCaseFailures,
    boundary:"Valid only for this exact artifact, profile, host, Compose limits, and loopback fixture run."
  }
' > "$output_dir/capacity-result.json"

"${compose[@]}" down --volumes --remove-orphans
printf 'Capacity staircase evidence: %s\n' "$output_dir"
jq . "$output_dir/capacity-result.json"
if [[ "$accepted" != "true" ]]; then
    echo "Capacity qualification did not establish a reproducible knee and forecast envelope" >&2
    exit 1
fi
printf 'Capacity qualification passed for the reviewed loopback profile only.\n'
