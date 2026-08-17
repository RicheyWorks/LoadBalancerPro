#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
staging_example="$script_dir/staging-profile.example.json"
capacity_example="$script_dir/staging-capacity-profile.example.json"
staging_validator="$script_dir/validate-staging-target.py"
capacity_validator="$script_dir/validate-staging-capacity.py"
deployment_validator="$script_dir/validate-staging-deployment.py"
sample_validator="$script_dir/validate-staging-capacity-sample.py"
result_evaluator="$script_dir/evaluate-staging-capacity.py"
target_renderer="$script_dir/render-capacity-targets.jq"

mode=validate
staging_profile="$staging_example"
capacity_profile="$capacity_example"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --mode) [[ $# -ge 2 ]] || { echo "--mode requires validate or run" >&2; exit 2; }; mode="$2"; shift 2 ;;
        --staging-profile) [[ $# -ge 2 ]] || { echo "--staging-profile requires a JSON file" >&2; exit 2; }; staging_profile="$2"; shift 2 ;;
        --capacity-profile) [[ $# -ge 2 ]] || { echo "--capacity-profile requires a JSON file" >&2; exit 2; }; capacity_profile="$2"; shift 2 ;;
        --help|-h)
            echo "Usage: $0 --mode validate|run [--staging-profile staging.json] [--capacity-profile capacity.json]"
            exit 0
            ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
done
[[ "$mode" == "validate" || "$mode" == "run" ]] || { echo "Mode must be validate or run" >&2; exit 2; }
for required_file in "$staging_profile" "$capacity_profile" "$staging_validator" "$capacity_validator" \
    "$deployment_validator" "$sample_validator" "$result_evaluator" "$target_renderer"; do
    [[ -f "$required_file" ]] || { echo "Missing required file: $required_file" >&2; exit 2; }
done

python_command=""
for candidate in python3 python; do
    if command -v "$candidate" >/dev/null 2>&1 \
        && "$candidate" -c 'import sys; raise SystemExit(sys.version_info < (3, 9))' >/dev/null 2>&1; then
        python_command="$candidate"
        break
    fi
done
[[ -n "$python_command" ]] || { echo "Python 3.9 or newer is required" >&2; exit 2; }
command -v jq >/dev/null 2>&1 || { echo "jq is required" >&2; exit 2; }

"$python_command" "$staging_validator" --profile "$staging_profile" >/dev/null
"$python_command" "$capacity_validator" --staging-profile "$staging_profile" \
    --capacity-profile "$capacity_profile" >/dev/null
if [[ "$mode" == "validate" ]]; then
    jq -c --arg baseUrl 'https://127.0.0.1:18445' --arg apiKey validation-only \
        -f "$target_renderer" "$capacity_profile" \
        | jq -s -e 'length == 100 and all(.[];
            (.url | startswith("https://127.0.0.1:18445/proxy/"))
            and (.header["X-API-Key"] == ["validation-only"]))' >/dev/null
    printf 'Validated deployment-equivalent capacity profile %s without DNS resolution or traffic.\n' \
        "$(jq -r '.profileId' "$capacity_profile")"
    printf 'Validated exact staging-profile binding, workload parity, rate ladder, replica telemetry, and rollback inputs.\n'
    printf 'Run mode remains disabled until both reviews and all external secret, trust, action, and sampler hashes are present.\n'
    exit 0
fi

if ! contract_json="$("$python_command" "$capacity_validator" --staging-profile "$staging_profile" \
    --capacity-profile "$capacity_profile" --execution)"; then
    exit 2
fi

jq -e '
  .review.status == "reviewed"
  and (.review.approvedBy | type == "string" and length > 0 and (test("replace|example|todo"; "i") | not))
  and (.review.approvedAt | fromdateiso8601 > 0 and . <= now)
  and .environment.billableImpactReviewed == true
  and .environment.productionTrafficAuthorized == false
  and (.environment.cleanupAuthority | type == "string" and length > 0 and (test("replace|example|todo"; "i") | not))
  and (.environment.changeTicket | type == "string" and length > 0 and (test("replace|example|todo"; "i") | not))
  and (.artifact.registryRepository | test("invalid|replace|example|todo"; "i") | not)
  and (.artifact.prior.imageDigest != ("sha256:" + ("0" * 64)))
  and (.artifact.candidate.imageDigest != ("sha256:" + ("1" * 64)))
  and (.deployment.ingressIdentitySha256 != ("0" * 64))
  and (.deployment.configurationSha256 != ("0" * 64))
  and all(.hooks[]; . != ("0" * 64))
' "$staging_profile" >/dev/null || {
    echo "Run mode requires reviewed staging authority and non-placeholder artifact, deployment, and hook identities" >&2
    exit 2
}

git_revision="$(git -C "$repo_root" rev-parse HEAD 2>/dev/null || echo unknown)"
[[ "$git_revision" == "$(jq -r '.artifact.candidate.sourceRevision' "$staging_profile")" ]] || {
    echo "The reviewed staging candidate sourceRevision must match the clean runner checkout" >&2
    exit 2
}
[[ -z "$(git -C "$repo_root" status --porcelain 2>/dev/null)" ]] || {
    echo "Run mode requires a clean checkout" >&2
    exit 2
}

for command_name in awk curl find jq openssl realpath sha256sum stat timeout vegeta; do
    command -v "$command_name" >/dev/null 2>&1 || { echo "$command_name is required" >&2; exit 2; }
done

api_key_file="${LBP_STAGING_API_KEY_FILE:-}"
ca_file="${LBP_STAGING_CA_FILE:-}"
action_dir="${LBP_STAGING_ACTION_DIR:-}"
sampler_file="${LBP_STAGING_CAPACITY_SAMPLER:-}"
for required_path in "$api_key_file" "$ca_file" "$action_dir" "$sampler_file"; do
    [[ -n "$required_path" ]] || {
        rm -f -- "$contract_file"
        echo "Staging API key, CA, action directory, and capacity sampler must be supplied through environment variables" >&2
        exit 2
    }
done
[[ -f "$api_key_file" && ! -L "$api_key_file" ]] || { echo "LBP_STAGING_API_KEY_FILE must be a regular non-symlink file" >&2; exit 2; }
[[ -s "$ca_file" && ! -L "$ca_file" ]] || { echo "LBP_STAGING_CA_FILE must be a non-empty regular non-symlink file" >&2; exit 2; }
[[ -d "$action_dir" && ! -L "$action_dir" ]] || { echo "LBP_STAGING_ACTION_DIR must be a non-symlink directory" >&2; exit 2; }
[[ -f "$sampler_file" && ! -L "$sampler_file" && -x "$sampler_file" ]] || {
    echo "LBP_STAGING_CAPACITY_SAMPLER must be an executable regular non-symlink file" >&2
    exit 2
}

repo_resolved="$(cd "$repo_root" && pwd -P)"
api_key_file="$(cd "$(dirname "$api_key_file")" && pwd -P)/$(basename "$api_key_file")"
ca_file="$(cd "$(dirname "$ca_file")" && pwd -P)/$(basename "$ca_file")"
action_dir="$(cd "$action_dir" && pwd -P)"
sampler_file="$(cd "$(dirname "$sampler_file")" && pwd -P)/$(basename "$sampler_file")"
for external_path in "$api_key_file" "$ca_file" "$action_dir" "$sampler_file"; do
    case "$external_path/" in
        "$repo_resolved"/*) echo "Staging secrets, trust, action adapters, and sampler must remain outside the repository" >&2; exit 2 ;;
    esac
done
api_key_mode="$(stat -c '%a' "$api_key_file")"
[[ "$api_key_mode" =~ ^[0-7]00$ ]] || { echo "Staging API-key file must deny group and other access" >&2; exit 2; }
action_dir_mode="$(stat -c '%a' "$action_dir")"
(( (8#$action_dir_mode & 0022) == 0 )) || { echo "Staging action directory must not be group/other writable" >&2; exit 2; }
sampler_mode="$(stat -c '%a' "$sampler_file")"
(( (8#$sampler_mode & 0022) == 0 )) || { echo "Capacity sampler must not be group/other writable" >&2; exit 2; }
actual_sampler_sha="$(sha256sum "$sampler_file" | awk '{print $1}')"
expected_sampler_sha="$(jq -r '.telemetry.samplerSha256' "$capacity_profile")"
[[ "$actual_sampler_sha" == "$expected_sampler_sha" ]] || { echo "Capacity sampler hash does not match the reviewed profile" >&2; exit 2; }

profile_id="$(jq -r '.profileId' "$capacity_profile")"
default_run_id="$(date -u +%Y%m%dT%H%M%SZ)-${BASHPID}"
output_dir="${LBP_STAGING_CAPACITY_OUTPUT_DIR:-$repo_root/target/staging-capacity/$profile_id/$default_run_id}"
umask 077
mkdir -p "$repo_root/target"
target_root="$(cd "$repo_root/target" && pwd -P)"
[[ "$output_dir" == /* ]] || output_dir="$repo_root/$output_dir"
output_dir="$(realpath -m -- "$output_dir")"
case "$output_dir/" in
    "$target_root"/*) ;;
    *) echo "LBP_STAGING_CAPACITY_OUTPUT_DIR must remain beneath repository target/" >&2; exit 2 ;;
esac
mkdir -p "$output_dir"
output_dir="$(cd "$output_dir" && pwd -P)"
[[ -z "$(find "$output_dir" -mindepth 1 -print -quit)" ]] || { echo "Staging capacity evidence directory must be empty" >&2; exit 2; }
printf '%s\n' "$contract_json" > "$output_dir/validated-contract.json"
chmod 0400 "$output_dir/validated-contract.json"

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-staging-capacity.XXXXXX")"
verified_hook_dir="$work_dir/verified-hooks"
trusted_ca="$work_dir/staging-ca.pem"
runtime_api_key_file="$work_dir/api-key"
verified_sampler="$work_dir/capacity-sampler.sh"
mkdir -m 0700 "$verified_hook_dir"
cp -- "$ca_file" "$trusted_ca"
cp -- "$sampler_file" "$verified_sampler"
chmod 0400 "$trusted_ca"
chmod 0500 "$verified_sampler"
[[ "$(sha256sum "$verified_sampler" | awk '{print $1}')" == "$expected_sampler_sha" ]] || {
    echo "Capacity sampler changed while it was pinned" >&2
    exit 2
}

attack_pid=""
sample_pid=""
needs_reset=false
needs_rollback=false
hooks_verified=false
recovery_seconds="$(jq -r '.thresholds.recoveryWindowSeconds' "$staging_profile")"
rollback_seconds="$(jq -r '.deployment.rollout.maximumRollbackSeconds' "$staging_profile")"
cleanup() {
    local status=$?
    trap - EXIT
    if [[ -n "$attack_pid" ]]; then kill "$attack_pid" >/dev/null 2>&1 || true; wait "$attack_pid" >/dev/null 2>&1 || true; fi
    if [[ -n "$sample_pid" ]]; then kill "$sample_pid" >/dev/null 2>&1 || true; wait "$sample_pid" >/dev/null 2>&1 || true; fi
    if [[ "$needs_reset" == "true" && "$hooks_verified" == "true" ]]; then
        timeout --foreground "${recovery_seconds}s" "$verified_hook_dir/reset.sh" >/dev/null 2>&1 || true
    fi
    if [[ "$needs_rollback" == "true" && "$hooks_verified" == "true" ]]; then
        timeout --foreground "${rollback_seconds}s" "$verified_hook_dir/rollback-prior.sh" >/dev/null 2>&1 || true
    fi
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-staging-capacity.*) rm -rf -- "$work_dir" ;;
        *) echo "Refusing to remove unexpected staging capacity path: $work_dir" >&2 ;;
    esac
    exit "$status"
}
trap cleanup EXIT

hook_names=(verify-deployment rollout-candidate rollback-prior slow failure drain restart reset)
for hook_name in "${hook_names[@]}"; do
    hook_path="$action_dir/$hook_name.sh"
    [[ -f "$hook_path" && ! -L "$hook_path" && -x "$hook_path" ]] || {
        echo "Missing executable non-symlink staging hook: $hook_path" >&2
        exit 2
    }
    hook_mode="$(stat -c '%a' "$hook_path")"
    (( (8#$hook_mode & 0022) == 0 )) || { echo "Staging hook must not be group/other writable: $hook_path" >&2; exit 2; }
    actual_hash="$(sha256sum "$hook_path" | awk '{print $1}')"
    expected_hash="$(jq -r --arg name "$hook_name" '.hooks[$name]' "$staging_profile")"
    [[ "$actual_hash" == "$expected_hash" ]] || { echo "Hash mismatch for staging hook $hook_name" >&2; exit 2; }
    cp -- "$hook_path" "$verified_hook_dir/$hook_name.sh"
    chmod 0500 "$verified_hook_dir/$hook_name.sh"
    [[ "$(sha256sum "$verified_hook_dir/$hook_name.sh" | awk '{print $1}')" == "$expected_hash" ]] || {
        echo "Staging hook changed while it was pinned: $hook_name" >&2
        exit 2
    }
done
hooks_verified=true

resolution_file="$output_dir/resolved-target.json"
"$python_command" "$staging_validator" --profile "$staging_profile" --resolve --output "$resolution_file"
host="$(jq -r '.host' "$resolution_file")"
port="$(jq -r '.port' "$resolution_file")"
pinned_address="$(jq -r '.pinnedAddress' "$resolution_file")"
tls_server_name="$(jq -r '.target.tlsServerName' "$staging_profile")"
curl_address="$pinned_address"
connect_address="$pinned_address:$port"
if [[ "$pinned_address" == *:* ]]; then curl_address="[$pinned_address]"; connect_address="[$pinned_address]:$port"; fi
base_url="https://$tls_server_name:$port"
api_key="$(<"$api_key_file")"
[[ "$api_key" =~ ^[A-Za-z0-9._~+/=-]{20,512}$ ]] || { echo "Staging API key must be 20-512 header-safe characters" >&2; exit 2; }
printf '%s' "$api_key" > "$runtime_api_key_file"
chmod 0400 "$runtime_api_key_file"
curl_config="$work_dir/curl-auth.conf"
printf 'header = "X-API-Key: %s"\n' "$api_key" > "$curl_config"
curl_common=(--silent --show-error --fail --cacert "$trusted_ca" --resolve "$tls_server_name:$port:$curl_address"
    --connect-timeout 5 --max-time 15 --config "$curl_config")
vegeta_connect_to="$tls_server_name:$port:$curl_address:$port"

repository="$(jq -r '.artifact.registryRepository' "$staging_profile")"
prior_digest="$(jq -r '.artifact.prior.imageDigest' "$staging_profile")"
candidate_digest="$(jq -r '.artifact.candidate.imageDigest' "$staging_profile")"
export LBP_STAGING_PRIOR_IMAGE_REFERENCE="$repository@$prior_digest"
export LBP_STAGING_CANDIDATE_IMAGE_REFERENCE="$repository@$candidate_digest"
export LBP_STAGING_PRIOR_SOURCE_REVISION="$(jq -r '.artifact.prior.sourceRevision' "$staging_profile")"
export LBP_STAGING_CANDIDATE_SOURCE_REVISION="$(jq -r '.artifact.candidate.sourceRevision' "$staging_profile")"
export LBP_STAGING_ATTESTATION_SCHEMA=1
export LBP_STAGING_CHANGE_TICKET="$(jq -r '.environment.changeTicket' "$staging_profile")"
export LBP_STAGING_TARGET_HOST="$host"
export LBP_STAGING_TARGET_PORT="$port"
export LBP_STAGING_EXPECTED_STRATEGY="$(jq -r '.workload.routeMix[0].strategy' "$staging_profile")"
export LBP_STAGING_REVIEWED_PROFILE="$staging_profile"
export LBP_STAGING_CAPACITY_PROFILE="$capacity_profile"
export LBP_STAGING_CAPACITY_EXPECTED_IMAGE_REFERENCE="$LBP_STAGING_CANDIDATE_IMAGE_REFERENCE"

run_hook() {
    local name="$1" maximum_seconds="${2:-$recovery_seconds}" started completed
    started="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    timeout --foreground "${maximum_seconds}s" "$verified_hook_dir/$name.sh"
    completed="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    jq -cn --arg name "$name" --arg startedAt "$started" --arg completedAt "$completed" \
        --arg sha256 "$(jq -r --arg name "$name" '.hooks[$name]' "$staging_profile")" \
        '{name:$name,sha256:$sha256,startedAt:$startedAt,completedAt:$completedAt,exitCode:0}' \
        >> "$output_dir/hook-executions.jsonl"
}

run_attested_hook() {
    local name="$1" phase="$2" snapshot_file="$3" maximum_seconds="$4"
    local raw_snapshot validation_file started completed
    raw_snapshot="$work_dir/$name-$phase-${BASHPID}.json"
    validation_file="${snapshot_file%.json}-validation.json"
    started="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    LBP_STAGING_EXPECTED_PHASE="$phase" timeout --foreground "${maximum_seconds}s" \
        "$verified_hook_dir/$name.sh" > "$raw_snapshot"
    [[ -s "$raw_snapshot" && "$(stat -c '%s' "$raw_snapshot")" -le 65536 ]] || {
        echo "Staging hook $name must emit one bounded deployment snapshot" >&2
        exit 1
    }
    cp -- "$raw_snapshot" "$snapshot_file"
    chmod 0400 "$snapshot_file"
    "$python_command" "$deployment_validator" --profile "$staging_profile" --snapshot "$snapshot_file" \
        --phase "$phase" --output "$validation_file"
    chmod 0400 "$validation_file"
    completed="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    jq -cn --arg name "$name" --arg phase "$phase" --arg startedAt "$started" --arg completedAt "$completed" \
        --arg sha256 "$(jq -r --arg name "$name" '.hooks[$name]' "$staging_profile")" \
        --arg snapshotSha256 "$(sha256sum "$snapshot_file" | awk '{print $1}')" \
        '{name:$name,phase:$phase,sha256:$sha256,startedAt:$startedAt,completedAt:$completedAt,exitCode:0,
          deploymentSnapshotValidated:true,snapshotSha256:$snapshotSha256}' >> "$output_dir/hook-executions.jsonl"
}

wait_for_health() {
    local deadline=$(( SECONDS + recovery_seconds ))
    while (( SECONDS < deadline )); do
        if curl "${curl_common[@]}" --output /dev/null "$base_url/actuator/health"; then return 0; fi
        sleep 2
    done
    echo "Staging target did not recover inside the reviewed recovery window" >&2
    return 1
}

write_targets() {
    local destination="$1"
    jq -c --arg baseUrl "$base_url" --rawfile apiKey "$runtime_api_key_file" \
        -f "$target_renderer" "$capacity_profile" > "$destination"
}

capture_sample() {
    local raw_file="$1" validation_file="${1%.json}-validation.json"
    local sampler_seconds
    sampler_seconds="$(jq -r '.telemetry.maximumSamplerSeconds' "$capacity_profile")"
    LBP_STAGING_CAPACITY_EXPECTED_PHASE=candidate timeout --foreground "${sampler_seconds}s" \
        "$verified_sampler" > "$raw_file"
    [[ -s "$raw_file" && "$(stat -c '%s' "$raw_file")" -le 131072 ]] || {
        echo "Capacity sampler must emit one bounded telemetry snapshot" >&2
        return 1
    }
    chmod 0400 "$raw_file"
    "$python_command" "$sample_validator" --staging-profile "$staging_profile" \
        --capacity-profile "$capacity_profile" --sample "$raw_file" --output "$validation_file"
    chmod 0400 "$validation_file"
}

sample_loop() {
    local samples_dir="$1" stop_file="$2" sequence=1 interval
    interval="$(jq -r '.capacity.sampleIntervalSeconds' "$capacity_profile")"
    while [[ ! -e "$stop_file" ]]; do
        capture_sample "$samples_dir/sample-$(printf '%04d' "$sequence").json"
        sequence=$(( sequence + 1 ))
        sleep "$interval"
    done
}

run_transition() {
    local scenario="$1" hook_name="$2" phase="$3" snapshot_file="$4"
    local duration rate p99_budget minimum_success transition_seconds targets_file results_file report_file
    duration="$(jq -r --arg name "$scenario" '.scenarios[$name].durationSeconds' "$staging_profile")"
    rate="$(jq -r --arg name "$scenario" '.scenarios[$name].ratePerSecond' "$staging_profile")"
    p99_budget="$(jq -r --arg name "$scenario" '.scenarios[$name].p99Millis' "$staging_profile")"
    minimum_success="$(jq -r --arg name "$scenario" '.scenarios[$name].minimumSuccessRatio' "$staging_profile")"
    transition_seconds="$(jq -r --arg phase "$phase" '
      if $phase == "candidate" then .deployment.rollout.maximumRolloutSeconds
      else .deployment.rollout.maximumRollbackSeconds end' "$staging_profile")"
    targets_file="$work_dir/$scenario.targets"
    results_file="$output_dir/$scenario-client.bin"
    report_file="$output_dir/$scenario-client.json"
    write_targets "$targets_file"
    vegeta attack -duration="${duration}s" -rate="${rate}/s" -timeout=10s \
        -connections="$(jq -r '.workload.concurrency.clientConnections' "$capacity_profile")" \
        -max-connections="$(jq -r '.workload.concurrency.clientConnections' "$capacity_profile")" \
        -keepalive="$(jq -r '.workload.concurrency.keepAlive' "$capacity_profile")" \
        -root-certs="$trusted_ca" -connect-to="$vegeta_connect_to" -max-body=0 \
        -format=json -targets="$targets_file" > "$results_file" &
    attack_pid=$!
    sleep 5
    if [[ "$phase" == "candidate" ]]; then needs_rollback=true; fi
    run_attested_hook "$hook_name" "$phase" "$snapshot_file" "$transition_seconds"
    wait_for_health
    kill -0 "$attack_pid" 2>/dev/null || {
        wait "$attack_pid" || true
        attack_pid=""
        echo "$scenario traffic ended before the reviewed deployment transition completed" >&2
        exit 1
    }
    wait "$attack_pid" || { attack_pid=""; echo "$scenario traffic attack failed" >&2; exit 1; }
    attack_pid=""
    vegeta report -type=json "$results_file" > "$report_file"
    vegeta report -type=text "$results_file" > "$output_dir/$scenario-client.txt"
    jq -e --argjson p99Budget "$p99_budget" --argjson minimumSuccess "$minimum_success" '
      (.latencies["99th"] / 1000000) <= $p99Budget and .success >= $minimumSuccess
    ' "$report_file" >/dev/null || { echo "$scenario exceeded reviewed transition objectives" >&2; exit 1; }
    if [[ "$phase" == "rollback" ]]; then needs_rollback=false; fi
}

run_attested_hook verify-deployment prior "$output_dir/deployment-prior.json" "$recovery_seconds"
wait_for_health
run_transition candidateRollout rollout-candidate candidate "$output_dir/deployment-candidate.json"

curl "${curl_common[@]}" --output "$output_dir/candidate-proxy-config.json" "$base_url/api/proxy/config"
jq -e --slurpfile reviewed "$capacity_profile" '
  . as $configured
  | $reviewed[0].workload.routeMix
  | all(.[]; . as $workloadRoute
      | any($configured.routes[];
          . as $configuredRoute
          | ($workloadRoute.path | startswith($configuredRoute.pathPrefix))
            and $configuredRoute.strategy == $workloadRoute.strategy
            and $configuredRoute.hostMatch == null
            and ($configuredRoute.headerMatchNames | length == 0)))
' "$output_dir/candidate-proxy-config.json" >/dev/null || {
    echo "The candidate deployment does not implement the reviewed capacity route strategy" >&2
    exit 1
}

initial_sample="$output_dir/candidate-initial-sample.json"
export LBP_STAGING_CAPACITY_SCENARIO=initial
export LBP_STAGING_CAPACITY_RATE_PER_SECOND=0
export LBP_STAGING_CAPACITY_REPEAT=0
capture_sample "$initial_sample"
last_sample_validation="${initial_sample%.json}-validation.json"
staging_profile_sha="$(sha256sum "$staging_profile" | awk '{print $1}')"
capacity_profile_sha="$(sha256sum "$capacity_profile" | awk '{print $1}')"
ca_sha="$(sha256sum "$trusted_ca" | awk '{print $1}')"
jq -n --arg profileId "$profile_id" --arg stagingProfileSha256 "$staging_profile_sha" \
  --arg capacityProfileSha256 "$capacity_profile_sha" --arg sourceRevision "$git_revision" \
  --arg candidateImageReference "$LBP_STAGING_CANDIDATE_IMAGE_REFERENCE" \
  --arg caSha256 "$ca_sha" --arg samplerSha256 "$expected_sampler_sha" \
  --arg targetBoundary "reviewed HTTPS staging target pinned to private DNS resolution" \
  --argjson rates "$(jq '.capacity.ratesPerSecond' "$capacity_profile")" \
  --argjson repeats "$(jq '.capacity.repeatsPerStep' "$capacity_profile")" \
  '{schemaVersion:1,profileId:$profileId,stagingProfileSha256:$stagingProfileSha256,
    capacityProfileSha256:$capacityProfileSha256,sourceRevision:$sourceRevision,
    candidateImageReference:$candidateImageReference,caSha256:$caSha256,samplerSha256:$samplerSha256,
    targetBoundary:$targetBoundary,ratesPerSecond:$rates,repeatsPerStep:$repeats}' \
  > "$output_dir/run-metadata.json"

rates=()
mapfile -t rates < <(jq -r '.capacity.ratesPerSecond[]' "$capacity_profile")
repeats="$(jq -r '.capacity.repeatsPerStep' "$capacity_profile")"
warmup_seconds="$(jq -r '.capacity.warmupSeconds' "$capacity_profile")"
measurement_seconds="$(jq -r '.capacity.measurementSeconds' "$capacity_profile")"
cooldown_seconds="$(jq -r '.capacity.cooldownSeconds' "$capacity_profile")"
client_connections="$(jq -r '.workload.concurrency.clientConnections' "$capacity_profile")"
keep_alive="$(jq -r '.workload.concurrency.keepAlive' "$capacity_profile")"
minimum_throughput_ratio="$(jq -r '.capacity.minimumThroughputRatio' "$capacity_profile")"
minimum_success_ratio="$(jq -r '.workload.objectives.minimumSuccessRatio' "$capacity_profile")"
failure_minimum_success_ratio="$(jq -r '.capacity.failureCaseMinimumSuccessRatio' "$capacity_profile")"
maximum_retry_ratio="$(jq -r '.capacity.maximumNonInjectedRetryRatio' "$capacity_profile")"
minimum_metric_coverage="$(jq -r '.capacity.minimumRequestMetricCoverageRatio' "$capacity_profile")"
maximum_metric_overcount="$(jq -r '.capacity.maximumRequestMetricOvercountRatio' "$capacity_profile")"
normal_p99_ms="$(jq -r '.workload.objectives.normalP99Millis' "$capacity_profile")"
slow_p99_ms="$(jq -r '.workload.objectives.slowP99Millis' "$capacity_profile")"
failure_p99_ms="$(jq -r '.workload.objectives.failureP99Millis' "$capacity_profile")"
max_cpu_ratio="$(jq -r '.capacity.maximumCpuUtilizationRatio' "$capacity_profile")"
max_memory_ratio="$(jq -r '.capacity.maximumMemoryUtilizationRatio' "$capacity_profile")"
max_gc_pause_seconds="$(jq -r '.capacity.maximumGcPauseSecondsPerMeasurement' "$capacity_profile")"
max_proxy_overhead="$(jq -r '.thresholds.maximumProxyOverheadP99Millis' "$staging_profile")"
max_inflight="$(jq -r '.capacity.maxInFlight' "$capacity_profile")"
max_connections="$(jq -r '.capacity.maximumOpenConnectionsPerReplica' "$capacity_profile")"
max_threads="$(jq -r '.capacity.maximumJvmLiveThreadsPerReplica' "$capacity_profile")"
measurements_file="$output_dir/measurements.jsonl"
: > "$measurements_file"

fresh_candidate_repeat() {
    local run_dir="$1" before_file="$last_sample_validation" after_raw after_validation
    export LBP_STAGING_CAPACITY_SCENARIO=fresh-repeat
    export LBP_STAGING_CAPACITY_RATE_PER_SECOND=0
    export LBP_STAGING_CAPACITY_REPEAT=0
    run_hook restart "$recovery_seconds"
    wait_for_health
    run_attested_hook verify-deployment candidate "$run_dir/deployment-after-restart.json" "$recovery_seconds"
    after_raw="$run_dir/fresh-candidate-sample.json"
    capture_sample "$after_raw"
    after_validation="${after_raw%.json}-validation.json"
    jq -n -e --slurpfile before "$before_file" --slurpfile after "$after_validation" '
      (($before[0].replicaIds - $after[0].replicaIds) | length) == ($before[0].replicaIds | length)
    ' >/dev/null || {
        echo "Restart hook did not replace every candidate replica with a fresh runtime identity" >&2
        exit 1
    }
    last_sample_validation="$after_validation"
}

run_warmup() {
    local rate="$1" run_dir="$2" targets_file="$work_dir/warmup.targets"
    write_targets "$targets_file"
    vegeta attack -duration="${warmup_seconds}s" -rate="${rate}/s" -timeout=10s \
        -connections="$client_connections" -max-connections="$client_connections" -keepalive="$keep_alive" \
        -root-certs="$trusted_ca" -connect-to="$vegeta_connect_to" -max-body=0 \
        -format=json -targets="$targets_file" > "$run_dir/warmup.bin"
    vegeta report -type=json "$run_dir/warmup.bin" > "$run_dir/warmup.json"
    capture_sample "$run_dir/warmup-after-sample.json"
}

run_scenario() {
    local scenario="$1" rate="$2" repeat="$3" p99_budget="$4" injected="$5"
    local before_hook="${6:-}" during_hook="${7:-}" after_hook="${8:-}"
    local scenario_dir="$9" targets_file results_file report_file samples_dir stop_file
    local resource_summary requests throughput success p50_ms p95_ms p99_ms throughput_ratio completion_ratio
    local requests_delta retries_delta sheds_delta limits_delta gc_pause_count_delta gc_pause_seconds_delta retry_ratio coverage_ratio
    local scenario_minimum_success case_pass saturated quiesced proxy_overhead final_raw
    scenario_dir="$(realpath -m -- "$scenario_dir")"
    mkdir -p "$scenario_dir"
    samples_dir="$scenario_dir/samples"
    mkdir -p "$samples_dir"
    stop_file="$scenario_dir/stop-sampler"
    targets_file="$work_dir/$scenario-$rate-$repeat.targets"
    results_file="$scenario_dir/client.bin"
    report_file="$scenario_dir/client.json"
    export LBP_STAGING_CAPACITY_SCENARIO="$scenario"
    export LBP_STAGING_CAPACITY_RATE_PER_SECOND="$rate"
    export LBP_STAGING_CAPACITY_REPEAT="$repeat"
    if [[ -n "$before_hook" ]]; then needs_reset=true; run_hook "$before_hook" "$recovery_seconds"; fi
    write_targets "$targets_file"
    rm -f -- "$stop_file"
    capture_sample "$samples_dir/sample-0000.json"
    sample_loop "$samples_dir" "$stop_file" &
    sample_pid=$!
    vegeta attack -duration="${measurement_seconds}s" -rate="${rate}/s" -timeout=10s \
        -connections="$client_connections" -max-connections="$client_connections" -keepalive="$keep_alive" \
        -root-certs="$trusted_ca" -connect-to="$vegeta_connect_to" -max-body=0 \
        -format=json -targets="$targets_file" > "$results_file" &
    attack_pid=$!
    if [[ -n "$during_hook" ]]; then
        sleep $(( measurement_seconds / 3 ))
        [[ "$during_hook" == "reset" ]] || needs_reset=true
        run_hook "$during_hook" "$recovery_seconds"
        [[ "$during_hook" != "reset" ]] || needs_reset=false
        kill -0 "$attack_pid" 2>/dev/null || {
            wait "$attack_pid" || true; attack_pid=""; echo "$scenario traffic ended before its reviewed action" >&2; exit 1;
        }
    fi
    wait "$attack_pid" || { attack_pid=""; echo "$scenario capacity attack failed" >&2; exit 1; }
    attack_pid=""
    if [[ -n "$after_hook" ]]; then
        run_hook "$after_hook" "$recovery_seconds"
        [[ "$after_hook" != "reset" ]] || needs_reset=false
    fi
    wait_for_health
    if [[ -n "$before_hook$during_hook$after_hook" ]]; then
        run_attested_hook verify-deployment candidate "$scenario_dir/deployment-after-actions.json" "$recovery_seconds"
    fi
    : > "$stop_file"
    wait "$sample_pid" || { sample_pid=""; echo "$scenario capacity sampler failed" >&2; exit 1; }
    sample_pid=""
    quiesced=false
    for _ in $(seq 1 "$recovery_seconds"); do
        final_raw="$samples_dir/quiescence-$(date -u +%s%N).json"
        capture_sample "$final_raw"
        if jq -e '.inflight == 0' "${final_raw%.json}-validation.json" >/dev/null; then quiesced=true; break; fi
        sleep 1
    done
    [[ "$quiesced" == "true" ]] || { echo "$scenario in-flight work did not quiesce" >&2; exit 1; }
    vegeta report -type=json "$results_file" > "$report_file"
    vegeta report -type=text "$results_file" > "$scenario_dir/client.txt"
    resource_summary="$(jq -s '
      sort_by(.observedAt) as $samples
      | {samples:($samples|length),stableReplicaSet:([$samples[].replicaIds]|unique|length == 1),
         maxCpuUtilizationRatio:([$samples[].maximumCpuUtilizationRatio]|max),
         maxMemoryUtilizationRatio:([$samples[].maximumMemoryUtilizationRatio]|max),
         maxOpenConnectionsPerReplica:([$samples[].maximumOpenConnectionsPerReplica]|max),
         maxJvmLiveThreadsPerReplica:([$samples[].maximumJvmLiveThreadsPerReplica]|max),
         maxInflight:([$samples[].inflight]|max),maxProxyP99Millis:([$samples[].proxyP99Millis]|max),
         maxUpstreamP99Millis:([$samples[].upstreamP99Millis]|max),
         firstCounters:$samples[0].counters,lastCounters:$samples[-1].counters,
         firstUpstreamRequests:$samples[0].upstreamRequestsTotal,
         lastUpstreamRequests:$samples[-1].upstreamRequestsTotal,
         counterMonotonic:($samples[-1].counters.requestsTotal >= $samples[0].counters.requestsTotal
           and $samples[-1].counters.retriesTotal >= $samples[0].counters.retriesTotal
           and $samples[-1].counters.shedsTotal >= $samples[0].counters.shedsTotal
           and $samples[-1].counters.limitRejectionsTotal >= $samples[0].counters.limitRejectionsTotal
           and $samples[-1].counters.gcPauseCountTotal >= $samples[0].counters.gcPauseCountTotal
           and $samples[-1].counters.gcPauseSecondsTotal >= $samples[0].counters.gcPauseSecondsTotal
           and ([$samples[0].upstreamRequestsTotal | to_entries[] | . as $entry
             | $samples[-1].upstreamRequestsTotal[$entry.key] >= $entry.value] | all))}
    ' "$samples_dir"/*-validation.json)"
    [[ "$(jq -r '.samples' <<<"$resource_summary")" -ge 3 ]] || { echo "$scenario produced fewer than three valid telemetry samples" >&2; exit 1; }
    requests="$(jq -r '.requests' "$report_file")"
    throughput="$(jq -r '.throughput' "$report_file")"
    success="$(jq -r '.success' "$report_file")"
    p50_ms="$(jq -r '.latencies["50th"] / 1000000' "$report_file")"
    p95_ms="$(jq -r '.latencies["95th"] / 1000000' "$report_file")"
    p99_ms="$(jq -r '.latencies["99th"] / 1000000' "$report_file")"
    throughput_ratio="$(awk -v actual="$throughput" -v offered="$rate" 'BEGIN { printf "%.9f", actual / offered }')"
    completion_ratio="$(awk -v count="$requests" -v offered="$rate" -v seconds="$measurement_seconds" 'BEGIN { printf "%.9f", count / (offered * seconds) }')"
    requests_delta="$(jq -r '.lastCounters.requestsTotal - .firstCounters.requestsTotal' <<<"$resource_summary")"
    retries_delta="$(jq -r '.lastCounters.retriesTotal - .firstCounters.retriesTotal' <<<"$resource_summary")"
    sheds_delta="$(jq -r '.lastCounters.shedsTotal - .firstCounters.shedsTotal' <<<"$resource_summary")"
    limits_delta="$(jq -r '.lastCounters.limitRejectionsTotal - .firstCounters.limitRejectionsTotal' <<<"$resource_summary")"
    gc_pause_count_delta="$(jq -r '.lastCounters.gcPauseCountTotal - .firstCounters.gcPauseCountTotal' <<<"$resource_summary")"
    gc_pause_seconds_delta="$(jq -r '.lastCounters.gcPauseSecondsTotal - .firstCounters.gcPauseSecondsTotal' <<<"$resource_summary")"
    retry_ratio="$(awk -v retries="$retries_delta" -v count="$requests" 'BEGIN { printf "%.9f", count > 0 ? retries / count : 1 }')"
    coverage_ratio="$(awk -v observed="$requests_delta" -v count="$requests" 'BEGIN { printf "%.9f", count > 0 ? observed / count : 0 }')"
    scenario_minimum_success="$minimum_success_ratio"
    [[ "$injected" == "false" ]] || scenario_minimum_success="$failure_minimum_success_ratio"
    proxy_overhead="$(awk -v client="$p99_ms" -v upstream="$(jq -r '.maxUpstreamP99Millis' <<<"$resource_summary")" 'BEGIN { value=client-upstream; printf "%.6f", value > 0 ? value : 0 }')"
    case_pass="$(jq -nr --argjson injected "$injected" --argjson throughputRatio "$throughput_ratio" \
      --argjson completionRatio "$completion_ratio" --argjson minimumThroughput "$minimum_throughput_ratio" \
      --argjson success "$success" --argjson minimumSuccess "$scenario_minimum_success" \
      --argjson p99 "$p99_ms" --argjson p99Budget "$p99_budget" --argjson retryRatio "$retry_ratio" \
      --argjson maximumRetry "$maximum_retry_ratio" --argjson sheds "$sheds_delta" --argjson limits "$limits_delta" \
      --argjson coverage "$coverage_ratio" --argjson minimumCoverage "$minimum_metric_coverage" \
      --argjson maximumOvercount "$maximum_metric_overcount" --argjson quiesced "$quiesced" \
      --argjson proxyOverhead "$proxy_overhead" --argjson maximumProxyOverhead "$max_proxy_overhead" \
      --argjson gcPauseSeconds "$gc_pause_seconds_delta" --argjson maximumGcPauseSeconds "$max_gc_pause_seconds" \
      --argjson resources "$resource_summary" --argjson maxCpu "$max_cpu_ratio" --argjson maxMemory "$max_memory_ratio" \
      --argjson maxInflight "$max_inflight" --argjson maxConnections "$max_connections" --argjson maxThreads "$max_threads" '
      ((if $injected then $completionRatio else $throughputRatio end) >= $minimumThroughput)
      and ($success >= $minimumSuccess) and ($p99 <= $p99Budget)
      and ($injected or $retryRatio <= $maximumRetry) and ($sheds == 0) and ($limits == 0)
      and ($coverage >= $minimumCoverage and $coverage <= $maximumOvercount)
      and ($proxyOverhead <= $maximumProxyOverhead) and ($gcPauseSeconds <= $maximumGcPauseSeconds)
      and $quiesced and $resources.counterMonotonic and $resources.stableReplicaSet
      and ($resources.maxCpuUtilizationRatio <= $maxCpu)
      and ($resources.maxMemoryUtilizationRatio <= $maxMemory)
      and ($resources.maxInflight < $maxInflight)
      and ($resources.maxOpenConnectionsPerReplica <= $maxConnections)
      and ($resources.maxJvmLiveThreadsPerReplica <= $maxThreads)
    ')"
    saturated=false
    if [[ "$injected" == "false" && "$case_pass" != "true" ]]; then saturated=true; fi
    jq -n --arg scenario "$scenario" --argjson rate "$rate" --argjson repeat "$repeat" \
      --argjson injected "$injected" --argjson casePassed "$case_pass" --argjson saturated "$saturated" \
      --argjson requests "$requests" --argjson achievedThroughput "$throughput" \
      --argjson throughputRatio "$throughput_ratio" --argjson completionRatio "$completion_ratio" \
      --argjson successRatio "$success" --argjson p50Millis "$p50_ms" --argjson p95Millis "$p95_ms" \
      --argjson p99Millis "$p99_ms" --argjson p99BudgetMillis "$p99_budget" \
      --argjson statusCodes "$(jq '.status_codes' "$report_file")" --argjson errors "$(jq '.errors' "$report_file")" \
      --argjson requestsDelta "$requests_delta" --argjson retriesDelta "$retries_delta" \
      --argjson retryRatio "$retry_ratio" --argjson shedsDelta "$sheds_delta" \
      --argjson limitRejectionsDelta "$limits_delta" --argjson metricCoverageRatio "$coverage_ratio" \
      --argjson gcPauseCountDelta "$gc_pause_count_delta" --argjson gcPauseSecondsDelta "$gc_pause_seconds_delta" \
      --argjson proxyOverheadP99Millis "$proxy_overhead" --argjson resources "$resource_summary" \
      --argjson minimumSuccessRatio "$scenario_minimum_success" --argjson quiesced "$quiesced" \
      --argjson minimumThroughputRatio "$minimum_throughput_ratio" \
      --argjson maximumRetryRatio "$maximum_retry_ratio" \
      --argjson minimumMetricCoverage "$minimum_metric_coverage" \
      --argjson maximumMetricOvercount "$maximum_metric_overcount" \
      --argjson maximumCpuRatio "$max_cpu_ratio" --argjson maximumMemoryRatio "$max_memory_ratio" \
      --argjson maximumGcPauseSeconds "$max_gc_pause_seconds" --argjson maximumProxyOverhead "$max_proxy_overhead" \
      --argjson maximumInflight "$max_inflight" --argjson maximumConnections "$max_connections" \
      --argjson maximumThreads "$max_threads" '
      {scenario:$scenario,rate:$rate,repeat:$repeat,injectedFailure:$injected,casePassed:$casePassed,
       saturated:$saturated,client:{requests:$requests,achievedThroughput:$achievedThroughput,
         throughputRatio:$throughputRatio,completionRatio:$completionRatio,successRatio:$successRatio,
         p50Millis:$p50Millis,p95Millis:$p95Millis,p99Millis:$p99Millis,p99BudgetMillis:$p99BudgetMillis,
         statusCodes:$statusCodes,errors:$errors},
       deploymentMetrics:{requestsDelta:$requestsDelta,retriesDelta:$retriesDelta,retryRatio:$retryRatio,
         shedsDelta:$shedsDelta,limitRejectionsDelta:$limitRejectionsDelta,
         gcPauseCountDelta:$gcPauseCountDelta,gcPauseSecondsDelta:$gcPauseSecondsDelta,
         requestMetricCoverageRatio:$metricCoverageRatio,proxyOverheadP99Millis:$proxyOverheadP99Millis},
       runtime:$resources,minimumSuccessRatio:$minimumSuccessRatio,quiesced:$quiesced,
       saturationSignals:[
         if (if $injected then $completionRatio else $throughputRatio end) < $minimumThroughputRatio
           then "throughput" else empty end,
         if $successRatio < $minimumSuccessRatio then "success" else empty end,
         if $p99Millis > $p99BudgetMillis then "p99" else empty end,
         if (($injected | not) and $retryRatio > $maximumRetryRatio) then "retries" else empty end,
         if $shedsDelta > 0 then "sheds" else empty end,
         if $limitRejectionsDelta > 0 then "safety-limit" else empty end,
         if ($metricCoverageRatio < $minimumMetricCoverage or $metricCoverageRatio > $maximumMetricOvercount)
           then "metric-coverage" else empty end,
         if ($proxyOverheadP99Millis > $maximumProxyOverhead) then "proxy-overhead" else empty end,
         if ($gcPauseSecondsDelta > $maximumGcPauseSeconds) then "gc-pause" else empty end,
         if ($resources.maxCpuUtilizationRatio > $maximumCpuRatio) then "cpu" else empty end,
         if ($resources.maxMemoryUtilizationRatio > $maximumMemoryRatio) then "memory" else empty end,
         if ($resources.maxInflight >= $maximumInflight) then "max-inflight" else empty end,
         if ($resources.maxOpenConnectionsPerReplica > $maximumConnections) then "connections" else empty end,
         if ($resources.maxJvmLiveThreadsPerReplica > $maximumThreads) then "threads" else empty end,
         if ($resources.stableReplicaSet | not) then "replica-restart" else empty end,
         if ($resources.counterMonotonic | not) then "counter-regression" else empty end
       ]}
    ' > "$scenario_dir/result.json"
    jq -c . "$scenario_dir/result.json" >> "$measurements_file"
    last_sample_validation="${final_raw%.json}-validation.json"
}

stop_ladder=false
for rate in "${rates[@]}"; do
    saturated_repeats=0
    for repeat in $(seq 1 "$repeats"); do
        run_dir="$output_dir/rate-$rate/repeat-$repeat"
        mkdir -p "$run_dir"
        fresh_candidate_repeat "$run_dir"
        run_warmup "$rate" "$run_dir"
        run_scenario equal "$rate" "$repeat" "$normal_p99_ms" false '' '' '' "$run_dir/equal"
        run_scenario slow "$rate" "$repeat" "$slow_p99_ms" false '' slow reset "$run_dir/slow"
        run_scenario failing "$rate" "$repeat" "$failure_p99_ms" true '' failure reset "$run_dir/failing"
        run_scenario draining "$rate" "$repeat" "$normal_p99_ms" false '' drain reset "$run_dir/draining"
        run_scenario recovering "$rate" "$repeat" "$normal_p99_ms" false failure reset '' "$run_dir/recovering"
        sleep "$cooldown_seconds"
        jq -n --argjson seconds "$cooldown_seconds" --arg completedAt "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
            '{seconds:$seconds,completedAt:$completedAt,inflightQuiesced:true}' > "$run_dir/cooldown.json"
        repeat_saturated="$(jq -s --argjson rate "$rate" --argjson repeat "$repeat" \
            'any(.[]; .rate == $rate and .repeat == $repeat and .saturated)' "$measurements_file")"
        [[ "$repeat_saturated" != "true" ]] || saturated_repeats=$(( saturated_repeats + 1 ))
    done
    [[ "$saturated_repeats" -eq 0 ]] || stop_ladder=true
    [[ "$stop_ladder" == "false" ]] || break
done

run_transition priorRollback rollback-prior rollback "$output_dir/deployment-rollback.json"

"$python_command" - "$runtime_api_key_file" "$output_dir" <<'PY'
from pathlib import Path
import sys

secret = Path(sys.argv[1]).read_bytes()
for evidence in Path(sys.argv[2]).rglob("*"):
    if evidence.is_file() and secret in evidence.read_bytes():
        raise SystemExit(f"API key leaked into staging capacity evidence: {evidence}")
PY

"$python_command" "$result_evaluator" --staging-profile "$staging_profile" \
    --capacity-profile "$capacity_profile" --measurements "$measurements_file" \
    --prior-deployment "$output_dir/deployment-prior-validation.json" \
    --candidate-deployment "$output_dir/deployment-candidate-validation.json" \
    --rollback-deployment "$output_dir/deployment-rollback-validation.json" \
    --output "$output_dir/staging-capacity-result.json" \
    --repeat-summary-output "$output_dir/repeat-summary.json" \
    --rate-summary-output "$output_dir/rate-summary.json"
accepted="$(jq -r '.accepted' "$output_dir/staging-capacity-result.json")"

printf 'Deployment-equivalent staging capacity evidence: %s\n' "$output_dir"
jq . "$output_dir/staging-capacity-result.json"
if [[ "$accepted" != "true" ]]; then
    echo "Staging capacity qualification did not establish a reproducible knee and forecast envelope" >&2
    exit 1
fi
printf 'Staging capacity qualification passed for the exact reviewed candidate and deployment profile only.\n'
