#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
example_profile="$script_dir/staging-profile.example.json"
validator="$script_dir/validate-staging-target.py"
target_renderer="$script_dir/render-capacity-targets.jq"

mode=validate
profile="$example_profile"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --mode) [[ $# -ge 2 ]] || { echo "--mode requires validate or run" >&2; exit 2; }; mode="$2"; shift 2 ;;
        --profile) [[ $# -ge 2 ]] || { echo "--profile requires a JSON file" >&2; exit 2; }; profile="$2"; shift 2 ;;
        --help|-h) echo "Usage: $0 --mode validate|run [--profile staging-profile.json]"; exit 0 ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
done
[[ "$mode" == "validate" || "$mode" == "run" ]] || { echo "Mode must be validate or run" >&2; exit 2; }
for required_file in "$profile" "$validator" "$target_renderer"; do
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
"$python_command" "$validator" --profile "$profile" >/dev/null

if [[ "$mode" == "validate" ]]; then
    printf 'Validated fail-closed staging profile %s without DNS resolution or traffic.\n' \
        "$(jq -r '.profileId' "$profile")"
    printf 'Run mode remains disabled until review, private resolution, secret files, and hook hashes are present.\n'
    exit 0
fi

for command_name in curl jq openssl realpath sha256sum stat timeout vegeta; do
    command -v "$command_name" >/dev/null 2>&1 || { echo "$command_name is required" >&2; exit 2; }
done

jq -e '
  .review.status == "reviewed"
  and (.review.approvedBy | type == "string" and length > 0)
  and (.review.approvedAt | fromdateiso8601 > 0)
  and .environment.billableImpactReviewed == true
  and (.environment.cleanupAuthority | type == "string" and length > 0)
  and (.environment.changeTicket | type == "string"
       and length > 0 and (test("replace|example|todo"; "i") | not))
  and (.environment.name | test("replace|example|todo"; "i") | not)
  and (.environment.cleanupAuthority | test("replace|example|todo"; "i") | not)
  and (.review.approvedBy | test("replace|example|todo"; "i") | not)
  and (.review.approvedAt | fromdateiso8601 <= now)
  and (.artifact.imageDigest != "sha256:" + ("0" * 64))
  and (.artifact.sourceRevision != ("0" * 40))
  and all(.hooks[]; . != ("0" * 64))
' "$profile" >/dev/null || {
    echo "Run mode requires reviewed staging authority, exact artifact identity, and non-placeholder hook hashes" >&2
    exit 2
}

git_revision="$(git -C "$repo_root" rev-parse HEAD 2>/dev/null || echo unknown)"
[[ "$git_revision" == "$(jq -r '.artifact.sourceRevision' "$profile")" ]] || {
    echo "The reviewed artifact sourceRevision must match the clean runner checkout" >&2
    exit 2
}
[[ -z "$(git -C "$repo_root" status --porcelain 2>/dev/null)" ]] || {
    echo "Run mode requires a clean checkout" >&2
    exit 2
}

api_key_file="${LBP_STAGING_API_KEY_FILE:-}"
ca_file="${LBP_STAGING_CA_FILE:-}"
action_dir="${LBP_STAGING_ACTION_DIR:-}"
for required_path in "$api_key_file" "$ca_file" "$action_dir"; do
    [[ -n "$required_path" ]] || { echo "Staging secret, CA, and action paths must be supplied through environment variables" >&2; exit 2; }
done
[[ -f "$api_key_file" && ! -L "$api_key_file" ]] || { echo "LBP_STAGING_API_KEY_FILE must be a regular non-symlink file" >&2; exit 2; }
[[ -s "$ca_file" && ! -L "$ca_file" ]] || { echo "LBP_STAGING_CA_FILE must be a non-empty regular non-symlink file" >&2; exit 2; }
[[ -d "$action_dir" && ! -L "$action_dir" ]] || { echo "LBP_STAGING_ACTION_DIR must be a non-symlink directory" >&2; exit 2; }

repo_resolved="$(cd "$repo_root" && pwd -P)"
api_key_file="$(cd "$(dirname "$api_key_file")" && pwd -P)/$(basename "$api_key_file")"
ca_file="$(cd "$(dirname "$ca_file")" && pwd -P)/$(basename "$ca_file")"
action_dir="$(cd "$action_dir" && pwd -P)"
for external_path in "$api_key_file" "$ca_file" "$action_dir"; do
    case "$external_path/" in
        "$repo_resolved"/*) echo "Staging secrets, trust, and action adapters must remain outside the repository" >&2; exit 2 ;;
    esac
done
api_key_mode="$(stat -c '%a' "$api_key_file")"
[[ "$api_key_mode" =~ ^[0-7]00$ ]] || { echo "Staging API-key file must deny group and other access" >&2; exit 2; }
action_dir_mode="$(stat -c '%a' "$action_dir")"
(( (8#$action_dir_mode & 0022) == 0 )) || { echo "Staging action directory must not be group/other writable" >&2; exit 2; }

profile_id="$(jq -r '.profileId' "$profile")"
default_run_id="$(date -u +%Y%m%dT%H%M%SZ)-${BASHPID}"
output_dir="${LBP_STAGING_OUTPUT_DIR:-$repo_root/target/staging/$profile_id/$default_run_id}"
umask 077
mkdir -p "$repo_root/target"
target_root="$(cd "$repo_root/target" && pwd -P)"
[[ "$output_dir" == /* ]] || output_dir="$repo_root/$output_dir"
output_dir="$(realpath -m -- "$output_dir")"
case "$output_dir/" in
    "$target_root"/*) ;;
    *) echo "LBP_STAGING_OUTPUT_DIR must remain beneath repository target/" >&2; exit 2 ;;
esac
mkdir -p "$output_dir"
output_dir="$(cd "$output_dir" && pwd -P)"
[[ -z "$(find "$output_dir" -mindepth 1 -print -quit)" ]] || { echo "Staging evidence directory must be empty" >&2; exit 2; }

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-staging.XXXXXX")"
verified_hook_dir="$work_dir/verified-hooks"
trusted_ca="$work_dir/staging-ca.pem"
mkdir -m 0700 "$verified_hook_dir"
cp -- "$ca_file" "$trusted_ca"
chmod 0400 "$trusted_ca"
attack_pid=""
needs_reset=false
hooks_verified=false
hook_timeout_seconds="$(jq -r '.thresholds.recoveryWindowSeconds' "$profile")"
cleanup() {
    local status=$?
    trap - EXIT
    if [[ -n "$attack_pid" ]]; then
        kill "$attack_pid" >/dev/null 2>&1 || true
        wait "$attack_pid" >/dev/null 2>&1 || true
    fi
    if [[ "$needs_reset" == "true" && "$hooks_verified" == "true" ]]; then
        timeout --foreground "${hook_timeout_seconds}s" "$verified_hook_dir/reset.sh" >/dev/null 2>&1 || true
    fi
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-staging.*) rm -rf -- "$work_dir" ;;
        *) echo "Refusing to remove unexpected staging temporary path: $work_dir" >&2 ;;
    esac
    exit "$status"
}
trap cleanup EXIT

hook_names=(verify-artifact slow failure reload drain restart certificate-rotation reset)
for hook_name in "${hook_names[@]}"; do
    hook_path="$action_dir/$hook_name.sh"
    [[ -f "$hook_path" && ! -L "$hook_path" && -x "$hook_path" ]] || {
        echo "Missing executable non-symlink staging hook: $hook_path" >&2
        exit 2
    }
    hook_mode="$(stat -c '%a' "$hook_path")"
    (( (8#$hook_mode & 0022) == 0 )) || { echo "Staging hook must not be group/other writable: $hook_path" >&2; exit 2; }
    actual_hash="$(sha256sum "$hook_path" | awk '{print $1}')"
    expected_hash="$(jq -r --arg name "$hook_name" '.hooks[$name]' "$profile")"
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
"$python_command" "$validator" --profile "$profile" --resolve --output "$resolution_file"
host="$(jq -r '.host' "$resolution_file")"
port="$(jq -r '.port' "$resolution_file")"
pinned_address="$(jq -r '.pinnedAddress' "$resolution_file")"
tls_server_name="$(jq -r '.target.tlsServerName' "$profile")"
curl_address="$pinned_address"
connect_address="$pinned_address:$port"
if [[ "$pinned_address" == *:* ]]; then
    curl_address="[$pinned_address]"
    connect_address="[$pinned_address]:$port"
fi
base_url="https://$tls_server_name:$port"
api_key="$(<"$api_key_file")"
[[ "$api_key" =~ ^[A-Za-z0-9._~+/=-]{20,512}$ ]] || {
    echo "Staging API key must be 20-512 header-safe characters" >&2
    exit 2
}
runtime_api_key_file="$work_dir/api-key"
printf '%s' "$api_key" > "$runtime_api_key_file"
chmod 0400 "$runtime_api_key_file"
curl_config="$work_dir/curl-auth.conf"
printf 'header = "X-API-Key: %s"\n' "$api_key" > "$curl_config"
curl_common=(--silent --show-error --fail --cacert "$trusted_ca" --resolve "$tls_server_name:$port:$curl_address"
    --connect-timeout 5 --max-time 15 --config "$curl_config")
vegeta_connect_to="$tls_server_name:$port:$curl_address:$port"

export LBP_EXPECTED_IMAGE_DIGEST="$(jq -r '.artifact.imageDigest' "$profile")"
export LBP_EXPECTED_SOURCE_REVISION="$(jq -r '.artifact.sourceRevision' "$profile")"
export LBP_STAGING_CHANGE_TICKET="$(jq -r '.environment.changeTicket' "$profile")"
export LBP_STAGING_TARGET_HOST="$host"
export LBP_STAGING_TARGET_PORT="$port"
export LBP_STAGING_EXPECTED_STRATEGY="$(jq -r '.workload.routeMix[0].strategy' "$profile")"
export LBP_STAGING_REVIEWED_PROFILE="$profile"

run_hook() {
    local name="$1"
    local maximum_seconds="${2:-$hook_timeout_seconds}"
    local started completed
    started="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    timeout --foreground "${maximum_seconds}s" "$verified_hook_dir/$name.sh"
    completed="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    jq -cn --arg name "$name" --arg startedAt "$started" --arg completedAt "$completed" \
        --arg sha256 "$(jq -r --arg name "$name" '.hooks[$name]' "$profile")" \
        '{name:$name,sha256:$sha256,startedAt:$startedAt,completedAt:$completedAt,exitCode:0}' \
        >> "$output_dir/hook-executions.jsonl"
}

certificate_fingerprint() {
    openssl s_client -connect "$connect_address" -servername "$tls_server_name" \
        -CAfile "$trusted_ca" -verify_return_error </dev/null 2>/dev/null \
        | openssl x509 -noout -fingerprint -sha256 | sed 's/^sha256 Fingerprint=//; s/://g' | tr '[:upper:]' '[:lower:]'
}

wait_for_health() {
    local deadline=$(( SECONDS + $(jq -r '.thresholds.recoveryWindowSeconds' "$profile") ))
    while (( SECONDS < deadline )); do
        if curl "${curl_common[@]}" --output /dev/null "$base_url/actuator/health"; then return 0; fi
        sleep 2
    done
    echo "Staging target did not recover inside the reviewed recovery window" >&2
    return 1
}

run_hook verify-artifact
wait_for_health
curl "${curl_common[@]}" --output "$output_dir/initial-proxy-config.json" "$base_url/api/proxy/config"
jq -e --slurpfile reviewed "$profile" '
  . as $configured
  | $reviewed[0].workload.routeMix
  | all(.[]; . as $workloadRoute
      | any($configured.routes[];
          . as $configuredRoute
          | ($workloadRoute.path | startswith($configuredRoute.pathPrefix))
            and $configuredRoute.strategy == $workloadRoute.strategy
            and $configuredRoute.hostMatch == null
            and ($configuredRoute.headerMatchNames | length == 0)))
' "$output_dir/initial-proxy-config.json" >/dev/null || {
    echo "The deployed proxy configuration does not implement the reviewed route strategy" >&2
    exit 1
}
initial_certificate_fingerprint="$(certificate_fingerprint)"
[[ "$initial_certificate_fingerprint" =~ ^[0-9a-f]{64}$ ]] || { echo "Could not verify staging TLS certificate" >&2; exit 1; }
profile_sha256="$(sha256sum "$profile" | awk '{print $1}')"
ca_sha256="$(sha256sum "$trusted_ca" | awk '{print $1}')"
jq -n --arg profileId "$profile_id" --arg profileSha256 "$profile_sha256" \
  --arg sourceRevision "$git_revision" --arg expectedImageDigest "$LBP_EXPECTED_IMAGE_DIGEST" \
  --arg caSha256 "$ca_sha256" --arg initialCertificateSha256 "$initial_certificate_fingerprint" \
  --arg targetBoundary "reviewed HTTPS staging target pinned to private DNS resolution" \
  '{schemaVersion:1,profileId:$profileId,profileSha256:$profileSha256,sourceRevision:$sourceRevision,
    expectedImageDigest:$expectedImageDigest,caSha256:$caSha256,
    initialCertificateSha256:$initialCertificateSha256,targetBoundary:$targetBoundary}' \
  > "$output_dir/run-metadata.json"

write_targets() {
    jq -c --arg baseUrl "$base_url" --rawfile apiKey "$runtime_api_key_file" \
        -f "$target_renderer" "$profile" > "$work_dir/targets.jsonl"
}

run_scenario() {
    local scenario="$1"
    local hook_name="${2:-}"
    local scenario_dir="$output_dir/$scenario"
    local rate duration p99_budget minimum_success client_p99 client_success upstream_p99 overhead health_cost
    mkdir -p "$scenario_dir"
    rate="$(jq -r --arg name "$scenario" '.scenarios[$name].ratePerSecond' "$profile")"
    duration="$(jq -r --arg name "$scenario" '.scenarios[$name].durationSeconds' "$profile")"
    p99_budget="$(jq -r --arg name "$scenario" '.scenarios[$name].p99Millis' "$profile")"
    minimum_success="$(jq -r --arg name "$scenario" '.scenarios[$name].minimumSuccessRatio' "$profile")"
    curl "${curl_common[@]}" --output "$scenario_dir/status-before.json" "$base_url/api/proxy/status"
    curl "${curl_common[@]}" --output "$scenario_dir/metrics-before.prom" "$base_url/actuator/prometheus"
    write_targets
    vegeta attack -duration="${duration}s" -rate="${rate}/s" -timeout=10s \
        -root-certs="$trusted_ca" -connect-to="$vegeta_connect_to" \
        -max-body=0 -format=json -targets="$work_dir/targets.jsonl" > "$work_dir/$scenario.bin" &
    attack_pid=$!
    local before_rotation=""
    if [[ "$hook_name" == "certificate-rotation" ]]; then before_rotation="$(certificate_fingerprint)"; fi
    if [[ -n "$hook_name" ]]; then
        sleep $(( duration / 3 ))
        needs_reset=true
        run_hook "$hook_name" "$(( duration / 3 ))"
    fi
    if ! wait "$attack_pid"; then attack_pid=""; echo "$scenario staging attack failed" >&2; exit 1; fi
    attack_pid=""
    vegeta report -type=json "$work_dir/$scenario.bin" > "$scenario_dir/client.json"
    vegeta report -type=text "$work_dir/$scenario.bin" > "$scenario_dir/client.txt"
    rm -f -- "$work_dir/$scenario.bin" "$work_dir/targets.jsonl"
    curl "${curl_common[@]}" --output "$scenario_dir/status-after-action.json" "$base_url/api/proxy/status"
    if [[ "$hook_name" == "certificate-rotation" ]]; then
        local after_rotation
        after_rotation="$(certificate_fingerprint)"
        [[ "$after_rotation" =~ ^[0-9a-f]{64}$ && "$after_rotation" != "$before_rotation" ]] || {
            echo "Certificate-rotation hook did not present a new verified certificate" >&2
            exit 1
        }
        jq -n --arg before "$before_rotation" --arg after "$after_rotation" \
            '{verified:true,beforeSha256:$before,afterSha256:$after}' > "$scenario_dir/certificate-rotation.json"
    fi
    if [[ -n "$hook_name" ]]; then
        run_hook reset
        needs_reset=false
        wait_for_health
    fi
    curl "${curl_common[@]}" --output "$scenario_dir/status-after-reset.json" "$base_url/api/proxy/status"
    curl "${curl_common[@]}" --output "$scenario_dir/metrics-after.prom" "$base_url/actuator/prometheus"
    client_p99="$(jq -r '.latencies["99th"] / 1000000' "$scenario_dir/client.json")"
    client_success="$(jq -r '.success' "$scenario_dir/client.json")"
    upstream_p99="$(jq '[.upstreams[].runtimeStats.p99LatencyMillis] | max // 0' "$scenario_dir/status-after-action.json")"
    overhead="$(awk -v client="$client_p99" -v upstream="$upstream_p99" 'BEGIN { value=client-upstream; printf "%.6f", value > 0 ? value : 0 }')"
    health_cost="$(jq '.observability.backendTargetCount / (.healthCheck.intervalMillis / 1000)' "$scenario_dir/status-after-action.json")"
    jq -n --arg scenario "$scenario" --arg hook "${hook_name:-none}" \
      --argjson rate "$rate" --argjson durationSeconds "$duration" \
      --argjson clientP99Millis "$client_p99" --argjson upstreamP99Millis "$upstream_p99" \
      --argjson proxyOverheadP99Millis "$overhead" --argjson successRatio "$client_success" \
      --argjson p99BudgetMillis "$p99_budget" --argjson minimumSuccessRatio "$minimum_success" \
      --argjson healthCheckRequestsPerSecond "$health_cost" \
      --argjson maximumOverhead "$(jq '.thresholds.maximumProxyOverheadP99Millis' "$profile")" \
      --argjson maximumHealthCost "$(jq '.thresholds.maximumHealthCheckRequestsPerSecond' "$profile")" '
      {scenario:$scenario,hook:$hook,ratePerSecond:$rate,durationSeconds:$duration,
       clientP99Millis:$clientP99Millis,upstreamP99Millis:$upstreamP99Millis,
       proxyOverheadP99Millis:$proxyOverheadP99Millis,successRatio:$successRatio,
       healthCheckRequestsPerSecond:$healthCheckRequestsPerSecond,
       thresholds:{p99Millis:$p99BudgetMillis,minimumSuccessRatio:$minimumSuccessRatio,
         maximumProxyOverheadP99Millis:$maximumOverhead,
         maximumHealthCheckRequestsPerSecond:$maximumHealthCost},
       passed:($clientP99Millis <= $p99BudgetMillis and $successRatio >= $minimumSuccessRatio
         and $proxyOverheadP99Millis <= $maximumOverhead
         and $healthCheckRequestsPerSecond <= $maximumHealthCost)}' > "$scenario_dir/result.json"
    jq -e '.passed == true' "$scenario_dir/result.json" >/dev/null || {
        echo "$scenario exceeded its reviewed staging objectives" >&2
        exit 1
    }
}

run_scenario steady
run_scenario burst
run_scenario slow slow
run_scenario failure failure
run_scenario reload reload
run_scenario drain drain
run_scenario restart restart
run_scenario certificateRotation certificate-rotation

"$python_command" - "$runtime_api_key_file" "$output_dir" <<'PY'
from pathlib import Path
import sys

secret = Path(sys.argv[1]).read_bytes()
for evidence in Path(sys.argv[2]).rglob("*"):
    if evidence.is_file() and secret in evidence.read_bytes():
        raise SystemExit(f"API key leaked into staging evidence: {evidence}")
PY

jq -s '{accepted:all(.[]; .passed),scenarios:.,
  boundary:"Reviewed private staging target only; no production-capacity or multi-instance claim."}' \
  "$output_dir"/*/result.json > "$output_dir/staging-result.json"
jq -e '.accepted == true' "$output_dir/staging-result.json" >/dev/null
printf 'Staging qualification evidence: %s\n' "$output_dir"
