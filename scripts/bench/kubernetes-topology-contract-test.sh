#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runner="$script_dir/proxy-kubernetes-topology.sh"
profile="$script_dir/kubernetes-topology-profile.example.json"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-kubernetes-contract.XXXXXX")"
cleanup() {
    case "$work_dir" in "${TMPDIR:-/tmp}"/lbp-kubernetes-contract.*) rm -rf -- "$work_dir" ;;
        *) echo "Refusing to remove unexpected contract path: $work_dir" >&2 ;;
    esac
}
trap cleanup EXIT

bash -n "$runner"
bash "$runner" --mode validate --profile "$profile" > "$work_dir/valid.log"
grep -Fq 'service-distribution per-replica-metrics rolling-replacement endpoint-continuity pod-identity-turnover post-rollout-distribution planned-worker-drain stopped-worker degraded-service worker-recovery' \
    "$work_dir/valid.log"

assert_rejected() {
    local name="$1" filter="$2"
    local candidate="$work_dir/${name}.json"
    jq "$filter" "$profile" > "$candidate"
    if bash "$runner" --mode validate --profile "$candidate" > "$work_dir/${name}.log" 2>&1; then
        echo "Unsafe Kubernetes topology profile was accepted: $name" >&2
        exit 1
    fi
    grep -Fq 'does not satisfy the executable contract' "$work_dir/${name}.log"
}

assert_rejected production-status '.review.status = "reviewed"'
assert_rejected legacy-schema '.schemaVersion = 1'
assert_rejected skewed-kubectl '.cluster.kubectlVersion = "v1.32.2"'
assert_rejected mutable-node-image '.cluster.nodeImageDigest = "sha256:" + ("f" * 64)'
assert_rejected one-worker '.cluster.workers = 1'
assert_rejected one-zone '.cluster.zones = 1'
assert_rejected external-namespace '.cluster.namespace = "production"'
assert_rejected public-port '.cluster.hostPort = 443'
assert_rejected sticky-connection-mode '.workload.connectionMode = "keep-alive"'
assert_rejected low-rate '.workload.ratePerSecond = 1'
assert_rejected short-rollout '.workload.rolloutSeconds = 10'
assert_rejected short-post-rollout '.workload.postRolloutSeconds = 1'
assert_rejected rollout-window-too-short '.workload.rolloutSeconds = .objectives.maximumRolloutSeconds'
assert_rejected short-transition '.workload.transitionSeconds = 5'
assert_rejected weak-rollout-objective '.objectives.minimumRolloutSuccessRatio = 0.5'
assert_rejected weak-post-rollout-objective '.objectives.minimumPostRolloutSuccessRatio = 0.5'
assert_rejected long-rollout '.objectives.maximumRolloutSeconds = 180'
assert_rejected weak-transition-objective '.objectives.minimumTransitionSuccessRatio = 0.5'
assert_rejected long-recovery '.objectives.maximumRecoverySeconds = 600'

printf 'Kubernetes topology contract rejected 19 unsafe profiles without creating a cluster.\n'
