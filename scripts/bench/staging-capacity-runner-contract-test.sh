#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
runner="$script_dir/proxy-staging-capacity-staircase.sh"
staging_template="$script_dir/staging-profile.example.json"
capacity_template="$script_dir/staging-capacity-profile.example.json"

for command_name in git jq python3 sha256sum; do
    command -v "$command_name" >/dev/null 2>&1 || { echo "$command_name is required" >&2; exit 2; }
done
[[ -z "$(git -C "$repo_root" status --porcelain)" ]] || {
    echo "The staging capacity runner contract requires a clean checkout" >&2
    exit 2
}

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-staging-capacity-runner.XXXXXX")"
cleanup() {
    local status=$?
    trap - EXIT
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-staging-capacity-runner.*) rm -rf -- "$work_dir" ;;
        *) echo "Refusing to remove unexpected runner contract path: $work_dir" >&2 ;;
    esac
    exit "$status"
}
trap cleanup EXIT

action_dir="$work_dir/actions"
stub_dir="$work_dir/stubs"
state_dir="$work_dir/state"
mkdir -m 0700 "$action_dir" "$stub_dir" "$state_dir"
printf '0\n' > "$state_dir/generation"
printf '{"key":"","total":0,"completed":false}\n' > "$state_dir/metrics-state.json"

cat > "$stub_dir/sleep" <<'SH'
#!/usr/bin/env bash
/bin/sleep 0.02
SH

cat > "$stub_dir/curl" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
output=""
url=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --output) output="$2"; shift 2 ;;
        --output=*) output="${1#--output=}"; shift ;;
        http://*|https://*) url="$1"; shift ;;
        *) shift ;;
    esac
done
if [[ "$url" == */api/proxy/config ]]; then
    payload='{"routes":[{"pathPrefix":"/proxy/","strategy":"TAIL_LATENCY_POWER_OF_TWO","hostMatch":null,"headerMatchNames":[]}]}'
else
    payload='{"status":"UP"}'
fi
if [[ -n "$output" && "$output" != "/dev/null" ]]; then printf '%s\n' "$payload" > "$output"; fi
SH

cat > "$stub_dir/vegeta" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
command_name="${1:-}"
shift || true
case "$command_name" in
    attack)
        rate=1
        duration=1
        for argument in "$@"; do
            case "$argument" in
                -rate=*) rate="${argument#-rate=}"; rate="${rate%/s}" ;;
                -duration=*) duration="${argument#-duration=}"; duration="${duration%s}" ;;
            esac
        done
        /bin/sleep 0.15
        jq -cn --argjson rate "$rate" --argjson duration "$duration" '{rate:$rate,duration:$duration}'
        ;;
    report)
        report_type=text
        input=""
        for argument in "$@"; do
            case "$argument" in
                -type=*) report_type="${argument#-type=}" ;;
                -*) ;;
                *) input="$argument" ;;
            esac
        done
        if [[ "$report_type" == "json" ]]; then
            jq '{requests:(.rate*.duration),throughput:.rate,success:1,
              latencies:{"50th":40000000,"95th":80000000,"99th":120000000},
              status_codes:{"200":(.rate*.duration)},errors:[]}' "$input"
        else
            printf 'contract traffic completed\n'
        fi
        ;;
    *) echo "unsupported Vegeta contract command: $command_name" >&2; exit 2 ;;
esac
SH
chmod 0500 "$stub_dir/sleep" "$stub_dir/curl" "$stub_dir/vegeta"

cat > "$action_dir/attested" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
phase="${LBP_STAGING_EXPECTED_PHASE:?}"
if [[ "$phase" == "candidate" ]]; then
    image_reference="$LBP_STAGING_CANDIDATE_IMAGE_REFERENCE"
    revision="$LBP_STAGING_CANDIDATE_SOURCE_REVISION"
else
    image_reference="$LBP_STAGING_PRIOR_IMAGE_REFERENCE"
    revision="$LBP_STAGING_PRIOR_SOURCE_REVISION"
fi
digest="${image_reference##*@}"
elapsed=100
surge=1
if [[ "$phase" == "prior" ]]; then elapsed=0; surge=0; fi
signals="$(jq -c '.deployment.observability.requiredSignals' "$LBP_STAGING_REVIEWED_PROFILE")"
jq -n --arg phase "$phase" --arg observedAt "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  --arg imageReference "$image_reference" --arg imageDigest "$digest" --arg revision "$revision" \
  --arg configurationSha256 "$(jq -r '.deployment.configurationSha256' "$LBP_STAGING_REVIEWED_PROFILE")" \
  --arg ingressIdentitySha256 "$(jq -r '.deployment.ingressIdentitySha256' "$LBP_STAGING_REVIEWED_PROFILE")" \
  --argjson elapsed "$elapsed" --argjson surge "$surge" --argjson signals "$signals" '
  {schemaVersion:1,phase:$phase,observedAt:$observedAt,sourceRevision:$revision,
   imageReference:$imageReference,imageDigest:$imageDigest,configurationSha256:$configurationSha256,
   ingressIdentitySha256:$ingressIdentitySha256,
   replicas:{desired:2,ready:2,available:2,updated:2,images:[{imageDigest:$imageDigest,replicas:2}]},
   placement:{replicasByZone:[1,1]},
   resources:{cpuRequestMillis:100,cpuLimitMillis:1000,memoryRequestMiB:256,memoryLimitMiB:512},
   observability:{scrapeHealthy:true,readyReplicaMetrics:2,signals:$signals},
   transition:{elapsedMillis:$elapsed,maximumUnavailableObserved:0,
     maximumSurgeObserved:$surge,drainCompleted:true}}
'
SH
for hook in verify-deployment rollout-candidate rollback-prior; do cp "$action_dir/attested" "$action_dir/$hook.sh"; done

for hook in slow failure drain reset reload certificate-rotation; do
    cat > "$action_dir/$hook.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
:
SH
done
cat > "$action_dir/restart.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
generation_file="${LBP_STAGING_CAPACITY_CONTRACT_STATE:?}/generation"
generation="$(<"$generation_file")"
printf '%s\n' "$(( generation + 1 ))" > "$generation_file"
SH
chmod 0500 "$action_dir"/*

sampler="$work_dir/capacity-sampler.sh"
cat > "$sampler" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
state_dir="${LBP_STAGING_CAPACITY_CONTRACT_STATE:?}"
state_file="$state_dir/metrics-state.json"
scenario="${LBP_STAGING_CAPACITY_SCENARIO:-initial}"
rate="${LBP_STAGING_CAPACITY_RATE_PER_SECOND:-0}"
repeat="${LBP_STAGING_CAPACITY_REPEAT:-0}"
key="$scenario|$rate|$repeat"
stored_key="$(jq -r '.key' "$state_file")"
total="$(jq -r '.total' "$state_file")"
completed="$(jq -r '.completed' "$state_file")"
if [[ "$stored_key" != "$key" ]]; then
    observed="$total"
    completed=false
elif [[ "$completed" == "false" ]]; then
    observed=$(( total + rate * 30 ))
    total="$observed"
    completed=true
else
    observed="$total"
fi
jq -cn --arg key "$key" --argjson total "$total" --argjson completed "$completed" \
  '{key:$key,total:$total,completed:$completed}' > "$state_file.next"
mv "$state_file.next" "$state_file"
generation="$(<"$state_dir/generation")"
cpu=600
if [[ "$scenario" == "equal" && "$rate" == "160" ]]; then cpu=900; fi
half=$(( observed / 2 ))
jq -n --arg observedAt "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  --arg imageReference "$LBP_STAGING_CAPACITY_EXPECTED_IMAGE_REFERENCE" \
  --arg revision "$LBP_STAGING_CANDIDATE_SOURCE_REVISION" \
  --arg configurationSha256 "$(jq -r '.deployment.configurationSha256' "$LBP_STAGING_REVIEWED_PROFILE")" \
  --arg ingressIdentitySha256 "$(jq -r '.deployment.ingressIdentitySha256' "$LBP_STAGING_REVIEWED_PROFILE")" \
  --arg generation "$generation" --argjson cpu "$cpu" --argjson observed "$observed" --argjson half "$half" '
  {schemaVersion:1,observedAt:$observedAt,phase:"candidate",imageReference:$imageReference,
   sourceRevision:$revision,configurationSha256:$configurationSha256,
   ingressIdentitySha256:$ingressIdentitySha256,
   replicas:[
     {id:("proxy-a-"+$generation),zone:"zone-a",ready:true,imageReference:$imageReference,
      sourceRevision:$revision,cpuUsageMillis:$cpu,memoryWorkingSetMiB:300,openConnections:200,jvmLiveThreads:100},
     {id:("proxy-b-"+$generation),zone:"zone-b",ready:true,imageReference:$imageReference,
      sourceRevision:$revision,cpuUsageMillis:$cpu,memoryWorkingSetMiB:300,openConnections:200,jvmLiveThreads:100}],
   metrics:{requestsTotal:$observed,retriesTotal:0,shedsTotal:0,limitRejectionsTotal:0,inflight:0,
    gcPauseCountTotal:($observed/1000|floor),gcPauseSecondsTotal:($observed/100000),
    proxyP99Millis:100,upstreamP99Millis:90,
    upstreamRequestsTotal:{"backend-a":$half,"backend-b":($observed-$half)}}}
'
SH
chmod 0500 "$sampler"
chmod 0500 "$action_dir"

revision="$(git -C "$repo_root" rev-parse HEAD)"
repository="registry.example.internal/loadbalancerpro"
prior_digest="sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
candidate_digest="sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
staging="$work_dir/staging.json"
jq --arg repository "$repository" --arg revision "$revision" \
  --arg priorDigest "$prior_digest" --arg candidateDigest "$candidate_digest" '
  .profileId="runner-contract-staging"
  | .environment.name="capacity-staging.internal"
  | .environment.changeTicket="CHG-CONTRACT-1"
  | .environment.billableImpactReviewed=true
  | .environment.cleanupAuthority="contract-operator"
  | .review={status:"reviewed",approvedBy:"contract-reviewer",approvedAt:"2026-01-01T00:00:00Z"}
  | .artifact.registryRepository=$repository
  | .artifact.prior={imageDigest:$priorDigest,sourceRevision:("c" * 40)}
  | .artifact.candidate={imageDigest:$candidateDigest,sourceRevision:$revision}
  | .deployment.ingressIdentitySha256=("e" * 64)
  | .deployment.configurationSha256=("f" * 64)
  | .target={scheme:"https",host:"10.0.0.1",port:443,tlsServerName:"staging.internal",allowedCidrs:["10.0.0.0/8"]}
' "$staging_template" > "$staging"
for hook in verify-deployment rollout-candidate rollback-prior slow failure drain restart reset reload certificate-rotation; do
    hook_sha="$(sha256sum "$action_dir/$hook.sh" | awk '{print $1}')"
    jq --arg hook "$hook" --arg sha "$hook_sha" '.hooks[$hook]=$sha' "$staging" > "$staging.next"
    mv "$staging.next" "$staging"
done

staging_sha="$(sha256sum "$staging" | awk '{print $1}')"
sampler_sha="$(sha256sum "$sampler" | awk '{print $1}')"
capacity="$work_dir/capacity.json"
jq --arg stagingSha "$staging_sha" --arg samplerSha "$sampler_sha" '
  .profileId="runner-contract-capacity"
  | .review={status:"reviewed",approvedBy:"contract-reviewer",approvedAt:"2026-01-01T00:00:00Z"}
  | .stagingBinding.stagingProfileSha256=$stagingSha
  | .telemetry.samplerSha256=$samplerSha
' "$capacity_template" > "$capacity"

api_key_file="$work_dir/api-key"
ca_file="$work_dir/ca.pem"
printf 'contract-api-key-0123456789abcdef\n' > "$api_key_file"
printf 'contract trust fixture\n' > "$ca_file"
chmod 0600 "$api_key_file"
chmod 0400 "$ca_file"
export LBP_STAGING_API_KEY_FILE="$api_key_file"
export LBP_STAGING_CA_FILE="$ca_file"
export LBP_STAGING_ACTION_DIR="$action_dir"
export LBP_STAGING_CAPACITY_SAMPLER="$sampler"
export LBP_STAGING_CAPACITY_CONTRACT_STATE="$state_dir"
export LBP_STAGING_CAPACITY_OUTPUT_DIR="$repo_root/target/staging-capacity/runner-contract-${BASHPID}"

PATH="$stub_dir:$PATH" "$runner" --mode run --staging-profile "$staging" --capacity-profile "$capacity" \
    > "$work_dir/runner.log"
jq -e '
  .accepted == true
  and .firstReproducibleSaturationRate == 160
  and .highestFullyPassingRate == 120
  and .recommendedOperatingEnvelopeRate == 120
  and .deployments.prior.phase == "prior"
  and .deployments.candidate.phase == "candidate"
  and .deployments.rollback.phase == "rollback"
' "$LBP_STAGING_CAPACITY_OUTPUT_DIR/staging-capacity-result.json" >/dev/null
[[ "$(find "$LBP_STAGING_CAPACITY_OUTPUT_DIR" -name client.bin | wc -l | tr -d ' ')" == "60" ]] || {
    echo "Runner contract did not retain all 60 raw measured attacks" >&2
    exit 1
}
printf 'Staging capacity runner completed candidate rollout, 60 measured cases, reproducible saturation, and rollback without external traffic.\n'
