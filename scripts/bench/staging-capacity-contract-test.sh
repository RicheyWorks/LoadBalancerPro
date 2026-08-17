#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
staging_template="$script_dir/staging-profile.example.json"
capacity_template="$script_dir/staging-capacity-profile.example.json"
profile_validator="$script_dir/validate-staging-capacity.py"
sample_validator="$script_dir/validate-staging-capacity-sample.py"

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

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-staging-capacity-contract.XXXXXX")"
cleanup() {
    local status=$?
    trap - EXIT
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-staging-capacity-contract.*) rm -rf -- "$work_dir" ;;
        *) echo "Refusing to remove unexpected capacity contract path: $work_dir" >&2 ;;
    esac
    exit "$status"
}
trap cleanup EXIT

repository="registry.example.internal/loadbalancerpro"
candidate_digest="sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
candidate_revision="dddddddddddddddddddddddddddddddddddddddd"
staging="$work_dir/staging.json"
jq --arg repository "$repository" --arg candidateDigest "$candidate_digest" \
  --arg candidateRevision "$candidate_revision" '
  .profileId="reviewed-staging"
  | .environment.name="capacity-staging.internal"
  | .environment.changeTicket="CHG-12345"
  | .environment.billableImpactReviewed=true
  | .environment.cleanupAuthority="platform-operator"
  | .review={status:"reviewed",approvedBy:"capacity-reviewer",approvedAt:"2026-01-01T00:00:00Z"}
  | .artifact.registryRepository=$repository
  | .artifact.prior={imageDigest:("sha256:" + ("a" * 64)),sourceRevision:("c" * 40)}
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

"$python_command" "$profile_validator" --staging-profile "$staging" \
    --capacity-profile "$capacity" --execution >/dev/null

assert_profile_rejected() {
    local name="$1" filter="$2" mutation
    mutation="$work_dir/profile-$name.json"
    jq "$filter" "$capacity" > "$mutation"
    if "$python_command" "$profile_validator" --staging-profile "$staging" \
        --capacity-profile "$mutation" --execution >/dev/null 2>&1; then
        echo "Capacity validator accepted unsafe profile mutation: $name" >&2
        exit 1
    fi
}

assert_profile_rejected wrong-staging-hash '.stagingBinding.stagingProfileSha256=("7" * 64)'
assert_profile_rejected unreviewed '.review.status="example"'
assert_profile_rejected future-review '.review.approvedAt="2999-01-01T00:00:00Z"'
assert_profile_rejected placeholder-sampler '.telemetry.samplerSha256=("0" * 64)'
assert_profile_rejected route-drift '.workload.routeMix[0].path="/proxy/different"'
assert_profile_rejected payload-drift '.workload.payload.responseBytes.p99=1000'
assert_profile_rejected steady-rate-drift '.workload.requestRate.normalPerSecond=41'
assert_profile_rejected burst-rate-drift '.workload.requestRate.burstPerSecond=81'
assert_profile_rejected burst-duration-drift '.workload.requestRate.burstDurationSeconds=31'
assert_profile_rejected objective-drift '.workload.objectives.normalP99Millis=1501'
assert_profile_rejected insufficient-repeats '.capacity.repeatsPerStep=2'
assert_profile_rejected unordered-ladder '.capacity.ratesPerSecond=[40,120,80]'
assert_profile_rejected short-ladder '.capacity.ratesPerSecond=[40,80]'
assert_profile_rejected sparse-sampling '.capacity.sampleIntervalSeconds=15'
assert_profile_rejected sampler-timeout '.telemetry.maximumSamplerSeconds=5'
assert_profile_rejected unexpected-field '.credential="must-not-be-recorded"'

sample="$work_dir/sample.json"
observed_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
jq -n --arg observedAt "$observed_at" --arg imageReference "$repository@$candidate_digest" \
  --arg revision "$candidate_revision" '
  {schemaVersion:1,observedAt:$observedAt,phase:"candidate",imageReference:$imageReference,
   sourceRevision:$revision,configurationSha256:("f" * 64),ingressIdentitySha256:("e" * 64),
   replicas:[
     {id:"proxy-a",zone:"zone-a",ready:true,imageReference:$imageReference,sourceRevision:$revision,
      cpuUsageMillis:300,memoryWorkingSetMiB:300,openConnections:120,jvmLiveThreads:80},
     {id:"proxy-b",zone:"zone-b",ready:true,imageReference:$imageReference,sourceRevision:$revision,
      cpuUsageMillis:320,memoryWorkingSetMiB:310,openConnections:130,jvmLiveThreads:82}
   ],
   metrics:{requestsTotal:1000,retriesTotal:2,shedsTotal:0,limitRejectionsTotal:0,inflight:4,
    gcPauseCountTotal:3,gcPauseSecondsTotal:0.04,proxyP99Millis:120,upstreamP99Millis:90,
    upstreamRequestsTotal:{"backend-a":510,"backend-b":490}}}
  ' > "$sample"

"$python_command" "$sample_validator" --staging-profile "$staging" \
    --capacity-profile "$capacity" --sample "$sample" >/dev/null

assert_sample_rejected() {
    local name="$1" filter="$2" mutation
    mutation="$work_dir/sample-$name.json"
    jq "$filter" "$sample" > "$mutation"
    if "$python_command" "$sample_validator" --staging-profile "$staging" \
        --capacity-profile "$capacity" --sample "$mutation" >/dev/null 2>&1; then
        echo "Capacity telemetry validator accepted unsafe sample mutation: $name" >&2
        exit 1
    fi
}

assert_sample_rejected wrong-image '.imageReference="registry.example.internal/loadbalancerpro@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"'
assert_sample_rejected wrong-revision '.sourceRevision=("a" * 40)'
assert_sample_rejected wrong-config '.configurationSha256=("a" * 64)'
assert_sample_rejected wrong-ingress '.ingressIdentitySha256=("a" * 64)'
assert_sample_rejected missing-replica '.replicas = [.replicas[0]]'
assert_sample_rejected duplicate-replica '.replicas[1].id="proxy-a"'
assert_sample_rejected single-zone '.replicas[1].zone="zone-a"'
assert_sample_rejected not-ready '.replicas[1].ready=false'
assert_sample_rejected replica-image-drift '.replicas[1].imageReference="registry.example.internal/loadbalancerpro@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"'
assert_sample_rejected invalid-memory '.replicas[0].memoryWorkingSetMiB="300"'
assert_sample_rejected negative-counter '.metrics.requestsTotal=-1'
assert_sample_rejected missing-upstream '.metrics.upstreamRequestsTotal |= del(."backend-b")'
assert_sample_rejected unexpected-upstream '.metrics.upstreamRequestsTotal."backend-c"=1'
assert_sample_rejected stale-observation '.observedAt="2020-01-01T00:00:00Z"'
assert_sample_rejected wrong-phase '.phase="prior"'
assert_sample_rejected unexpected-field '.token="must-not-be-recorded"'

printf 'Staging capacity contracts accepted exact reviewed inputs and rejected 16 profile plus 16 telemetry mutations.\n'
