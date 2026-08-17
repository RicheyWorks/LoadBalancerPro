#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
profile_template="$script_dir/staging-profile.example.json"
validator="$script_dir/validate-staging-deployment.py"

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

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-staging-deployment-contract.XXXXXX")"
cleanup() {
    local status=$?
    trap - EXIT
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-staging-deployment-contract.*) rm -rf -- "$work_dir" ;;
        *) echo "Refusing to remove unexpected deployment contract path: $work_dir" >&2 ;;
    esac
    exit "$status"
}
trap cleanup EXIT

prior_digest="sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
candidate_digest="sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
prior_revision="cccccccccccccccccccccccccccccccccccccccc"
candidate_revision="dddddddddddddddddddddddddddddddddddddddd"
repository="registry.example.internal/loadbalancerpro"
profile="$work_dir/profile.json"
jq --arg repository "$repository" --arg priorDigest "$prior_digest" --arg candidateDigest "$candidate_digest" \
  --arg priorRevision "$prior_revision" --arg candidateRevision "$candidate_revision" '
  .artifact.registryRepository=$repository
  | .artifact.prior={imageDigest:$priorDigest,sourceRevision:$priorRevision}
  | .artifact.candidate={imageDigest:$candidateDigest,sourceRevision:$candidateRevision}
  | .deployment.ingressIdentitySha256=("e" * 64)
  | .deployment.configurationSha256=("f" * 64)' "$profile_template" > "$profile"

write_snapshot() {
    local phase="$1" digest="$2" revision="$3" elapsed="$4" surge="$5" output="$6"
    local observed_at signals
    observed_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    signals="$(jq -c '.deployment.observability.requiredSignals' "$profile")"
    jq -n --arg phase "$phase" --arg digest "$digest" --arg revision "$revision" \
      --arg imageReference "$repository@$digest" --arg observedAt "$observed_at" \
      --argjson elapsedMillis "$elapsed" --argjson surge "$surge" --argjson signals "$signals" '
      {schemaVersion:1,phase:$phase,observedAt:$observedAt,sourceRevision:$revision,
       imageReference:$imageReference,imageDigest:$digest,configurationSha256:("f" * 64),
       ingressIdentitySha256:("e" * 64),
       replicas:{desired:2,ready:2,available:2,updated:2,images:[{imageDigest:$digest,replicas:2}]},
       placement:{replicasByZone:[1,1]},
       resources:{cpuRequestMillis:100,cpuLimitMillis:1000,memoryRequestMiB:256,memoryLimitMiB:512},
       observability:{scrapeHealthy:true,readyReplicaMetrics:2,signals:$signals},
       transition:{elapsedMillis:$elapsedMillis,maximumUnavailableObserved:0,
         maximumSurgeObserved:$surge,drainCompleted:true}}' > "$output"
}

prior="$work_dir/prior.json"
candidate="$work_dir/candidate.json"
rollback="$work_dir/rollback.json"
write_snapshot prior "$prior_digest" "$prior_revision" 0 0 "$prior"
write_snapshot candidate "$candidate_digest" "$candidate_revision" 45000 1 "$candidate"
write_snapshot rollback "$prior_digest" "$prior_revision" 40000 1 "$rollback"

"$python_command" "$validator" --profile "$profile" --snapshot "$prior" --phase prior >/dev/null
"$python_command" "$validator" --profile "$profile" --snapshot "$candidate" --phase candidate >/dev/null
"$python_command" "$validator" --profile "$profile" --snapshot "$rollback" --phase rollback >/dev/null

assert_rejected() {
    local name="$1" filter="$2" phase="${3:-candidate}" baseline="${4:-$candidate}" mutation
    mutation="$work_dir/$name.json"
    jq "$filter" "$baseline" > "$mutation"
    if "$python_command" "$validator" --profile "$profile" --snapshot "$mutation" --phase "$phase" >/dev/null 2>&1; then
        echo "Deployment validator accepted unsafe snapshot mutation: $name" >&2
        exit 1
    fi
}

assert_rejected wrong-digest '.imageDigest=("sha256:" + ("9" * 64))'
assert_rejected tagged-reference '.imageReference="registry.example.internal/loadbalancerpro:latest"'
assert_rejected not-ready '.replicas.ready=1'
assert_rejected mixed-images '.replicas.images += [{imageDigest:("sha256:" + ("9" * 64)),replicas:1}]'
assert_rejected single-zone '.placement.replicasByZone=[2]'
assert_rejected resource-drift '.resources.memoryLimitMiB=1024'
assert_rejected missing-replica-metrics '.observability.readyReplicaMetrics=1'
assert_rejected missing-signal '.observability.signals -= ["lbp_proxy_requests_total"]'
assert_rejected unavailable-replica '.transition.maximumUnavailableObserved=1'
assert_rejected excessive-surge '.transition.maximumSurgeObserved=2'
assert_rejected incomplete-drain '.transition.drainCompleted=false'
assert_rejected expired-rollout-window '.transition.elapsedMillis=120001'
assert_rejected future-observation '.observedAt="2999-01-01T00:00:00Z"'
assert_rejected stale-observation '.observedAt="2020-01-01T00:00:00Z"'
assert_rejected unexpected-field '.credential="must-not-be-recorded"'
assert_rejected prior-transition '.transition.elapsedMillis=1' prior "$prior"

printf 'Staging deployment validator accepted three exact phases and rejected 16 unsafe snapshots.\n'
