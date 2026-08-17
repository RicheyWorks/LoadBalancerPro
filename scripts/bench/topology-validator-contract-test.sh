#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runner="$script_dir/proxy-active-active-topology.sh"
profile="$script_dir/topology-profile.example.json"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-topology-contract.XXXXXX")"
cleanup() {
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-topology-contract.*) rm -rf -- "$work_dir" ;;
        *) echo "Refusing to remove unexpected topology contract path: $work_dir" >&2 ;;
    esac
}
trap cleanup EXIT

bash "$runner" --mode validate --profile "$profile" >/dev/null

assert_rejected() {
    local name="$1" mutation="$2" candidate log
    candidate="$work_dir/$name.json"
    log="$work_dir/$name.log"
    jq "$mutation" "$profile" > "$candidate"
    if bash "$runner" --mode validate --profile "$candidate" >"$log" 2>&1; then
        echo "Topology validator accepted unsafe mutation: $name" >&2
        exit 1
    fi
    grep -Fq 'Topology profile does not satisfy the executable contract' "$log" || {
        echo "Topology validator did not fail at the profile boundary: $name" >&2
        cat "$log" >&2
        exit 1
    }
}

assert_rejected candidate-release-id '.rollout.candidateReleaseId = "registry.example/candidate:latest"'
assert_rejected traffic-window '.rollout.trafficDurationSeconds = 40'
assert_rejected replica-window '.rollout.maximumReplicaReplacementMillis = 1000'
assert_rejected rollout-window '.rollout.maximumRolloutMillis = 30000'
assert_rejected rollback-window '.rollout.maximumRollbackMillis = 30000'
assert_rejected abort-window '.rollout.maximumAbortRecoveryMillis = 10000'
assert_rejected aggregate-budget '.limits.aggregateUpstreamConnectionBudget = 50'
assert_rejected replica-count '.topology.replicas = 3'
assert_rejected multi-zone-claim '.topology.zones = 2'
assert_rejected success-ratio '.objectives.minimumSuccessRatio = 1.1'

printf 'Topology validator rejected 10 unsafe contract mutations.\n'
