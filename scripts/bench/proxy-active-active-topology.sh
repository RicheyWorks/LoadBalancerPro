#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
compose_file="$repo_root/deploy/topology/docker-compose.active-active.yml"
candidate_dockerfile="$repo_root/deploy/topology/RolloutCandidate.Dockerfile"
rejected_dockerfile="$repo_root/deploy/topology/RolloutRejected.Dockerfile"
example_profile="$script_dir/topology-profile.example.json"

mode=validate
profile="$example_profile"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --mode) [[ $# -ge 2 ]] || { echo "--mode requires validate, smoke, or run" >&2; exit 2; }; mode="$2"; shift 2 ;;
        --profile) [[ $# -ge 2 ]] || { echo "--profile requires JSON" >&2; exit 2; }; profile="$2"; shift 2 ;;
        --help|-h) echo "Usage: $0 --mode validate|smoke|run [--profile topology-profile.json]"; exit 0 ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
done
case "$mode" in validate|smoke|run) ;; *) echo "Mode must be validate, smoke, or run" >&2; exit 2 ;; esac
command -v jq >/dev/null 2>&1 || { echo "jq is required" >&2; exit 2; }
for required_file in "$compose_file" "$candidate_dockerfile" "$rejected_dockerfile" "$profile"; do
    [[ -f "$required_file" ]] || { echo "Missing required file: $required_file" >&2; exit 2; }
done

jq -e '
  .schemaVersion == 1
  and (.profileId | type == "string" and test("^[a-z0-9][a-z0-9._-]{0,62}$"))
  and (.review.status | type == "string" and length > 0)
  and .topology.replicas == 2
  and .topology.zones == 1
  and .topology.ingress == "bounded-loopback-round-robin-fixture"
  and (.topology.ingressUpstreamTimeoutMillis | type == "number" and . >= 100 and . <= 2000 and floor == .)
  and .topology.affinity == false
  and (.topology.baselineStrategy | IN("ROUND_ROBIN", "WEIGHTED_ROUND_ROBIN",
      "WEIGHTED_LEAST_CONNECTIONS", "WEIGHTED_LEAST_LOAD", "TAIL_LATENCY_POWER_OF_TWO"))
  and (.topology.rolloutStrategy | IN("ROUND_ROBIN", "WEIGHTED_ROUND_ROBIN",
      "WEIGHTED_LEAST_CONNECTIONS", "WEIGHTED_LEAST_LOAD", "TAIL_LATENCY_POWER_OF_TWO"))
  and .topology.baselineStrategy != .topology.rolloutStrategy
  and (.workload.ratePerSecond | type == "number" and . > 0 and floor == .)
  and (.workload.scenarioSeconds | type == "number" and . >= 8 and floor == .)
  and (.workload.distributionRequests | type == "number" and . >= 20 and floor == . and (. % 2 == 0))
  and (.rollout.candidateReleaseId | type == "string" and test("^[a-z0-9][a-z0-9._-]{0,62}$"))
  and (.rollout.trafficDurationSeconds | type == "number" and . >= 20 and floor == .)
  and (.rollout.maximumReplicaReplacementMillis | type == "number" and . >= 5000 and . <= 120000 and floor == .)
  and ((.rollout.maximumRolloutMillis | type) == "number"
      and .rollout.maximumRolloutMillis >= (2 * .rollout.maximumReplicaReplacementMillis)
      and .rollout.maximumRolloutMillis <= 300000
      and (.rollout.maximumRolloutMillis | floor) == .rollout.maximumRolloutMillis)
  and ((.rollout.maximumRollbackMillis | type) == "number"
      and .rollout.maximumRollbackMillis >= (2 * .rollout.maximumReplicaReplacementMillis)
      and .rollout.maximumRollbackMillis <= 300000
      and (.rollout.maximumRollbackMillis | floor) == .rollout.maximumRollbackMillis)
  and ((.rollout.maximumAbortRecoveryMillis | type) == "number"
      and .rollout.maximumAbortRecoveryMillis >= .rollout.maximumReplicaReplacementMillis
      and .rollout.maximumAbortRecoveryMillis <= 120000
      and (.rollout.maximumAbortRecoveryMillis | floor) == .rollout.maximumAbortRecoveryMillis)
  and ((.rollout.trafficDurationSeconds * 1000) >= (.rollout.maximumRolloutMillis + 5000))
  and ((.rollout.trafficDurationSeconds * 1000) >= (.rollout.maximumRollbackMillis + 5000))
  and ((.rollout.trafficDurationSeconds * 1000) >= (.rollout.maximumAbortRecoveryMillis + 5000))
  and (.limits.globalMaxInFlight | type == "number" and . > 0 and floor == .)
  and (.limits.perReplicaUpstreamMaxInFlight | type == "number" and . > 0 and floor == .)
  and (.limits.aggregateUpstreamConnectionBudget | type == "number" and . > 0 and floor == .)
  and (.limits.globalMaxInFlight <= (2 * .limits.perReplicaUpstreamMaxInFlight))
  and ((.topology.replicas * 2 * .limits.perReplicaUpstreamMaxInFlight)
       <= .limits.aggregateUpstreamConnectionBudget)
  and (.objectives.minimumSuccessRatio | type == "number" and . > 0 and . <= 1)
  and (.objectives.p99Millis | type == "number" and . > 0)
  and (.objectives.maximumDistributionSkewPercent | type == "number" and . >= 0 and . <= 50)
  and (.objectives.maximumGenerationSkewMillis | type == "number" and . > 0 and floor == .)
  and (.objectives.recoveryWindowSeconds | type == "number" and . >= 10 and floor == .)
' "$profile" >/dev/null || { echo "Topology profile does not satisfy the executable contract" >&2; exit 2; }

if [[ "$mode" == "validate" ]]; then
    printf 'Validated two-replica loopback topology contract %s.\n' "$(jq -r '.profileId' "$profile")"
    printf 'Validated proof cases: distribution config-rollout config-rollback candidate-rejection immutable-rollout immutable-rollback replica-loss recovery aggregate-limits per-instance-metrics\n'
    exit 0
fi
if [[ "$mode" == "run" ]]; then
    jq -e '.review.status == "reviewed"
      and (.review.approvedBy | type == "string" and length > 0)
      and (.review.approvedAt | fromdateiso8601 > 0)
      and .workload.scenarioSeconds >= 30' "$profile" >/dev/null || {
        echo "Run mode requires reviewed approval and at least 30 seconds per loaded case" >&2
        exit 2
    }
    [[ -z "$(git -C "$repo_root" status --porcelain)" ]] || { echo "Run mode requires a clean checkout" >&2; exit 2; }
fi
for command_name in awk curl docker jq openssl realpath sha256sum vegeta; do
    command -v "$command_name" >/dev/null 2>&1 || { echo "$command_name is required" >&2; exit 2; }
done
docker compose version >/dev/null

require_port() {
    local name="$1" value="$2"
    [[ "$value" =~ ^[1-9][0-9]*$ ]] && (( value >= 1024 && value <= 65535 )) || {
        echo "$name must be an unprivileged TCP port" >&2
        exit 2
    }
}
ingress_port="${LBP_TOPOLOGY_INGRESS_PORT:-18450}"
proxy_a_port="${LBP_TOPOLOGY_PROXY_A_PORT:-18451}"
proxy_b_port="${LBP_TOPOLOGY_PROXY_B_PORT:-18452}"
require_port LBP_TOPOLOGY_INGRESS_PORT "$ingress_port"
require_port LBP_TOPOLOGY_PROXY_A_PORT "$proxy_a_port"
require_port LBP_TOPOLOGY_PROXY_B_PORT "$proxy_b_port"
[[ "$ingress_port" != "$proxy_a_port" && "$ingress_port" != "$proxy_b_port" && "$proxy_a_port" != "$proxy_b_port" ]] || {
    echo "Topology ports must be distinct" >&2
    exit 2
}
project_name="${LBP_TOPOLOGY_PROJECT:-lbp-topology-${GITHUB_RUN_ID:-local-${BASHPID}}-${GITHUB_RUN_ATTEMPT:-1}}"
[[ "$project_name" =~ ^[a-z0-9][a-z0-9_-]{0,62}$ ]] || { echo "LBP_TOPOLOGY_PROJECT is invalid" >&2; exit 2; }

profile_id="$(jq -r '.profileId' "$profile")"
default_run_id="${GITHUB_RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)-${BASHPID}}-${GITHUB_RUN_ATTEMPT:-1}"
output_dir="${LBP_TOPOLOGY_OUTPUT_DIR:-$repo_root/target/topology/$profile_id/$default_run_id}"
umask 077
mkdir -p "$repo_root/target"
target_root="$(cd "$repo_root/target" && pwd -P)"
[[ "$output_dir" == /* ]] || output_dir="$repo_root/$output_dir"
output_dir="$(realpath -m -- "$output_dir")"
case "$output_dir/" in "$target_root"/*) ;; *) echo "Topology output must remain beneath target/" >&2; exit 2 ;; esac
mkdir -p "$output_dir"
output_dir="$(cd "$output_dir" && pwd -P)"
[[ -z "$(find "$output_dir" -mindepth 1 -print -quit)" ]] || { echo "Topology output directory must be empty" >&2; exit 2; }

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-topology.XXXXXX")"
api_key_file="$work_dir/loadbalancerpro-api-key"
tls_dir="$work_dir/tls"
trust_dir="$work_dir/trust"
identity_dir="$work_dir/identity"
config_dir="$work_dir/config"
ingress_dir="$work_dir/ingress"
attack_pid=""
compose=()
cleanup() {
    local status=$?
    trap - EXIT
    if [[ -n "$attack_pid" ]]; then kill "$attack_pid" >/dev/null 2>&1 || true; wait "$attack_pid" >/dev/null 2>&1 || true; fi
    if [[ ${#compose[@]} -gt 0 ]]; then "${compose[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true; fi
    case "$work_dir" in "${TMPDIR:-/tmp}"/lbp-topology.*) chmod -R u+w "$work_dir" 2>/dev/null || true; rm -rf -- "$work_dir" ;;
        *) echo "Refusing to remove unexpected topology path: $work_dir" >&2 ;; esac
    exit "$status"
}
trap cleanup EXIT

mkdir -p "$tls_dir" "$trust_dir" "$identity_dir" "$config_dir" "$ingress_dir"
openssl rand -hex 24 > "$api_key_file"
api_key="$(<"$api_key_file")"
openssl rand -hex 16 > "$ingress_dir/server-password"
ca_key="$work_dir/ca-key.pem"
server_csr="$work_dir/server.csr"
server_extensions="$work_dir/server-extensions.cnf"
MSYS2_ARG_CONV_EXCL='/CN=LoadBalancerPro Topology Proof CA' openssl req -x509 -newkey rsa:2048 -sha256 -days 1 -nodes \
    -subj '/CN=LoadBalancerPro Topology Proof CA' \
    -addext 'basicConstraints=critical,CA:TRUE' -addext 'keyUsage=critical,keyCertSign,cRLSign' \
    -keyout "$ca_key" -out "$tls_dir/ca.pem" >/dev/null 2>&1
MSYS2_ARG_CONV_EXCL='/CN=lbp-topology.local' openssl req -newkey rsa:2048 -sha256 -nodes -subj '/CN=lbp-topology.local' \
    -keyout "$tls_dir/private-key.pem" -out "$server_csr" >/dev/null 2>&1
printf '%s\n' \
    'subjectAltName=DNS:lbp-topology.local,DNS:proxy-a,DNS:proxy-b,DNS:topology-ingress,IP:127.0.0.1' \
    'basicConstraints=critical,CA:FALSE' \
    'keyUsage=critical,digitalSignature,keyEncipherment' \
    'extendedKeyUsage=serverAuth' > "$server_extensions"
openssl x509 -req -sha256 -days 1 -in "$server_csr" -CA "$tls_dir/ca.pem" -CAkey "$ca_key" \
    -set_serial 1 -extfile "$server_extensions" -out "$tls_dir/certificate.pem" >/dev/null 2>&1
openssl_password_file="$ingress_dir/server-password"
if command -v cygpath >/dev/null 2>&1; then
    openssl_password_file="$(cygpath -m "$openssl_password_file")"
fi
MSYS2_ARG_CONV_EXCL="file:$openssl_password_file" \
openssl pkcs12 -export -name topology-ingress -in "$tls_dir/certificate.pem" \
    -inkey "$tls_dir/private-key.pem" -certfile "$tls_dir/ca.pem" \
    -out "$ingress_dir/server.p12" -passout "file:$openssl_password_file" >/dev/null 2>&1
cp "$tls_dir/ca.pem" "$ingress_dir/ca.pem"
chmod 0444 "$api_key_file" "$tls_dir"/* "$ingress_dir"/*
chmod 0555 "$tls_dir" "$trust_dir" "$identity_dir" "$config_dir" "$ingress_dir"

export LBP_TOPOLOGY_API_KEY_FILE="$api_key_file"
export LBP_TOPOLOGY_TLS_DIRECTORY="$tls_dir"
export LBP_TOPOLOGY_TRUST_DIRECTORY="$trust_dir"
export LBP_TOPOLOGY_IDENTITY_DIRECTORY="$identity_dir"
export LBP_TOPOLOGY_CONFIG_DIRECTORY="$config_dir"
export LBP_TOPOLOGY_INGRESS_DIRECTORY="$ingress_dir"
export LBP_TOPOLOGY_INGRESS_PORT="$ingress_port"
export LBP_TOPOLOGY_PROXY_A_PORT="$proxy_a_port"
export LBP_TOPOLOGY_PROXY_B_PORT="$proxy_b_port"
export LBP_TOPOLOGY_INGRESS_TIMEOUT_MILLIS="$(jq -r '.topology.ingressUpstreamTimeoutMillis' "$profile")"
export LBP_TOPOLOGY_STRATEGY="$(jq -r '.topology.baselineStrategy' "$profile")"
export LBP_TOPOLOGY_PROXY_IMAGE="${LBP_TOPOLOGY_PROXY_IMAGE:-loadbalancerpro:${project_name}}"
export LBP_TOPOLOGY_FIXTURE_IMAGE="${LBP_TOPOLOGY_FIXTURE_IMAGE:-loadbalancerpro:${project_name}-fixture}"
export LBP_TOPOLOGY_INGRESS_IMAGE="${LBP_TOPOLOGY_INGRESS_IMAGE:-loadbalancerpro:${project_name}-ingress}"
candidate_image_tag="${LBP_TOPOLOGY_CANDIDATE_IMAGE:-loadbalancerpro:${project_name}-candidate}"
rejected_image_tag="${LBP_TOPOLOGY_REJECTED_IMAGE:-loadbalancerpro:${project_name}-rejected}"
for local_image_tag in "$candidate_image_tag" "$rejected_image_tag"; do
    [[ "$local_image_tag" =~ ^loadbalancerpro:[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$ ]] || {
        echo "Rollout proof image tags must remain in the local loadbalancerpro repository" >&2
        exit 2
    }
done
export LBP_TOPOLOGY_PROXY_A_IMAGE="$LBP_TOPOLOGY_PROXY_IMAGE"
export LBP_TOPOLOGY_PROXY_B_IMAGE="$LBP_TOPOLOGY_PROXY_IMAGE"
compose=(docker compose -p "$project_name" -f "$compose_file")
"${compose[@]}" config --quiet
if [[ "${LBP_TOPOLOGY_REUSE_PROXY_IMAGE:-false}" == "true" ]]; then
    "${compose[@]}" build backend-a topology-ingress
else
    "${compose[@]}" build proxy-a backend-a topology-ingress
fi

baseline_image_id="$(docker image inspect --format '{{.Id}}' "$LBP_TOPOLOGY_PROXY_IMAGE")"
[[ "$baseline_image_id" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "Baseline proxy image has no exact content ID" >&2; exit 1; }
candidate_release_id="$(jq -r '.rollout.candidateReleaseId' "$profile")"
docker build --quiet --file "$candidate_dockerfile" --build-arg "BASE_IMAGE=$LBP_TOPOLOGY_PROXY_IMAGE" \
    --build-arg "ROLLOUT_RELEASE_ID=$candidate_release_id" --tag "$candidate_image_tag" "$repo_root" >/dev/null
docker build --quiet --file "$rejected_dockerfile" --build-arg "BASE_IMAGE=$LBP_TOPOLOGY_PROXY_IMAGE" \
    --build-arg "ROLLOUT_RELEASE_ID=${candidate_release_id}-rejected" --tag "$rejected_image_tag" "$repo_root" >/dev/null
[[ "$(docker image inspect --format '{{.Id}}' "$LBP_TOPOLOGY_PROXY_IMAGE")" == "$baseline_image_id" ]] || {
    echo "Baseline proxy image tag changed while deriving rollout proof images" >&2; exit 1;
}
candidate_image_id="$(docker image inspect --format '{{.Id}}' "$candidate_image_tag")"
rejected_image_id="$(docker image inspect --format '{{.Id}}' "$rejected_image_tag")"
for exact_image_id in "$candidate_image_id" "$rejected_image_id"; do
    [[ "$exact_image_id" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "Rollout proof image has no exact content ID" >&2; exit 1; }
    [[ "$exact_image_id" != "$baseline_image_id" ]] || { echo "Rollout proof image is not content-distinct" >&2; exit 1; }
done
[[ "$candidate_image_id" != "$rejected_image_id" ]] || { echo "Candidate and rejected image IDs must differ" >&2; exit 1; }
baseline_layers="$(docker image inspect --format '{{json .RootFS.Layers}}' "$baseline_image_id")"
[[ "$(docker image inspect --format '{{json .RootFS.Layers}}' "$candidate_image_id")" == "$baseline_layers" ]] || {
    echo "Candidate proof image changed application layers" >&2; exit 1;
}
[[ "$(docker image inspect --format '{{json .RootFS.Layers}}' "$rejected_image_id")" == "$baseline_layers" ]] || {
    echo "Rejected proof image changed application layers" >&2; exit 1;
}
for inherited_config in '{{json .Config.Cmd}}' '{{json .Config.Entrypoint}}' '{{json .Config.User}}'; do
    [[ "$(docker image inspect --format "$inherited_config" "$candidate_image_id")" == \
       "$(docker image inspect --format "$inherited_config" "$baseline_image_id")" ]] || {
        echo "Candidate proof image changed inherited runtime configuration" >&2; exit 1;
    }
done
[[ "$(docker image inspect --format '{{index .Config.Labels "com.richeyworks.loadbalancerpro.rollout.release-id"}}' \
    "$candidate_image_id")" == "$candidate_release_id" ]] || { echo "Candidate release label is missing" >&2; exit 1; }
[[ "$(docker image inspect --format '{{json .Config.Entrypoint}}' "$rejected_image_id")" == '["/bin/false"]' ]] || {
    echo "Rejected candidate must fail before application startup" >&2; exit 1;
}
export LBP_TOPOLOGY_PROXY_A_IMAGE="$baseline_image_id"
export LBP_TOPOLOGY_PROXY_B_IMAGE="$baseline_image_id"
"${compose[@]}" config --quiet
"${compose[@]}" up --no-build --detach

curl_config="$work_dir/curl-auth.conf"
printf 'header = "X-API-Key: %s"\n' "$api_key" > "$curl_config"
curl_tls=(--silent --show-error --fail --cacert "$tls_dir/ca.pem" --config "$curl_config" --connect-timeout 3 --max-time 12)
ingress_url="https://127.0.0.1:$ingress_port"
proxy_a_url="https://proxy-a:$proxy_a_port"
proxy_b_url="https://proxy-b:$proxy_b_port"

curl_replica() {
    local replica="$1" path="$2" output="$3"
    if [[ "$replica" == "proxy-a" ]]; then
        curl "${curl_tls[@]}" --resolve "proxy-a:$proxy_a_port:127.0.0.1" --output "$output" "$proxy_a_url$path"
    else
        curl "${curl_tls[@]}" --resolve "proxy-b:$proxy_b_port:127.0.0.1" --output "$output" "$proxy_b_url$path"
    fi
}

wait_for_url() {
    local kind="$1" deadline=$(( SECONDS + $(jq -r '.objectives.recoveryWindowSeconds' "$profile") ))
    while (( SECONDS < deadline )); do
        if [[ "$kind" == "ingress" ]]; then
            if curl "${curl_tls[@]}" --output /dev/null "$ingress_url/health"; then return 0; fi
        elif curl_replica "$kind" /actuator/health /dev/null; then return 0; fi
        sleep 1
    done
    echo "$kind did not become ready within the recovery window" >&2
    return 1
}
wait_for_url proxy-a
wait_for_url proxy-b
wait_for_url ingress

global_max="$(jq -r '.limits.globalMaxInFlight' "$profile")"
upstream_max="$(jq -r '.limits.perReplicaUpstreamMaxInFlight' "$profile")"
write_config() {
    local strategy="$1" destination="$2"
    jq -n --arg strategy "$strategy" --argjson globalMax "$global_max" --argjson upstreamMax "$upstream_max" '{
      enabled:true,strategy:$strategy,connectTimeout:"PT1S",requestTimeout:"PT5S",maxRequestBytes:65536,maxResponseBytes:0,
      healthCheck:{enabled:true,path:"/health",timeout:"PT1S",interval:"PT1S",healthyThreshold:1,unhealthyThreshold:2},
      retry:{enabled:true,maxAttempts:2,budgetPercent:100,backoff:{base:"PT0.01S",max:"PT0.05S"},
        retryNonIdempotent:false,methods:["GET","HEAD"],retryStatuses:[502,503,504]},
      reload:{drainTimeout:"PT10S"},cooldown:{enabled:true,consecutiveFailureThreshold:2,duration:"PT1S",recoverOnSuccessfulHealthCheck:true},
      slowStart:{duration:"PT0S"},forwarded:{mode:"strip-and-set",trustedProxies:[]},
      limits:{maxInFlight:$globalMax,adaptive:false},shedding:{enabled:false},
      accessLog:{enabled:false,format:"JSON",path:"/tmp/loadbalancerpro/proxy-access.log",sampleRate:1.0},
      upstreams:[
        {id:"backend-a",url:"http://backend-a:8080",healthy:true,maxInFlight:$upstreamMax,weight:1.0},
        {id:"backend-b",url:"http://backend-b:8080",healthy:true,maxInFlight:$upstreamMax,weight:1.0}
      ]}' > "$destination"
}
baseline_config="$work_dir/baseline.json"
rollout_config="$work_dir/rollout.json"
write_config "$(jq -r '.topology.baselineStrategy' "$profile")" "$baseline_config"
write_config "$(jq -r '.topology.rolloutStrategy' "$profile")" "$rollout_config"

reload_replica() {
    local replica="$1" payload="$2" output="$3"
    if [[ "$replica" == "proxy-a" ]]; then
        curl "${curl_tls[@]}" --resolve "proxy-a:$proxy_a_port:127.0.0.1" \
            --header 'Content-Type: application/json' --data-binary "@$payload" --output "$output" "$proxy_a_url/api/proxy/reload"
    else
        curl "${curl_tls[@]}" --resolve "proxy-b:$proxy_b_port:127.0.0.1" \
            --header 'Content-Type: application/json' --data-binary "@$payload" --output "$output" "$proxy_b_url/api/proxy/reload"
    fi
    jq -e '.success == true' "$output" >/dev/null
}
reload_replica proxy-a "$baseline_config" "$work_dir/baseline-a.json"
reload_replica proxy-b "$baseline_config" "$work_dir/baseline-b.json"

capture_configs() {
    local prefix="$1"
    curl_replica proxy-a /api/proxy/config "$output_dir/${prefix}-proxy-a-config.json"
    curl_replica proxy-b /api/proxy/config "$output_dir/${prefix}-proxy-b-config.json"
}
config_hash() { jq -S 'del(.generation,.drainingUpstreamIds)' "$1" | sha256sum | awk '{print $1}'; }
assert_converged() {
    local prefix="$1" a b
    a="$output_dir/${prefix}-proxy-a-config.json"
    b="$output_dir/${prefix}-proxy-b-config.json"
    [[ "$(jq -r '.generation' "$a")" == "$(jq -r '.generation' "$b")" ]] || { echo "$prefix generations diverged" >&2; exit 1; }
    [[ "$(config_hash "$a")" == "$(config_hash "$b")" ]] || { echo "$prefix configurations diverged" >&2; exit 1; }
}
assert_same_config() {
    local prefix="$1" a b
    a="$output_dir/${prefix}-proxy-a-config.json"
    b="$output_dir/${prefix}-proxy-b-config.json"
    [[ "$(config_hash "$a")" == "$(config_hash "$b")" ]] || { echo "$prefix configurations diverged" >&2; exit 1; }
}
capture_configs baseline
assert_converged baseline

actual_aggregate_limit="$(jq -s '[.[].routes[].upstreams[].maxInFlight] | add' \
    "$output_dir/baseline-proxy-a-config.json" "$output_dir/baseline-proxy-b-config.json")"
reviewed_aggregate_limit="$(jq -r '.limits.aggregateUpstreamConnectionBudget' "$profile")"
(( actual_aggregate_limit <= reviewed_aggregate_limit )) || { echo "Aggregate upstream connection limit exceeds reviewed budget" >&2; exit 1; }
jq -n --argjson actual "$actual_aggregate_limit" --argjson budget "$reviewed_aggregate_limit" \
    '{passed:($actual <= $budget),actualAggregateUpstreamLimit:$actual,reviewedBudget:$budget}' \
    > "$output_dir/aggregate-limit.json"

distribution_requests="$(jq -r '.workload.distributionRequests' "$profile")"
proxy_a_count=0
proxy_b_count=0
for request in $(seq 1 "$distribution_requests"); do
    headers="$work_dir/distribution-$request.headers"
    curl "${curl_tls[@]}" --dump-header "$headers" --output /dev/null "$ingress_url/proxy/topology/distribution"
    replica="$(awk -F': ' 'tolower($1) == "x-topology-replica" { gsub("\r", "", $2); print $2 }' "$headers" | tail -n 1)"
    case "$replica" in proxy-a) proxy_a_count=$((proxy_a_count + 1)) ;; proxy-b) proxy_b_count=$((proxy_b_count + 1)) ;;
        *) echo "Ingress response did not identify a known replica" >&2; exit 1 ;; esac
done
distribution_skew=$(( proxy_a_count > proxy_b_count ? proxy_a_count - proxy_b_count : proxy_b_count - proxy_a_count ))
distribution_skew_percent=$(( distribution_skew * 100 / distribution_requests ))
maximum_distribution_skew="$(jq -r '.objectives.maximumDistributionSkewPercent' "$profile")"
(( proxy_a_count > 0 && proxy_b_count > 0 && distribution_skew_percent <= maximum_distribution_skew )) || {
    echo "Ingress distribution exceeded the reviewed skew bound" >&2
    exit 1
}
jq -n --argjson total "$distribution_requests" --argjson proxyA "$proxy_a_count" --argjson proxyB "$proxy_b_count" \
  --argjson skewPercent "$distribution_skew_percent" --argjson maximumSkewPercent "$maximum_distribution_skew" \
  '{passed:true,total:$total,replicas:{"proxy-a":$proxyA,"proxy-b":$proxyB},
    skewPercent:$skewPercent,maximumSkewPercent:$maximumSkewPercent}' > "$output_dir/distribution.json"

scenario_seconds="$(jq -r '.workload.scenarioSeconds' "$profile")"
rate="$(jq -r '.workload.ratePerSecond' "$profile")"
minimum_success="$(jq -r '.objectives.minimumSuccessRatio' "$profile")"
p99_budget="$(jq -r '.objectives.p99Millis' "$profile")"
targets="$work_dir/topology.targets"
printf 'GET %s/proxy/topology/load\nX-API-Key: %s\n\n' "$ingress_url" "$api_key" > "$targets"

run_attack() {
    local name="$1" during="$2" after="${3:-}" duration="${4:-$scenario_seconds}" warmup="${5:-$(( scenario_seconds / 3 ))}"
    vegeta attack -duration="${duration}s" -rate="${rate}/s" -timeout=10s \
        -root-certs="$tls_dir/ca.pem" -max-body=0 -targets="$targets" > "$work_dir/$name.bin" &
    attack_pid=$!
    sleep "$warmup"
    "$during"
    kill -0 "$attack_pid" 2>/dev/null || {
        wait "$attack_pid" || true
        attack_pid=""
        echo "$name traffic ended before its deployment action completed" >&2
        exit 1
    }
    if ! wait "$attack_pid"; then attack_pid=""; echo "$name attack failed" >&2; exit 1; fi
    attack_pid=""
    [[ -z "$after" ]] || "$after"
    vegeta report -type=json "$work_dir/$name.bin" > "$output_dir/$name-client.json"
    vegeta report -type=text "$work_dir/$name.bin" > "$output_dir/$name-client.txt"
    rm -f -- "$work_dir/$name.bin"
    jq -e --argjson success "$minimum_success" --argjson p99 "$p99_budget" \
        '.success >= $success and (.latencies["99th"] / 1000000) <= $p99' "$output_dir/$name-client.json" >/dev/null || {
        echo "$name exceeded its topology traffic objective" >&2
        exit 1
    }
}

epoch_millis() { date +%s%3N; }
rollout_action() {
    local started completed skew
    started="$(epoch_millis)"
    reload_replica proxy-a "$rollout_config" "$work_dir/rollout-a.json"
    capture_configs rollout-skew
    [[ "$(jq -r '.generation' "$output_dir/rollout-skew-proxy-a-config.json")" != \
       "$(jq -r '.generation' "$output_dir/rollout-skew-proxy-b-config.json")" ]] || {
        echo "Rolling update did not expose the expected temporary generation skew" >&2; exit 1;
    }
    curl "${curl_tls[@]}" --output /dev/null "$ingress_url/health"
    reload_replica proxy-b "$rollout_config" "$work_dir/rollout-b.json"
    completed="$(epoch_millis)"
    skew=$(( completed - started ))
    capture_configs rollout-converged
    assert_converged rollout-converged
    (( skew <= $(jq -r '.objectives.maximumGenerationSkewMillis' "$profile") )) || {
        echo "Rolling update exceeded generation-skew window" >&2; exit 1;
    }
    jq -n --arg phase rollout --argjson skewMillis "$skew" \
      --argjson maximumSkewMillis "$(jq '.objectives.maximumGenerationSkewMillis' "$profile")" \
      '{phase:$phase,passed:($skewMillis <= $maximumSkewMillis),skewMillis:$skewMillis,
        maximumSkewMillis:$maximumSkewMillis}' > "$output_dir/rollout.json"
}

rollback_action() {
    local started completed skew
    started="$(epoch_millis)"
    reload_replica proxy-a "$baseline_config" "$work_dir/rollback-a.json"
    capture_configs rollback-skew
    [[ "$(jq -r '.generation' "$output_dir/rollback-skew-proxy-a-config.json")" != \
       "$(jq -r '.generation' "$output_dir/rollback-skew-proxy-b-config.json")" ]] || {
        echo "Rollback did not expose the expected temporary generation skew" >&2; exit 1;
    }
    curl "${curl_tls[@]}" --output /dev/null "$ingress_url/health"
    reload_replica proxy-b "$baseline_config" "$work_dir/rollback-b.json"
    completed="$(epoch_millis)"
    skew=$(( completed - started ))
    capture_configs rollback-converged
    assert_converged rollback-converged
    (( skew <= $(jq -r '.objectives.maximumGenerationSkewMillis' "$profile") )) || {
        echo "Rollback exceeded generation-skew window" >&2; exit 1;
    }
    jq -n --arg phase rollback --argjson skewMillis "$skew" \
      --argjson maximumSkewMillis "$(jq '.objectives.maximumGenerationSkewMillis' "$profile")" \
      '{phase:$phase,passed:($skewMillis <= $maximumSkewMillis),skewMillis:$skewMillis,
        maximumSkewMillis:$maximumSkewMillis}' > "$output_dir/rollback.json"
}

set_replica_image() {
    local replica="$1" exact_image_id="$2"
    [[ "$exact_image_id" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "Replica image must be an exact local content ID" >&2; exit 1; }
    docker image inspect "$exact_image_id" >/dev/null
    case "$replica" in
        proxy-a) export LBP_TOPOLOGY_PROXY_A_IMAGE="$exact_image_id" ;;
        proxy-b) export LBP_TOPOLOGY_PROXY_B_IMAGE="$exact_image_id" ;;
        *) echo "Unknown topology replica: $replica" >&2; exit 1 ;;
    esac
}

replica_container_id() {
    local replica="$1" container_id
    container_id="$("${compose[@]}" ps --all --quiet "$replica")"
    [[ "$container_id" =~ ^[0-9a-f]{12,64}$ ]] || { echo "Could not resolve one container for $replica" >&2; return 1; }
    printf '%s\n' "$container_id"
}

replica_image_id() {
    docker container inspect --format '{{.Image}}' "$(replica_container_id "$1")"
}

assert_replica_image() {
    local replica="$1" expected="$2" actual
    actual="$(replica_image_id "$replica")"
    [[ "$actual" == "$expected" ]] || {
        echo "$replica runs $actual instead of reviewed exact image $expected" >&2
        exit 1
    }
}

recreate_replica() {
    local replica="$1" exact_image_id="$2"
    set_replica_image "$replica" "$exact_image_id"
    "${compose[@]}" up --detach --no-deps --no-build --force-recreate "$replica" >/dev/null
    assert_replica_image "$replica" "$exact_image_id"
}

wait_for_replacement_readiness() {
    local replica="$1" expected_image_id="$2" maximum_millis="$3"
    local deadline container_id state actual
    deadline=$(( $(epoch_millis) + maximum_millis ))
    while (( $(epoch_millis) < deadline )); do
        container_id="$(replica_container_id "$replica")" || return 2
        actual="$(docker container inspect --format '{{.Image}}' "$container_id")"
        [[ "$actual" == "$expected_image_id" ]] || return 2
        state="$(docker container inspect --format '{{.State.Status}}' "$container_id")"
        case "$state" in
            exited|dead) return 1 ;;
            running) if curl_replica "$replica" /actuator/health /dev/null 2>/dev/null; then return 0; fi ;;
        esac
        sleep 1
    done
    return 2
}

maximum_replica_replacement_millis="$(jq -r '.rollout.maximumReplicaReplacementMillis' "$profile")"
baseline_config_hash="$(config_hash "$output_dir/baseline-proxy-a-config.json")"
replace_replica_ready() {
    local replica="$1" exact_image_id="$2" evidence_file="$3"
    local started completed elapsed replica_config actual_config_hash generation
    started="$(epoch_millis)"
    recreate_replica "$replica" "$exact_image_id"
    wait_for_replacement_readiness "$replica" "$exact_image_id" "$maximum_replica_replacement_millis" || {
        echo "$replica did not become ready on exact image $exact_image_id" >&2
        exit 1
    }
    reload_replica "$replica" "$baseline_config" "$work_dir/$(basename "$evidence_file").reload.json"
    wait_for_url ingress
    replica_config="$work_dir/$(basename "$evidence_file").config.json"
    curl_replica "$replica" /api/proxy/config "$replica_config"
    actual_config_hash="$(config_hash "$replica_config")"
    [[ "$actual_config_hash" == "$baseline_config_hash" ]] || {
        echo "$replica configuration changed during image replacement" >&2; exit 1;
    }
    generation="$(jq -r '.generation' "$replica_config")"
    completed="$(epoch_millis)"
    elapsed=$(( completed - started ))
    (( elapsed <= maximum_replica_replacement_millis )) || {
        echo "$replica replacement exceeded its reviewed window" >&2; exit 1;
    }
    jq -n --arg replica "$replica" --arg imageId "$exact_image_id" --arg configSha256 "$actual_config_hash" \
      --argjson generation "$generation" --argjson elapsedMillis "$elapsed" \
      --argjson maximumMillis "$maximum_replica_replacement_millis" \
      '{passed:($elapsedMillis <= $maximumMillis),replica:$replica,imageId:$imageId,
        configSha256:$configSha256,processLocalGeneration:$generation,
        elapsedMillis:$elapsedMillis,maximumMillis:$maximumMillis}' > "$evidence_file"
}

candidate_rejection_action() {
    local started completed elapsed candidate_container state maximum_abort
    started="$(epoch_millis)"
    recreate_replica proxy-a "$rejected_image_id"
    if wait_for_replacement_readiness proxy-a "$rejected_image_id" "$maximum_replica_replacement_millis"; then
        echo "The deliberately unhealthy candidate unexpectedly became ready" >&2
        exit 1
    fi
    candidate_container="$(replica_container_id proxy-a)"
    state="$(docker container inspect --format '{{.State.Status}}' "$candidate_container")"
    [[ "$state" == "exited" || "$state" == "dead" ]] || {
        echo "Candidate rejection did not produce a terminal container state" >&2; exit 1;
    }
    docker container inspect "$candidate_container" > "$output_dir/rejected-candidate-container.json"
    assert_replica_image proxy-b "$baseline_image_id"
    replace_replica_ready proxy-a "$baseline_image_id" "$output_dir/candidate-rejection-restore-step.json"
    assert_replica_image proxy-a "$baseline_image_id"
    assert_replica_image proxy-b "$baseline_image_id"
    capture_configs candidate-rejection-restored
    assert_same_config candidate-rejection-restored
    completed="$(epoch_millis)"
    elapsed=$(( completed - started ))
    maximum_abort="$(jq -r '.rollout.maximumAbortRecoveryMillis' "$profile")"
    (( elapsed <= maximum_abort )) || { echo "Candidate abort recovery exceeded its reviewed window" >&2; exit 1; }
    jq -n --arg rejectedImageId "$rejected_image_id" --arg restoredImageId "$baseline_image_id" \
      --arg rejectedState "$state" --argjson elapsedMillis "$elapsed" --argjson maximumMillis "$maximum_abort" \
      '{passed:($rejectedState == "exited" and $elapsedMillis <= $maximumMillis),
        rejectedImageId:$rejectedImageId,rejectedState:$rejectedState,
        secondReplicaPromoted:false,restoredImageId:$restoredImageId,
        elapsedMillis:$elapsedMillis,maximumMillis:$maximumMillis}' > "$output_dir/candidate-rejection.json"
}

immutable_rollout_action() {
    local started completed elapsed maximum_rollout
    started="$(epoch_millis)"
    replace_replica_ready proxy-a "$candidate_image_id" "$output_dir/immutable-rollout-proxy-a-step.json"
    assert_replica_image proxy-b "$baseline_image_id"
    capture_configs immutable-rollout-one-candidate
    assert_same_config immutable-rollout-one-candidate
    replace_replica_ready proxy-b "$candidate_image_id" "$output_dir/immutable-rollout-proxy-b-step.json"
    assert_replica_image proxy-a "$candidate_image_id"
    assert_replica_image proxy-b "$candidate_image_id"
    capture_configs immutable-rollout-converged
    assert_same_config immutable-rollout-converged
    completed="$(epoch_millis)"
    elapsed=$(( completed - started ))
    maximum_rollout="$(jq -r '.rollout.maximumRolloutMillis' "$profile")"
    (( elapsed <= maximum_rollout )) || { echo "Immutable rollout exceeded its reviewed window" >&2; exit 1; }
    jq -n --slurpfile proxyA "$output_dir/immutable-rollout-proxy-a-step.json" \
      --slurpfile proxyB "$output_dir/immutable-rollout-proxy-b-step.json" \
      --arg baselineImageId "$baseline_image_id" --arg candidateImageId "$candidate_image_id" \
      --argjson elapsedMillis "$elapsed" --argjson maximumMillis "$maximum_rollout" \
      '{passed:($proxyA[0].passed and $proxyB[0].passed and $elapsedMillis <= $maximumMillis),
        fromImageId:$baselineImageId,toImageId:$candidateImageId,steps:[$proxyA[0],$proxyB[0]],
        elapsedMillis:$elapsedMillis,maximumMillis:$maximumMillis}' > "$output_dir/immutable-rollout.json"
}

immutable_rollback_action() {
    local started completed elapsed maximum_rollback
    started="$(epoch_millis)"
    replace_replica_ready proxy-a "$baseline_image_id" "$output_dir/immutable-rollback-proxy-a-step.json"
    assert_replica_image proxy-b "$candidate_image_id"
    capture_configs immutable-rollback-one-baseline
    assert_same_config immutable-rollback-one-baseline
    replace_replica_ready proxy-b "$baseline_image_id" "$output_dir/immutable-rollback-proxy-b-step.json"
    assert_replica_image proxy-a "$baseline_image_id"
    assert_replica_image proxy-b "$baseline_image_id"
    capture_configs immutable-rollback-converged
    assert_same_config immutable-rollback-converged
    completed="$(epoch_millis)"
    elapsed=$(( completed - started ))
    maximum_rollback="$(jq -r '.rollout.maximumRollbackMillis' "$profile")"
    (( elapsed <= maximum_rollback )) || { echo "Immutable rollback exceeded its reviewed window" >&2; exit 1; }
    jq -n --slurpfile proxyA "$output_dir/immutable-rollback-proxy-a-step.json" \
      --slurpfile proxyB "$output_dir/immutable-rollback-proxy-b-step.json" \
      --arg candidateImageId "$candidate_image_id" --arg baselineImageId "$baseline_image_id" \
      --argjson elapsedMillis "$elapsed" --argjson maximumMillis "$maximum_rollback" \
      '{passed:($proxyA[0].passed and $proxyB[0].passed and $elapsedMillis <= $maximumMillis),
        fromImageId:$candidateImageId,toImageId:$baselineImageId,steps:[$proxyA[0],$proxyB[0]],
        elapsedMillis:$elapsedMillis,maximumMillis:$maximumMillis}' > "$output_dir/immutable-rollback.json"
}

loss_started_millis=0
stop_proxy_a() {
    loss_started_millis="$(epoch_millis)"
    "${compose[@]}" kill --signal SIGKILL proxy-a
}
recover_proxy_a() {
    "${compose[@]}" start proxy-a
    wait_for_url proxy-a
    reload_replica proxy-a "$baseline_config" "$work_dir/recovered-a.json"
    wait_for_url ingress
    local recovered_millis recovery_millis maximum_recovery_millis
    recovered_millis="$(epoch_millis)"
    recovery_millis=$(( recovered_millis - loss_started_millis ))
    maximum_recovery_millis=$(( $(jq -r '.objectives.recoveryWindowSeconds' "$profile") * 1000 ))
    (( recovery_millis <= maximum_recovery_millis )) || { echo "Replica recovery exceeded its reviewed window" >&2; exit 1; }
    jq -n --argjson recoveryMillis "$recovery_millis" --argjson maximumRecoveryMillis "$maximum_recovery_millis" \
      '{passed:($recoveryMillis <= $maximumRecoveryMillis),failureMode:"abrupt SIGKILL process loss",
        recoveryMillis:$recoveryMillis,
        maximumRecoveryMillis:$maximumRecoveryMillis}' > "$output_dir/recovery.json"
}

run_attack rollout-under-load rollout_action
run_attack rollback-under-load rollback_action
rollout_traffic_duration="$(jq -r '.rollout.trafficDurationSeconds' "$profile")"
run_attack candidate-rejection-under-load candidate_rejection_action "" "$rollout_traffic_duration" 2
run_attack immutable-rollout-under-load immutable_rollout_action "" "$rollout_traffic_duration" 2
run_attack immutable-rollback-under-load immutable_rollback_action "" "$rollout_traffic_duration" 2
run_attack replica-loss-under-load stop_proxy_a recover_proxy_a
capture_configs recovered
assert_same_config recovered
curl_replica proxy-a /actuator/prometheus "$output_dir/proxy-a-metrics.prom"
curl_replica proxy-b /actuator/prometheus "$output_dir/proxy-b-metrics.prom"
for metrics in "$output_dir/proxy-a-metrics.prom" "$output_dir/proxy-b-metrics.prom"; do
    for signal in lbp_proxy_requests_total lbp_proxy_inflight lbp_proxy_retries_total process_cpu_usage jvm_memory_used_bytes; do
        grep -q "^$signal" "$metrics" || { echo "Missing per-instance metric $signal" >&2; exit 1; }
    done
    ! grep -Fq -f "$api_key_file" "$metrics" || { echo "API key leaked into topology metrics" >&2; exit 1; }
done

docker info --format '{{json .}}' > "$output_dir/docker-info.json"
docker image inspect "$baseline_image_id" > "$output_dir/proxy-baseline-image.json"
docker image inspect "$candidate_image_id" > "$output_dir/proxy-candidate-image.json"
docker image inspect "$rejected_image_id" > "$output_dir/proxy-rejected-image.json"
docker image inspect "$LBP_TOPOLOGY_INGRESS_IMAGE" > "$output_dir/ingress-image.json"
docker container inspect "$(replica_container_id proxy-a)" > "$output_dir/proxy-a-container.json"
docker container inspect "$(replica_container_id proxy-b)" > "$output_dir/proxy-b-container.json"
assert_replica_image proxy-a "$baseline_image_id"
assert_replica_image proxy-b "$baseline_image_id"
jq -n --arg baselineImageId "$baseline_image_id" --arg candidateImageId "$candidate_image_id" \
  --arg rejectedImageId "$rejected_image_id" --arg candidateReleaseId "$candidate_release_id" \
  '{schemaVersion:1,identityType:"local Docker content-addressed image ID",
    baseline:{imageId:$baselineImageId},candidate:{releaseId:$candidateReleaseId,imageId:$candidateImageId},
    deliberatelyRejectedCandidate:{imageId:$rejectedImageId},
    applicationLayersIdentical:true,
    boundary:"Local image IDs prove replacement mechanics; a registry manifest digest is still required for reviewed staging."}' \
  > "$output_dir/image-identities.json"
profile_sha256="$(sha256sum "$profile" | awk '{print $1}')"
source_revision="$(git -C "$repo_root" rev-parse HEAD 2>/dev/null || echo unknown)"
jq -n --arg profileId "$profile_id" --arg profileSha256 "$profile_sha256" \
  --arg sourceRevision "$source_revision" --arg mode "$mode" \
  '{schemaVersion:1,profileId:$profileId,profileSha256:$profileSha256,
    sourceRevision:$sourceRevision,mode:$mode,
    boundary:"Two proxy processes behind a loopback TLS ingress fixture; not multi-zone or production-ingress evidence."}' \
  > "$output_dir/run-metadata.json"
jq -n --slurpfile distribution "$output_dir/distribution.json" \
  --slurpfile aggregate "$output_dir/aggregate-limit.json" --slurpfile rollout "$output_dir/rollout.json" \
  --slurpfile rollback "$output_dir/rollback.json" --slurpfile recovery "$output_dir/recovery.json" \
  --slurpfile candidateRejection "$output_dir/candidate-rejection.json" \
  --slurpfile immutableRollout "$output_dir/immutable-rollout.json" \
  --slurpfile immutableRollback "$output_dir/immutable-rollback.json" \
  --slurpfile rolloutClient "$output_dir/rollout-under-load-client.json" \
  --slurpfile rollbackClient "$output_dir/rollback-under-load-client.json" \
  --slurpfile rejectionClient "$output_dir/candidate-rejection-under-load-client.json" \
  --slurpfile immutableRolloutClient "$output_dir/immutable-rollout-under-load-client.json" \
  --slurpfile immutableRollbackClient "$output_dir/immutable-rollback-under-load-client.json" \
  --slurpfile lossClient "$output_dir/replica-loss-under-load-client.json" \
  --argjson minimumSuccess "$minimum_success" --argjson maximumP99Millis "$p99_budget" '
  {accepted:($distribution[0].passed and $aggregate[0].passed and $rollout[0].passed
      and $rollback[0].passed and $candidateRejection[0].passed and $immutableRollout[0].passed
      and $immutableRollback[0].passed and $recovery[0].passed
      and all([$rolloutClient[0],$rollbackClient[0],$rejectionClient[0],$immutableRolloutClient[0],
          $immutableRollbackClient[0],$lossClient[0]][];
        .success >= $minimumSuccess and (.latencies["99th"] / 1000000) <= $maximumP99Millis)),
   distribution:$distribution[0],aggregateLimits:$aggregate[0],rollout:$rollout[0],rollback:$rollback[0],
   candidateRejection:$candidateRejection[0],immutableRollout:$immutableRollout[0],
   immutableRollback:$immutableRollback[0],recovery:$recovery[0],
   trafficObjectives:{minimumSuccessRatio:$minimumSuccess,maximumP99Millis:$maximumP99Millis},
   traffic:{
     rollout:{successRatio:$rolloutClient[0].success,p99Millis:($rolloutClient[0].latencies["99th"] / 1000000)},
     rollback:{successRatio:$rollbackClient[0].success,p99Millis:($rollbackClient[0].latencies["99th"] / 1000000)},
     candidateRejection:{successRatio:$rejectionClient[0].success,p99Millis:($rejectionClient[0].latencies["99th"] / 1000000)},
     immutableRollout:{successRatio:$immutableRolloutClient[0].success,p99Millis:($immutableRolloutClient[0].latencies["99th"] / 1000000)},
     immutableRollback:{successRatio:$immutableRollbackClient[0].success,p99Millis:($immutableRollbackClient[0].latencies["99th"] / 1000000)},
     replicaLoss:{successRatio:$lossClient[0].success,p99Millis:($lossClient[0].latencies["99th"] / 1000000)}},
   boundary:"Local active-active and content-addressed image replacement mechanics only; registry-digest, reviewed deployment ingress, and multi-zone runs remain required."}' \
  > "$output_dir/topology-result.json"
jq -e '.accepted == true' "$output_dir/topology-result.json" >/dev/null
printf 'Active-active topology evidence: %s\n' "$output_dir"
