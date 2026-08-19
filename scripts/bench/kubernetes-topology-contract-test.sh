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
grep -Fq 'service-distribution per-replica-metrics content-distinct-rollout endpoint-continuity candidate-pod-identity-turnover post-rollout-distribution baseline-rollback rollback-endpoint-continuity rollback-pod-identity-turnover post-rollback-distribution immutable-certificate-secrets certificate-identity-transition certificate-rotation-continuity certificate-pod-identity-turnover post-certificate-rotation-distribution certificate-identity-rollback certificate-rollback-continuity certificate-rollback-pod-identity-turnover post-certificate-rollback-distribution planned-worker-drain stopped-worker degraded-service worker-recovery abrupt-worker-stop out-of-service-remediation abrupt-endpoint-withdrawal abrupt-recovery' \
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
assert_rejected older-schema '.schemaVersion = 3'
assert_rejected previous-schema '.schemaVersion = 4'
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
assert_rejected short-rollback '.workload.rollbackSeconds = 10'
assert_rejected short-post-rollback '.workload.postRollbackSeconds = 1'
assert_rejected rollback-window-too-short '.workload.rollbackSeconds = .objectives.maximumRollbackSeconds'
assert_rejected short-certificate-rotation '.workload.certificateRotationSeconds = 10'
assert_rejected short-post-certificate-rotation '.workload.postCertificateRotationSeconds = 1'
assert_rejected certificate-rotation-window-too-short '.workload.certificateRotationSeconds = .objectives.maximumCertificateRotationSeconds'
assert_rejected short-certificate-rollback '.workload.certificateRollbackSeconds = 10'
assert_rejected short-post-certificate-rollback '.workload.postCertificateRollbackSeconds = 1'
assert_rejected certificate-rollback-window-too-short '.workload.certificateRollbackSeconds = .objectives.maximumCertificateRollbackSeconds'
assert_rejected short-transition '.workload.transitionSeconds = 5'
assert_rejected short-abrupt-transition '.workload.abruptTransitionSeconds = 5'
assert_rejected short-abrupt-degraded '.workload.abruptDegradedSeconds = 1'
assert_rejected short-abrupt-recovered '.workload.abruptRecoveredSeconds = 1'
assert_rejected abrupt-window-too-short '.workload.abruptTransitionSeconds = .objectives.maximumAbruptEndpointWithdrawalSeconds'
assert_rejected weak-rollout-objective '.objectives.minimumRolloutSuccessRatio = 0.5'
assert_rejected weak-post-rollout-objective '.objectives.minimumPostRolloutSuccessRatio = 0.5'
assert_rejected weak-rollback-objective '.objectives.minimumRollbackSuccessRatio = 0.5'
assert_rejected weak-post-rollback-objective '.objectives.minimumPostRollbackSuccessRatio = 0.5'
assert_rejected weak-certificate-rotation-objective '.objectives.minimumCertificateRotationSuccessRatio = 0.5'
assert_rejected weak-post-certificate-rotation-objective '.objectives.minimumPostCertificateRotationSuccessRatio = 0.5'
assert_rejected weak-certificate-rollback-objective '.objectives.minimumCertificateRollbackSuccessRatio = 0.5'
assert_rejected weak-post-certificate-rollback-objective '.objectives.minimumPostCertificateRollbackSuccessRatio = 0.5'
assert_rejected long-rollout '.objectives.maximumRolloutSeconds = 180'
assert_rejected long-rollback '.objectives.maximumRollbackSeconds = 180'
assert_rejected long-certificate-rotation '.objectives.maximumCertificateRotationSeconds = 180'
assert_rejected long-certificate-rollback '.objectives.maximumCertificateRollbackSeconds = 180'
assert_rejected weak-transition-objective '.objectives.minimumTransitionSuccessRatio = 0.5'
assert_rejected weak-abrupt-transition-objective '.objectives.minimumAbruptTransitionSuccessRatio = 0.5'
assert_rejected weak-abrupt-degraded-objective '.objectives.minimumAbruptDegradedSuccessRatio = 0.5'
assert_rejected weak-abrupt-recovered-objective '.objectives.minimumAbruptRecoveredSuccessRatio = 0.5'
assert_rejected long-abrupt-transition-p99 '.objectives.maximumAbruptTransitionP99Millis = 10000'
assert_rejected long-abrupt-endpoint-withdrawal '.objectives.maximumAbruptEndpointWithdrawalSeconds = 60'
assert_rejected long-recovery '.objectives.maximumRecoverySeconds = 600'
assert_rejected long-abrupt-recovery '.objectives.maximumAbruptRecoverySeconds = 600'
assert_rejected wrong-tls-hostname '.tlsRotation.hostname = "production.example.com"'
assert_rejected wrong-baseline-tls-secret '.tlsRotation.baselineSecret = "production-server-tls"'
assert_rejected unchanged-tls-secret '.tlsRotation.candidateSecret = .tlsRotation.baselineSecret'

printf 'Kubernetes topology contract rejected 52 unsafe profiles without creating a cluster.\n'
