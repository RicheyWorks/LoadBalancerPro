#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
compose_base="$repo_root/deploy/docker-compose.proxy-prod.yml"
compose_override="$script_dir/docker-compose.bench.yml"
heap_analyzer="$script_dir/analyze-heap.awk"

mode="${LBP_BENCH_MODE:-smoke}"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --mode)
            [[ $# -ge 2 ]] || { echo "--mode requires smoke, soak, or validate" >&2; exit 2; }
            mode="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [--mode smoke|soak|validate]"
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            exit 2
            ;;
    esac
done

case "$mode" in
    smoke|soak|validate) ;;
    *) echo "Mode must be smoke, soak, or validate" >&2; exit 2 ;;
esac

require_positive_integer() {
    local name="$1"
    local value="$2"
    [[ "$value" =~ ^[1-9][0-9]*$ ]] || {
        echo "$name must be a positive integer" >&2
        exit 2
    }
}

scenario_seconds="${LBP_BENCH_SCENARIO_SECONDS:-8}"
soak_seconds="${LBP_BENCH_SOAK_SECONDS:-3600}"
steady_rate="${LBP_BENCH_RATE:-20}"
spike_rate="${LBP_BENCH_SPIKE_RATE:-60}"
heap_sample_seconds="${LBP_BENCH_HEAP_SAMPLE_SECONDS:-30}"
heap_growth_budget_bytes="${LBP_BENCH_HEAP_GROWTH_BUDGET_BYTES:-67108864}"
normal_p99_budget_ms="${LBP_BENCH_P99_BUDGET_MS:-1500}"
slow_p99_budget_ms="${LBP_BENCH_SLOW_P99_BUDGET_MS:-2500}"
failure_p99_budget_ms="${LBP_BENCH_FAILURE_P99_BUDGET_MS:-3000}"
proxy_port="${LBP_BENCH_PORT:-18444}"
project_name="${LBP_BENCH_PROJECT:-lbp-bench-${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}}"

for pair in \
    "LBP_BENCH_SCENARIO_SECONDS:$scenario_seconds" \
    "LBP_BENCH_SOAK_SECONDS:$soak_seconds" \
    "LBP_BENCH_RATE:$steady_rate" \
    "LBP_BENCH_SPIKE_RATE:$spike_rate" \
    "LBP_BENCH_HEAP_SAMPLE_SECONDS:$heap_sample_seconds" \
    "LBP_BENCH_HEAP_GROWTH_BUDGET_BYTES:$heap_growth_budget_bytes" \
    "LBP_BENCH_P99_BUDGET_MS:$normal_p99_budget_ms" \
    "LBP_BENCH_SLOW_P99_BUDGET_MS:$slow_p99_budget_ms" \
    "LBP_BENCH_FAILURE_P99_BUDGET_MS:$failure_p99_budget_ms" \
    "LBP_BENCH_PORT:$proxy_port"; do
    require_positive_integer "${pair%%:*}" "${pair#*:}"
done
(( proxy_port >= 1024 && proxy_port <= 65535 )) || {
    echo "LBP_BENCH_PORT must be between 1024 and 65535" >&2
    exit 2
}
[[ "$project_name" =~ ^[a-z0-9][a-z0-9_-]{0,62}$ ]] || {
    echo "LBP_BENCH_PROJECT must contain only lower-case letters, digits, underscores, or hyphens" >&2
    exit 2
}
if [[ "$mode" == "soak" && "$soak_seconds" -lt 3600 ]]; then
    echo "Soak mode requires at least 3600 seconds" >&2
    exit 2
fi

for required_file in "$compose_base" "$compose_override" "$heap_analyzer"; do
    [[ -f "$required_file" ]] || { echo "Missing required file: $required_file" >&2; exit 2; }
done

scenario_names=(steady spike slow-backend backend-kill reload-under-load drain-under-load)
if [[ "$mode" == "validate" ]]; then
    command -v awk >/dev/null 2>&1 || { echo "awk is required" >&2; exit 2; }
    {
        printf 'epoch_seconds,heap_used_bytes\n'
        for sample in $(seq 1 12); do
            printf '%s,%s\n' "$sample" "$(( 104857600 + sample ))"
        done
    } | awk -F, -v budget="$heap_growth_budget_bytes" -v output=/dev/null -f "$heap_analyzer"
    printf 'Validated loopback-only scenarios: %s\n' "${scenario_names[*]}"
    exit 0
fi

for command_name in awk curl docker jq openssl vegeta; do
    command -v "$command_name" >/dev/null 2>&1 || {
        echo "$command_name is required" >&2
        exit 2
    }
done
docker compose version >/dev/null

output_dir="${LBP_BENCH_OUTPUT_DIR:-$repo_root/target/bench/$mode}"
mkdir -p "$output_dir"
summary_file="$output_dir/scenario-summary.csv"
printf 'scenario,requests,p99_ms,p99_budget_ms,status_5xx,errors,success_ratio,zero_failure_required,max_failure_percent\n' > "$summary_file"

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-proxy-bench.XXXXXX")"
api_key_file="$work_dir/loadbalancerpro-api-key"
tls_dir="$work_dir/tls"
trust_dir="$work_dir/trust"
identity_dir="$work_dir/identity"
config_dir="$work_dir/config"
reload_payload="$work_dir/reload.json"
heap_sampler_pid=""
attack_pid=""
heap_stop_file="$work_dir/stop-heap-sampler"

cleanup() {
    local status=$?
    trap - EXIT
    if [[ -n "$attack_pid" ]]; then
        kill "$attack_pid" >/dev/null 2>&1 || true
        wait "$attack_pid" >/dev/null 2>&1 || true
    fi
    if [[ -n "$heap_sampler_pid" ]]; then
        kill "$heap_sampler_pid" >/dev/null 2>&1 || true
        wait "$heap_sampler_pid" >/dev/null 2>&1 || true
    fi
    docker compose -p "$project_name" -f "$compose_base" -f "$compose_override" \
        down --volumes --remove-orphans >/dev/null 2>&1 || true
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-proxy-bench.*)
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
openssl req -x509 -newkey rsa:2048 -sha256 -days 1 -nodes \
    -subj "/CN=$tls_hostname" \
    -addext "subjectAltName=DNS:$tls_hostname,IP:127.0.0.1" \
    -keyout "$tls_dir/private-key.pem" \
    -out "$tls_dir/certificate.pem" >/dev/null 2>&1
cp "$tls_dir/certificate.pem" "$tls_dir/ca.pem"
chmod 0444 "$tls_dir/private-key.pem" "$tls_dir/certificate.pem" "$tls_dir/ca.pem"
chmod 0555 "$tls_dir" "$trust_dir" "$identity_dir" "$config_dir"

export LBP_API_KEY_FILE="$api_key_file"
export LBP_TLS_DIRECTORY="$tls_dir"
export LBP_TRUST_DIRECTORY="$trust_dir"
export LBP_IDENTITY_DIRECTORY="$identity_dir"
export LBP_CONFIG_DIRECTORY="$config_dir"
export LBP_TLS_HOSTNAME="$tls_hostname"
export LBP_PROXY_PROD_PORT="$proxy_port"
export LBP_PROXY_PROD_IMAGE="${LBP_PROXY_PROD_IMAGE:-loadbalancerpro:${project_name}}"
export LBP_PROXY_PROD_FIXTURE_IMAGE="${LBP_PROXY_PROD_FIXTURE_IMAGE:-loadbalancerpro:${project_name}-fixture}"

compose=(docker compose -p "$project_name" -f "$compose_base" -f "$compose_override")
"${compose[@]}" config --quiet
if [[ "${LBP_BENCH_REUSE_IMAGE:-false}" == "true" ]]; then
    "${compose[@]}" up --no-build --detach
else
    "${compose[@]}" up --build --detach
fi

base_url="https://127.0.0.1:$proxy_port"
curl_tls=(--silent --show-error --cacert "$tls_dir/ca.pem" --connect-timeout 3 --max-time 10)
for attempt in $(seq 1 120); do
    if curl "${curl_tls[@]}" --fail --output /dev/null "$base_url/api/health"; then
        break
    fi
    if [[ "$attempt" -eq 120 ]]; then
        "${compose[@]}" ps
        "${compose[@]}" logs loadbalancerpro
        echo "Benchmark stack did not become healthy" >&2
        exit 1
    fi
    sleep 1
done

jq -n '{
  enabled: true,
  strategy: "ROUND_ROBIN",
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
  limits: {maxInFlight: 100, adaptive: false},
  shedding: {enabled: false},
  accessLog: {enabled: false, format: "JSON", path: "/tmp/loadbalancerpro/proxy-access.log", sampleRate: 1.0},
  upstreams: [
    {id: "backend-a", url: "http://backend-a:8080", healthy: true, maxInFlight: 50, weight: 1.0},
    {id: "backend-b", url: "http://backend-b:8080", healthy: true, maxInFlight: 50, weight: 1.0}
  ]
}' > "$reload_payload"

authenticated_curl() {
    curl "${curl_tls[@]}" --header "X-API-Key: $api_key" "$@"
}

reload_baseline() {
    local response_file="$work_dir/reload-response.json"
    authenticated_curl --fail --header 'Content-Type: application/json' \
        --data-binary "@$reload_payload" --output "$response_file" "$base_url/api/proxy/reload"
    jq -e '.success == true and .status == "success"' "$response_file" >/dev/null
}

wait_for_two_healthy_backends() {
    local status_file="$work_dir/proxy-status.json"
    for attempt in $(seq 1 30); do
        authenticated_curl --fail --output "$status_file" "$base_url/api/proxy/status"
        if jq -e '.observability.effectiveHealthyBackendCount == 2' "$status_file" >/dev/null 2>&1; then
            return
        fi
        sleep 1
    done
    echo "Both loopback fixture backends did not become healthy" >&2
    exit 1
}

fetch_metrics() {
    authenticated_curl --fail "$base_url/actuator/prometheus"
}

wait_for_inflight_zero() {
    local value
    for attempt in $(seq 1 30); do
        value="$(fetch_metrics | awk '$1 ~ /^lbp_proxy_inflight(\{|$)/ { total += $NF } END { printf "%.0f", total + 0 }')"
        if [[ "$value" == "0" ]]; then
            return
        fi
        sleep 1
    done
    echo "lbp_proxy_inflight did not return to zero" >&2
    exit 1
}

assert_report() {
    local scenario="$1"
    local report_file="$2"
    local p99_budget_ms="$3"
    local require_zero_failures="$4"
    local max_failure_percent="$5"
    local requests p99_ns p99_ms status_5xx error_count success_ratio failures
    requests="$(jq -r '.requests' "$report_file")"
    p99_ns="$(jq -r '.latencies["99th"]' "$report_file")"
    status_5xx="$(jq '[.status_codes | to_entries[]? | select(.key | test("^5[0-9][0-9]$")) | .value] | add // 0' "$report_file")"
    error_count="$(jq '.errors | length' "$report_file")"
    success_ratio="$(jq -r '.success' "$report_file")"
    require_positive_integer "$scenario requests" "$requests"
    p99_ms="$(awk -v value="$p99_ns" 'BEGIN { printf "%.3f", value / 1000000 }')"
    awk -v actual="$p99_ms" -v budget="$p99_budget_ms" 'BEGIN { exit !(actual <= budget) }' || {
        echo "$scenario p99 ${p99_ms}ms exceeded local budget ${p99_budget_ms}ms" >&2
        exit 1
    }
    failures=$(( status_5xx + error_count ))
    if [[ "$require_zero_failures" == "true" && "$failures" -ne 0 ]]; then
        echo "$scenario requires zero 5xx responses and transport errors; observed $failures" >&2
        exit 1
    fi
    if [[ "$require_zero_failures" != "true" ]] \
        && (( failures * 100 > requests * max_failure_percent )); then
        echo "$scenario exceeded its ${max_failure_percent}% bounded failure allowance" >&2
        exit 1
    fi
    printf '%s,%s,%s,%s,%s,%s,%s,%s,%s\n' \
        "$scenario" "$requests" "$p99_ms" "$p99_budget_ms" "$status_5xx" "$error_count" \
        "$success_ratio" "$require_zero_failures" "$max_failure_percent" >> "$summary_file"
}

action_reload() {
    reload_baseline
}

action_drain() {
    local config_file="$work_dir/admin-config.json"
    local response_file="$work_dir/drain-response.json"
    local generation
    authenticated_curl --fail --output "$config_file" "$base_url/api/proxy/config"
    generation="$(jq -r '.generation' "$config_file")"
    require_positive_integer "active configuration generation" "$generation"
    authenticated_curl --fail --request DELETE --output "$response_file" \
        "$base_url/api/proxy/upstreams/backend-a?expectedGeneration=$generation"
    jq -e '.success == true and .action == "delete"' "$response_file" >/dev/null
}

action_backend_kill() {
    "${compose[@]}" stop --timeout 2 backend-a
}

after_backend_kill() {
    "${compose[@]}" start backend-a
    wait_for_two_healthy_backends
}

after_drain() {
    local config_file="$work_dir/drain-config.json"
    for attempt in $(seq 1 30); do
        authenticated_curl --fail --output "$config_file" "$base_url/api/proxy/config"
        if jq -e '.drainingUpstreamIds | length == 0' "$config_file" >/dev/null; then
            return
        fi
        sleep 1
    done
    echo "Drained upstream did not converge inside the bounded wait" >&2
    exit 1
}

run_attack() {
    local scenario="$1"
    local path="$2"
    local rate="$3"
    local duration_seconds="$4"
    local p99_budget_ms="$5"
    local require_zero_failures="$6"
    local max_failure_percent="$7"
    local during_action="${8:-}"
    local after_action="${9:-}"
    local targets_file="$work_dir/$scenario.targets"
    local results_file="$work_dir/$scenario.bin"
    local report_file="$output_dir/$scenario.json"
    local text_file="$output_dir/$scenario.txt"
    printf 'GET %s%s\nX-API-Key: %s\n\n' "$base_url" "$path" "$api_key" > "$targets_file"
    vegeta attack -duration="${duration_seconds}s" -rate="${rate}/s" -timeout=6s \
        -root-certs="$tls_dir/ca.pem" -targets="$targets_file" > "$results_file" &
    attack_pid=$!
    if [[ -n "$during_action" ]]; then
        sleep $(( duration_seconds / 3 > 0 ? duration_seconds / 3 : 1 ))
        "$during_action"
    fi
    if ! wait "$attack_pid"; then
        attack_pid=""
        echo "$scenario Vegeta attack failed" >&2
        exit 1
    fi
    attack_pid=""
    if [[ -n "$after_action" ]]; then
        "$after_action"
    fi
    vegeta report -type=json "$results_file" > "$report_file"
    vegeta report -type=text "$results_file" > "$text_file"
    rm -f -- "$results_file"
    assert_report "$scenario" "$report_file" "$p99_budget_ms" \
        "$require_zero_failures" "$max_failure_percent"
    wait_for_inflight_zero
}

run_scenario_set() {
    reload_baseline
    wait_for_two_healthy_backends
    run_attack steady '/proxy/bench/steady' "$steady_rate" "$scenario_seconds" \
        "$normal_p99_budget_ms" true 0
    run_attack spike '/proxy/bench/spike' "$spike_rate" "$scenario_seconds" \
        "$normal_p99_budget_ms" true 0
    run_attack slow-backend '/proxy/slow?millis=250' "$steady_rate" "$scenario_seconds" \
        "$slow_p99_budget_ms" true 0
    run_attack backend-kill '/proxy/bench/backend-kill' "$steady_rate" "$scenario_seconds" \
        "$failure_p99_budget_ms" false 10 action_backend_kill after_backend_kill
    run_attack reload-under-load '/proxy/bench/reload' "$steady_rate" "$scenario_seconds" \
        "$normal_p99_budget_ms" true 0 action_reload
    run_attack drain-under-load '/proxy/bench/drain' "$steady_rate" "$scenario_seconds" \
        "$normal_p99_budget_ms" true 0 action_drain after_drain
}

read_heap_used() {
    fetch_metrics | awk '$1 ~ /^jvm_memory_used_bytes\{/ && $1 ~ /area="heap"/ { total += $NF } END { printf "%.0f", total + 0 }'
}

sample_heap() {
    local heap_file="$output_dir/heap-samples.csv"
    printf 'epoch_seconds,heap_used_bytes\n' > "$heap_file"
    while [[ ! -f "$heap_stop_file" ]]; do
        local heap_used waited
        heap_used="$(read_heap_used)"
        [[ "$heap_used" =~ ^[1-9][0-9]*$ ]] || {
            echo "Heap sample must be a positive byte count" >&2
            return 1
        }
        printf '%s,%s\n' "$(date +%s)" "$heap_used" >> "$heap_file"
        for (( waited = 0; waited < heap_sample_seconds; waited++ )); do
            [[ -f "$heap_stop_file" ]] && break
            sleep 1
        done
    done
}

analyze_heap_trend() {
    local heap_file="$output_dir/heap-samples.csv"
    local analysis_file="$output_dir/heap-analysis.json"
    awk -F, -v budget="$heap_growth_budget_bytes" -v output="$analysis_file" \
        -f "$heap_analyzer" "$heap_file"
}

run_scenario_set

if [[ "$mode" == "soak" ]]; then
    "${compose[@]}" restart loadbalancerpro backend-a backend-b
    for attempt in $(seq 1 120); do
        if curl "${curl_tls[@]}" --fail --output /dev/null "$base_url/api/health"; then
            break
        fi
        [[ "$attempt" -lt 120 ]] || { echo "Stack did not recover before soak" >&2; exit 1; }
        sleep 1
    done
    reload_baseline
    wait_for_two_healthy_backends
    initial_heap_used="$(read_heap_used)"
    require_positive_integer "initial heap sample" "$initial_heap_used"
    sample_heap &
    heap_sampler_pid=$!
    first_segment=$(( soak_seconds / 3 ))
    second_segment=$(( soak_seconds / 3 ))
    third_segment=$(( soak_seconds - first_segment - second_segment ))
    run_attack soak-steady '/proxy/bench/soak' "$steady_rate" "$first_segment" \
        "$normal_p99_budget_ms" true 0
    run_attack soak-reload-under-load '/proxy/bench/soak-reload' "$steady_rate" "$second_segment" \
        "$normal_p99_budget_ms" true 0 action_reload
    run_attack soak-drain-under-load '/proxy/bench/soak-drain' "$steady_rate" "$third_segment" \
        "$normal_p99_budget_ms" true 0 action_drain after_drain
    : > "$heap_stop_file"
    if ! wait "$heap_sampler_pid"; then
        heap_sampler_pid=""
        echo "Heap sampler failed" >&2
        exit 1
    fi
    heap_sampler_pid=""
    analyze_heap_trend
fi

wait_for_inflight_zero
printf 'Proxy %s regression harness passed against loopback Compose fixtures.\n' "$mode"
printf 'Evidence: %s\n' "$output_dir"
printf 'Boundary: local regression budgets only; no production SLO, capacity, or certification claim.\n'
