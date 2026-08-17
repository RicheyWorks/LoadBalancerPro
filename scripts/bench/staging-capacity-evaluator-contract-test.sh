#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
staging_template="$script_dir/staging-profile.example.json"
capacity_template="$script_dir/staging-capacity-profile.example.json"
evaluator="$script_dir/evaluate-staging-capacity.py"

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
command -v sha256sum >/dev/null 2>&1 || { echo "sha256sum is required" >&2; exit 2; }

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-staging-capacity-evaluator.XXXXXX")"
cleanup() {
    local status=$?
    trap - EXIT
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-staging-capacity-evaluator.*) rm -rf -- "$work_dir" ;;
        *) echo "Refusing to remove unexpected evaluator contract path: $work_dir" >&2 ;;
    esac
    exit "$status"
}
trap cleanup EXIT

repository="registry.example.internal/loadbalancerpro"
prior_digest="sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
candidate_digest="sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
prior_revision="cccccccccccccccccccccccccccccccccccccccc"
candidate_revision="dddddddddddddddddddddddddddddddddddddddd"
staging="$work_dir/staging.json"
jq --arg repository "$repository" --arg priorDigest "$prior_digest" --arg candidateDigest "$candidate_digest" \
  --arg priorRevision "$prior_revision" --arg candidateRevision "$candidate_revision" '
  .profileId="reviewed-staging"
  | .environment.name="capacity-staging.internal"
  | .environment.changeTicket="CHG-12345"
  | .environment.billableImpactReviewed=true
  | .environment.cleanupAuthority="platform-operator"
  | .review={status:"reviewed",approvedBy:"capacity-reviewer",approvedAt:"2026-01-01T00:00:00Z"}
  | .artifact.registryRepository=$repository
  | .artifact.prior={imageDigest:$priorDigest,sourceRevision:$priorRevision}
  | .artifact.candidate={imageDigest:$candidateDigest,sourceRevision:$candidateRevision}
  | .deployment.ingressIdentitySha256=("e" * 64)
  | .deployment.configurationSha256=("f" * 64)
  | .hooks |= with_entries(.value=("9" * 64))
  ' "$staging_template" > "$staging"
staging_sha="$(sha256sum "$staging" | awk '{print $1}')"
capacity="$work_dir/capacity.json"
jq --arg stagingSha "$staging_sha" '
  .profileId="reviewed-staging-capacity"
  | .review={status:"reviewed",approvedBy:"capacity-reviewer",approvedAt:"2026-01-01T00:00:00Z"}
  | .stagingBinding.stagingProfileSha256=$stagingSha
  | .telemetry.samplerSha256=("8" * 64)
  ' "$capacity_template" > "$capacity"

deployment_validation() {
    local phase="$1" image_reference="$2" revision="$3" output="$4"
    jq -n --arg phase "$phase" --arg imageReference "$image_reference" --arg revision "$revision" '
      {accepted:true,phase:$phase,imageReference:$imageReference,sourceRevision:$revision,
       replicas:2,zones:2,maximumZoneSkewObserved:0,elapsedMillis:(if $phase == "prior" then 0 else 40000 end),
       maximumElapsedMillis:(if $phase == "prior" then 0 else 120000 end),snapshotSha256:("7" * 64)}
    ' > "$output"
}
prior="$work_dir/prior.json"
candidate="$work_dir/candidate.json"
rollback="$work_dir/rollback.json"
deployment_validation prior "$repository@$prior_digest" "$prior_revision" "$prior"
deployment_validation candidate "$repository@$candidate_digest" "$candidate_revision" "$candidate"
deployment_validation rollback "$repository@$prior_digest" "$prior_revision" "$rollback"

measurements="$work_dir/measurements.jsonl"
jq -cn '
  [40,80,120,160][] as $rate
  | range(1;4) as $repeat
  | ["equal","slow","failing","draining","recovering"][] as $scenario
  | ($scenario == "failing") as $injected
  | ($rate == 160 and $scenario == "equal") as $saturated
  | (if $scenario == "slow" then 2500 elif $scenario == "failing" then 3000 else 1500 end) as $budget
  | {scenario:$scenario,rate:$rate,repeat:$repeat,injectedFailure:$injected,
     casePassed:($saturated|not),saturated:$saturated,quiesced:true,
     client:{requests:($rate*30),achievedThroughput:$rate,throughputRatio:1,completionRatio:1,
       successRatio:1,p50Millis:40,p95Millis:80,p99Millis:120,p99BudgetMillis:$budget,
       statusCodes:{"200":($rate*30)},errors:[]},
     deploymentMetrics:{requestsDelta:($rate*30),retriesDelta:0,retryRatio:0,shedsDelta:0,
       limitRejectionsDelta:0,gcPauseCountDelta:1,gcPauseSecondsDelta:0.01,
       requestMetricCoverageRatio:1,proxyOverheadP99Millis:30},
     runtime:{samples:7,stableReplicaSet:true,
       maxCpuUtilizationRatio:(if $saturated then 0.9 else 0.6 end),
       maxMemoryUtilizationRatio:0.6,maxOpenConnectionsPerReplica:200,
       maxJvmLiveThreadsPerReplica:100,maxInflight:100,maxProxyP99Millis:100,maxUpstreamP99Millis:90,
       firstCounters:{requestsTotal:0,retriesTotal:0,shedsTotal:0,limitRejectionsTotal:0,
         gcPauseCountTotal:0,gcPauseSecondsTotal:0},
       lastCounters:{requestsTotal:($rate*30),retriesTotal:0,shedsTotal:0,limitRejectionsTotal:0,
         gcPauseCountTotal:1,gcPauseSecondsTotal:0.01},
       firstUpstreamRequests:{"backend-a":0,"backend-b":0},
       lastUpstreamRequests:{"backend-a":($rate*15),"backend-b":($rate*15)},counterMonotonic:true},
     minimumSuccessRatio:(if $injected then 0.9 else 0.999 end),
     saturationSignals:(if $saturated then ["cpu"] else [] end)}
' > "$measurements"

evaluate() {
    local input="$1" output="$2" prior_file="${3:-$prior}"
    "$python_command" "$evaluator" --staging-profile "$staging" --capacity-profile "$capacity" \
        --measurements "$input" --prior-deployment "$prior_file" --candidate-deployment "$candidate" \
        --rollback-deployment "$rollback" --output "$output" \
        --repeat-summary-output "$output.repeats" --rate-summary-output "$output.rates"
}

result="$work_dir/result.json"
evaluate "$measurements" "$result"
jq -e '
  .accepted == true
  and .firstReproducibleSaturationRate == 160
  and .highestFullyPassingRate == 120
  and .recommendedOperatingEnvelopeRate == 120
  and .requiredQualificationRate == 90
' "$result" >/dev/null

assert_rejected() {
    local name="$1" filter="$2" mutation output
    mutation="$work_dir/$name.jsonl"
    output="$work_dir/$name-result.json"
    jq -s -c "$filter | .[]" "$measurements" > "$mutation"
    if evaluate "$mutation" "$output" >/dev/null 2>&1; then
        echo "Capacity evaluator accepted unsafe measurement mutation: $name" >&2
        exit 1
    fi
}

assert_rejected missing-case 'del(.[-1])'
assert_rejected duplicate-case '. + [.[0]]'
assert_rejected false-pass '.[0].casePassed=false'
assert_rejected false-saturation '.[0].saturated=true'
assert_rejected false-signal '.[0].saturationSignals=["cpu"]'
assert_rejected injected-misclassification '.[0].injectedFailure=true'
assert_rejected unexpected-field '.[0].credential="must-not-be-recorded"'
assert_rejected counter-regression '.[0].runtime.counterMonotonic=false'
assert_rejected missing-rate '[.[] | select(.rate != 80)]'
assert_rejected premature-stop '[.[] | select(.rate != 160)]'
assert_rejected continued-after-saturation 'map(if .rate == 120 and .scenario == "equal" then .casePassed=false | .saturated=true | .runtime.maxCpuUtilizationRatio=0.9 | .saturationSignals=["cpu"] else . end)'

bad_prior="$work_dir/bad-prior.json"
jq '.phase="candidate"' "$prior" > "$bad_prior"
if evaluate "$measurements" "$work_dir/bad-deployment-result.json" "$bad_prior" >/dev/null 2>&1; then
    echo "Capacity evaluator accepted a wrong-phase prior deployment" >&2
    exit 1
fi

unstable="$work_dir/unstable.jsonl"
jq -s -c 'map(if .rate == 160 and .scenario == "equal" and .repeat > 1
  then .casePassed=true | .saturated=false | .runtime.maxCpuUtilizationRatio=0.6 | .saturationSignals=[]
  else . end) | .[]' "$measurements" > "$unstable"
unstable_result="$work_dir/unstable-result.json"
evaluate "$unstable" "$unstable_result"
jq -e '.accepted == false and .unstableStepCount == 1' "$unstable_result" >/dev/null

printf 'Staging capacity evaluator accepted one complete envelope, rejected 12 unsafe matrices, and classified instability as non-accepted.\n'
