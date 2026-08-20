#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
cluster_config="$repo_root/deploy/kubernetes/kind-cluster.yaml"
workload_manifest="$repo_root/deploy/kubernetes/qualification.yaml"
example_profile="$script_dir/kubernetes-topology-profile.example.json"
candidate_dockerfile="$repo_root/deploy/topology/RolloutCandidate.Dockerfile"

mode=validate
profile="$example_profile"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --mode) [[ $# -ge 2 ]] || { echo "--mode requires validate or smoke" >&2; exit 2; }; mode="$2"; shift 2 ;;
        --profile) [[ $# -ge 2 ]] || { echo "--profile requires JSON" >&2; exit 2; }; profile="$2"; shift 2 ;;
        --help|-h) echo "Usage: $0 --mode validate|smoke [--profile kubernetes-topology-profile.json]"; exit 0 ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
done
case "$mode" in validate|smoke) ;; *) echo "Mode must be validate or smoke" >&2; exit 2 ;; esac
command -v jq >/dev/null 2>&1 || { echo "jq is required" >&2; exit 2; }
for required_file in "$cluster_config" "$workload_manifest" "$candidate_dockerfile" "$profile"; do
    [[ -f "$required_file" ]] || { echo "Missing required file: $required_file" >&2; exit 2; }
done

jq -e '
  .schemaVersion == 6
  and (.profileId | type == "string" and test("^[a-z0-9][a-z0-9._-]{0,62}$"))
  and .review.status == "example"
  and .cluster.kindVersion == "v0.31.0"
  and .cluster.kubectlVersion == "v1.34.3"
  and .cluster.kubernetesVersion == "v1.34.3"
  and .cluster.nodeImageDigest == "sha256:08497ee19eace7b4b5348db5c6a1591d7752b164530a36f855cb0f2bdcbadd48"
  and .cluster.workers == 2
  and .cluster.zones == 2
  and .cluster.namespace == "lbp-kubernetes-smoke"
  and .cluster.hostPort == 18460
  and .cluster.nodePort == 30443
  and .workload.connectionMode == "close-per-request"
  and (.workload.ratePerSecond | type == "number" and . >= 10 and . <= 500 and floor == .)
  and (.workload.baselineSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.rolloutSeconds | type == "number" and . >= 20 and . <= 180 and floor == .)
  and (.workload.postRolloutSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.rollbackSeconds | type == "number" and . >= 20 and . <= 180 and floor == .)
  and (.workload.postRollbackSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.certificateRotationSeconds | type == "number" and . >= 20 and . <= 180 and floor == .)
  and (.workload.postCertificateRotationSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.certificateRollbackSeconds | type == "number" and . >= 20 and . <= 180 and floor == .)
  and (.workload.postCertificateRollbackSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.apiKeyTransitionSeconds | type == "number" and . >= 20 and . <= 120 and floor == .)
  and (.workload.postApiKeyTransitionSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.transitionSeconds | type == "number" and . >= 40 and . <= 120 and floor == .)
  and (.workload.degradedSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.recoveredSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.abruptTransitionSeconds | type == "number" and . >= 15 and . <= 120 and floor == .)
  and (.workload.abruptDegradedSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.abruptRecoveredSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.objectives.minimumBaselineSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumRolloutSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumPostRolloutSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumRollbackSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumPostRollbackSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumCertificateRotationSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumPostCertificateRotationSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumCertificateRollbackSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumPostCertificateRollbackSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumApiKeyTransitionSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumPostApiKeyTransitionSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumTransitionSuccessRatio | type == "number" and . >= 0.90 and . <= 1)
  and (.objectives.minimumDegradedSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumRecoveredSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumAbruptTransitionSuccessRatio | type == "number" and . >= 0.90 and . <= 1)
  and (.objectives.minimumAbruptDegradedSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumAbruptRecoveredSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.maximumP99Millis | type == "number" and . >= 100 and . <= 5000 and floor == .)
  and (.objectives.maximumAbruptTransitionP99Millis | type == "number" and . >= 1000 and . <= 6000 and floor == .)
  and (.objectives.maximumAbruptDegradedP99Millis | type == "number" and . >= 1000 and . <= 6000 and floor == .)
  and (.objectives.maximumRolloutSeconds | type == "number" and . >= 20 and . <= 120 and floor == .)
  and (.objectives.maximumRollbackSeconds | type == "number" and . >= 20 and . <= 120 and floor == .)
  and (.objectives.maximumCertificateRotationSeconds | type == "number" and . >= 20 and . <= 120 and floor == .)
  and (.objectives.maximumCertificateRollbackSeconds | type == "number" and . >= 20 and . <= 120 and floor == .)
  and (.objectives.maximumApiKeyTransitionSeconds | type == "number" and . >= 20 and . <= 90 and floor == .)
  and (.objectives.maximumRecoverySeconds | type == "number" and . >= 30 and . <= 300 and floor == .)
  and (.objectives.maximumAbruptEndpointWithdrawalSeconds | type == "number" and . >= 5 and . <= 30 and floor == .)
  and (.objectives.maximumAbruptRecoverySeconds | type == "number" and . >= 30 and . <= 300 and floor == .)
  and .workload.rolloutSeconds >= (.objectives.maximumRolloutSeconds + 5)
  and .workload.rollbackSeconds >= (.objectives.maximumRollbackSeconds + 5)
  and .workload.certificateRotationSeconds >= (.objectives.maximumCertificateRotationSeconds + 5)
  and .workload.certificateRollbackSeconds >= (.objectives.maximumCertificateRollbackSeconds + 5)
  and .workload.apiKeyTransitionSeconds >= (.objectives.maximumApiKeyTransitionSeconds + 5)
  and .workload.abruptTransitionSeconds >= (.objectives.maximumAbruptEndpointWithdrawalSeconds + 5)
  and .tlsRotation.hostname == "lbp-kubernetes.local"
  and .tlsRotation.baselineSecret == "loadbalancerpro-server-tls-a"
  and .tlsRotation.candidateSecret == "loadbalancerpro-server-tls-b"
  and .tlsRotation.baselineSecret != .tlsRotation.candidateSecret
  and .apiKeyRotation.baselineSecret == "loadbalancerpro-api-key-a"
  and .apiKeyRotation.overlapSecret == "loadbalancerpro-api-key-a-b"
  and .apiKeyRotation.candidateSecret == "loadbalancerpro-api-key-b"
  and ([.apiKeyRotation.baselineSecret, .apiKeyRotation.overlapSecret,
        .apiKeyRotation.candidateSecret] | unique | length) == 3
' "$profile" >/dev/null || { echo "Kubernetes topology profile does not satisfy the executable contract" >&2; exit 2; }

for invariant in \
    'kindest/node:v1.34.3@sha256:08497ee19eace7b4b5348db5c6a1591d7752b164530a36f855cb0f2bdcbadd48' \
    'listenAddress: 127.0.0.1' \
    'containerPort: 30443' \
    'hostPort: 18460' \
    'kind: KubeProxyConfiguration' \
    'mode: iptables' \
    'minSyncPeriod: 0s' \
    'syncPeriod: 1s'; do
    grep -Fq "$invariant" "$cluster_config" || { echo "Kind cluster config is missing: $invariant" >&2; exit 2; }
done
for invariant in \
    'pod-security.kubernetes.io/enforce: restricted' \
    'automountServiceAccountToken: false' \
    'maxUnavailable: 0' \
    'maxSurge: 1' \
    'terminationGracePeriodSeconds: 45' \
    'command: ["sh", "-c", "sleep 10"]' \
    'minDomains: 2' \
    'runAsUser: 10001' \
    'readOnlyRootFilesystem: true' \
    'nodePort: 30443' \
    'kind: PodDisruptionBudget' \
    'kind: NetworkPolicy' \
    'secretName: loadbalancerpro-server-tls-a' \
    'secretName: loadbalancerpro-api-key-a' \
    'https://${LBP_TLS_HOSTNAME}:8080/proxy/kubernetes/topology' \
    'LBP_HEALTH_CHECK_ENABLED: "false"' \
    'LBP_COOLDOWN_ENABLED: "false"' \
    'LBP_RETRY_ENABLED: "true"' \
    'LBP_RETRY_MAX_ATTEMPTS: "3"' \
    'LBP_RETRY_BUDGET_PERCENT: "100"' \
    'LBP_RETRY_NON_IDEMPOTENT: "false"' \
    'path: loadbalancerpro.api.rotation-key'; do
    grep -Fq "$invariant" "$workload_manifest" || { echo "Kubernetes workload is missing: $invariant" >&2; exit 2; }
done
if grep -Eq '(BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|api-key:[[:space:]]+[^[:space:]]+)' "$workload_manifest"; then
    echo "Kubernetes workload must not contain private keys or API-key values" >&2
    exit 2
fi

if [[ "$mode" == "validate" ]]; then
    printf 'Validated disposable two-worker/two-zone Kubernetes topology contract %s.\n' "$(jq -r '.profileId' "$profile")"
    printf 'Validated proof cases: service-distribution per-replica-metrics content-distinct-rollout endpoint-continuity candidate-pod-identity-turnover post-rollout-distribution baseline-rollback rollback-endpoint-continuity rollback-pod-identity-turnover post-rollback-distribution immutable-certificate-secrets certificate-identity-transition certificate-rotation-continuity certificate-pod-identity-turnover post-certificate-rotation-distribution certificate-identity-rollback certificate-rollback-continuity certificate-rollback-pod-identity-turnover post-certificate-rollback-distribution bounded-api-key-overlap immutable-api-key-secrets api-key-rotation-continuity api-key-retirement api-key-rollback-continuity api-key-rollback-retirement planned-worker-drain stopped-worker degraded-service worker-recovery abrupt-worker-stop out-of-service-remediation abrupt-endpoint-withdrawal abrupt-recovery\n'
    exit 0
fi

for command_name in awk curl docker git jq kind kubectl openssl realpath sha256sum vegeta; do
    command -v "$command_name" >/dev/null 2>&1 || { echo "$command_name is required" >&2; exit 2; }
done
docker info --format '{{.OSType}}' | grep -Fxq linux || { echo "A running Linux Docker engine is required" >&2; exit 2; }
kind version | grep -Fq 'v0.31.0' || { echo "kind v0.31.0 is required" >&2; exit 2; }
kubectl version --client -o json | jq -e '.clientVersion.gitVersion == "v1.34.3"' >/dev/null || {
    echo "kubectl v1.34.3 is required to keep client/server version skew supported" >&2
    exit 2
}

profile_id="$(jq -r '.profileId' "$profile")"
namespace="$(jq -r '.cluster.namespace' "$profile")"
host_port="$(jq -r '.cluster.hostPort' "$profile")"
rate="$(jq -r '.workload.ratePerSecond' "$profile")"
tls_hostname="$(jq -r '.tlsRotation.hostname' "$profile")"
baseline_tls_secret="$(jq -r '.tlsRotation.baselineSecret' "$profile")"
candidate_tls_secret="$(jq -r '.tlsRotation.candidateSecret' "$profile")"
baseline_api_key_secret="$(jq -r '.apiKeyRotation.baselineSecret' "$profile")"
overlap_api_key_secret="$(jq -r '.apiKeyRotation.overlapSecret' "$profile")"
candidate_api_key_secret="$(jq -r '.apiKeyRotation.candidateSecret' "$profile")"
default_run_id="${GITHUB_RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)-${BASHPID}}-${GITHUB_RUN_ATTEMPT:-1}"
cluster_name="${LBP_KUBERNETES_CLUSTER:-lbp-k8s-${GITHUB_RUN_ID:-local-${BASHPID}}-${GITHUB_RUN_ATTEMPT:-1}}"
[[ "$cluster_name" =~ ^lbp-k8s-[a-z0-9][a-z0-9-]{0,48}$ ]] || {
    echo "LBP_KUBERNETES_CLUSTER must be an isolated lbp-k8s-* name" >&2
    exit 2
}

output_dir="${LBP_KUBERNETES_OUTPUT_DIR:-$repo_root/target/kubernetes/$profile_id/$default_run_id}"
umask 077
mkdir -p "$repo_root/target"
target_root="$(cd "$repo_root/target" && pwd -P)"
[[ "$output_dir" == /* ]] || output_dir="$repo_root/$output_dir"
output_dir="$(realpath -m -- "$output_dir")"
case "$output_dir/" in "$target_root"/*) ;; *) echo "Kubernetes evidence must remain beneath target/" >&2; exit 2 ;; esac
mkdir -p "$output_dir"
output_dir="$(cd "$output_dir" && pwd -P)"
[[ -z "$(find "$output_dir" -mindepth 1 -print -quit)" ]] || {
    echo "Kubernetes evidence directory must be empty" >&2
    exit 2
}

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-kubernetes.XXXXXX")"
kubeconfig="$work_dir/kubeconfig"
api_key_file="$work_dir/loadbalancerpro-api-key-a"
candidate_api_key_file="$work_dir/loadbalancerpro-api-key-b"
empty_rotation_key_file="$work_dir/loadbalancerpro-api-key-empty"
tls_dir="$work_dir/tls"
candidate_tls_dir="$work_dir/tls-candidate"
tls_trust_bundle="$work_dir/tls-rollover-ca-bundle.pem"
attack_pid=""
rollout_sampler_pid=""
rollout_stop_file="$work_dir/stop-rollout-sampler"
stopped_node=""
cluster_created=false
cleanup() {
    local status=$?
    trap - EXIT
    if [[ -n "$attack_pid" ]]; then kill "$attack_pid" >/dev/null 2>&1 || true; wait "$attack_pid" >/dev/null 2>&1 || true; fi
    if [[ -n "$rollout_sampler_pid" ]]; then
        : > "$rollout_stop_file"
        kill "$rollout_sampler_pid" >/dev/null 2>&1 || true
        wait "$rollout_sampler_pid" >/dev/null 2>&1 || true
    fi
    if [[ -n "$stopped_node" ]]; then docker start "$stopped_node" >/dev/null 2>&1 || true; fi
    if [[ "$cluster_created" == true ]]; then kind delete cluster --name "$cluster_name" >/dev/null 2>&1 || true; fi
    case "$work_dir" in "${TMPDIR:-/tmp}"/lbp-kubernetes.*) chmod -R u+w "$work_dir" 2>/dev/null || true; rm -rf -- "$work_dir" ;;
        *) echo "Refusing to remove unexpected Kubernetes work path: $work_dir" >&2 ;;
    esac
    exit "$status"
}
trap cleanup EXIT

proxy_image=loadbalancerpro:kubernetes-proxy
candidate_image=loadbalancerpro:kubernetes-candidate
fixture_image=loadbalancerpro:kubernetes-fixture
proxy_source="${LBP_KUBERNETES_PROXY_SOURCE_IMAGE:-}"
candidate_source="${LBP_KUBERNETES_CANDIDATE_SOURCE_IMAGE:-}"
fixture_source="${LBP_KUBERNETES_FIXTURE_SOURCE_IMAGE:-}"
source_revision="$(git -C "$repo_root" rev-parse HEAD)"
[[ "$source_revision" =~ ^[0-9a-f]{40}$ ]] || { echo "Unable to bind source revision" >&2; exit 1; }
candidate_release_id="kubernetes-${source_revision:0:12}"
if [[ -n "$proxy_source" ]]; then
    docker image inspect "$proxy_source" >/dev/null
    docker tag "$proxy_source" "$proxy_image"
else
    docker build --tag "$proxy_image" "$repo_root"
fi
if [[ -n "$candidate_source" ]]; then
    docker image inspect "$candidate_source" >/dev/null
    docker tag "$candidate_source" "$candidate_image"
    candidate_release_id="$(docker image inspect --format \
        '{{index .Config.Labels "com.richeyworks.loadbalancerpro.rollout.release-id"}}' "$candidate_image")"
    [[ "$candidate_release_id" =~ ^[a-z0-9][a-z0-9._-]{0,127}$ ]] || {
        echo "Provided Kubernetes rollout candidate has no bounded release label" >&2; exit 1;
    }
else
    docker build --quiet --file "$candidate_dockerfile" --build-arg "BASE_IMAGE=$proxy_image" \
        --build-arg "ROLLOUT_RELEASE_ID=$candidate_release_id" --tag "$candidate_image" "$repo_root" >/dev/null
fi
if [[ -n "$fixture_source" ]]; then
    docker image inspect "$fixture_source" >/dev/null
    docker tag "$fixture_source" "$fixture_image"
else
    docker build --file "$repo_root/deploy/fixture/Dockerfile" --tag "$fixture_image" "$repo_root"
fi
proxy_image_id="$(docker image inspect --format '{{.Id}}' "$proxy_image")"
candidate_image_id="$(docker image inspect --format '{{.Id}}' "$candidate_image")"
fixture_image_id="$(docker image inspect --format '{{.Id}}' "$fixture_image")"
for image_id in "$proxy_image_id" "$candidate_image_id" "$fixture_image_id"; do
    [[ "$image_id" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "Qualification image has no exact local content ID" >&2; exit 1; }
done
[[ "$candidate_image_id" != "$proxy_image_id" ]] || {
    echo "Kubernetes rollout candidate is not content-distinct from the baseline image" >&2; exit 1;
}
baseline_layers="$(docker image inspect --format '{{json .RootFS.Layers}}' "$proxy_image_id")"
[[ "$(docker image inspect --format '{{json .RootFS.Layers}}' "$candidate_image_id")" == "$baseline_layers" ]] || {
    echo "Kubernetes rollout candidate changed application layers" >&2; exit 1;
}
for inherited_config in '{{json .Config.Cmd}}' '{{json .Config.Entrypoint}}' '{{json .Config.User}}'; do
    [[ "$(docker image inspect --format "$inherited_config" "$candidate_image_id")" == \
       "$(docker image inspect --format "$inherited_config" "$proxy_image_id")" ]] || {
        echo "Kubernetes rollout candidate changed inherited runtime configuration" >&2; exit 1;
    }
done
[[ "$(docker image inspect --format '{{index .Config.Labels "com.richeyworks.loadbalancerpro.rollout.release-id"}}' \
    "$candidate_image_id")" == "$candidate_release_id" ]] || {
    echo "Kubernetes rollout candidate release label is missing or stale" >&2; exit 1;
}
if kind get clusters | grep -Fxq "$cluster_name"; then
    echo "Refusing to reuse or delete an existing kind cluster named $cluster_name" >&2
    exit 2
fi
cluster_created=true
kind create cluster --name "$cluster_name" --config "$cluster_config" --kubeconfig "$kubeconfig" --wait 180s
if command -v cygpath >/dev/null 2>&1; then
    export KUBECONFIG="$(cygpath -w "$kubeconfig")"
else
    export KUBECONFIG="$kubeconfig"
fi
[[ "$(kubectl config current-context)" == "kind-$cluster_name" ]] || { echo "Unexpected Kubernetes context" >&2; exit 1; }
kubectl wait --for=condition=Ready nodes --all --timeout=120s
kube_proxy_config="$(kubectl get configmap kube-proxy --namespace kube-system -o json \
    | jq -r '.data["config.conf"]')"
for invariant in 'mode: iptables' 'minSyncPeriod: 0s' 'syncPeriod: 1s'; do
    grep -Fq "$invariant" <<< "$kube_proxy_config" || {
        echo "Live kube-proxy config is missing: $invariant" >&2; exit 1;
    }
done
printf '%s\n' "$kube_proxy_config" > "$output_dir/kube-proxy-config.yaml"
mapfile -t workers < <(kind get nodes --name "$cluster_name" | grep -- '-worker' | sort)
[[ ${#workers[@]} -eq 2 ]] || { echo "Expected exactly two kind workers" >&2; exit 1; }
kubectl label node "${workers[0]}" loadbalancerpro.io/qualification-worker=true topology.kubernetes.io/zone=zone-a --overwrite
kubectl label node "${workers[1]}" loadbalancerpro.io/qualification-worker=true topology.kubernetes.io/zone=zone-b --overwrite

kind load docker-image "$proxy_image" "$candidate_image" "$fixture_image" --name "$cluster_name"
openssl rand -hex 24 > "$api_key_file"
openssl rand -hex 24 > "$candidate_api_key_file"
: > "$empty_rotation_key_file"
[[ "$(<"$api_key_file")" != "$(<"$candidate_api_key_file")" ]] || {
    echo "Generated Kubernetes API keys were not distinct" >&2; exit 1;
}
generate_server_identity() {
    local directory="$1" authority_common_name="$2" serial="$3"
    local ca_key="$directory/ca-key.pem"
    local server_csr="$directory/server.csr"
    local server_extensions="$directory/server-extensions.cnf"
    mkdir -p "$directory"
    MSYS2_ARG_CONV_EXCL="/CN=$authority_common_name" \
    openssl req -x509 -newkey rsa:2048 -sha256 -days 1 -nodes \
        -subj "/CN=$authority_common_name" \
        -addext 'basicConstraints=critical,CA:TRUE' -addext 'keyUsage=critical,keyCertSign,cRLSign' \
        -keyout "$ca_key" -out "$directory/ca.pem" >/dev/null 2>&1
    MSYS2_ARG_CONV_EXCL="/CN=$tls_hostname" \
    openssl req -newkey rsa:2048 -sha256 -nodes -subj "/CN=$tls_hostname" \
        -keyout "$directory/private-key.pem" -out "$server_csr" >/dev/null 2>&1
    printf '%s\n' \
        "subjectAltName=DNS:$tls_hostname,DNS:loadbalancerpro,DNS:loadbalancerpro.$namespace.svc,IP:127.0.0.1" \
        'basicConstraints=critical,CA:FALSE' \
        'keyUsage=critical,digitalSignature,keyEncipherment' \
        'extendedKeyUsage=serverAuth' > "$server_extensions"
    openssl x509 -req -sha256 -days 1 -in "$server_csr" -CA "$directory/ca.pem" -CAkey "$ca_key" \
        -set_serial "$serial" -extfile "$server_extensions" -out "$directory/certificate.pem" >/dev/null 2>&1
}

certificate_fingerprint() {
    openssl x509 -in "$1" -noout -fingerprint -sha256 \
        | awk -F= '{print tolower($2)}' | tr -d ':'
}

generate_server_identity "$tls_dir" 'LoadBalancerPro Kubernetes Qualification CA A' 1001
generate_server_identity "$candidate_tls_dir" 'LoadBalancerPro Kubernetes Qualification CA B' 2001
openssl verify -CAfile "$tls_dir/ca.pem" -verify_hostname "$tls_hostname" \
    "$tls_dir/certificate.pem" >/dev/null
openssl verify -CAfile "$candidate_tls_dir/ca.pem" -verify_hostname "$tls_hostname" \
    "$candidate_tls_dir/certificate.pem" >/dev/null
if openssl verify -CAfile "$tls_dir/ca.pem" "$candidate_tls_dir/certificate.pem" >/dev/null 2>&1 \
    || openssl verify -CAfile "$candidate_tls_dir/ca.pem" "$tls_dir/certificate.pem" >/dev/null 2>&1; then
    echo "Generated Kubernetes TLS identities did not use independent trust roots" >&2
    exit 1
fi
cat "$tls_dir/ca.pem" "$candidate_tls_dir/ca.pem" > "$tls_trust_bundle"
baseline_certificate_fingerprint="$(certificate_fingerprint "$tls_dir/certificate.pem")"
candidate_certificate_fingerprint="$(certificate_fingerprint "$candidate_tls_dir/certificate.pem")"
[[ "$baseline_certificate_fingerprint" =~ ^[0-9a-f]{64}$ \
   && "$candidate_certificate_fingerprint" =~ ^[0-9a-f]{64}$ \
   && "$baseline_certificate_fingerprint" != "$candidate_certificate_fingerprint" ]] || {
    echo "Generated Kubernetes TLS identities were not content-distinct" >&2; exit 1;
}
chmod 0600 "$api_key_file" "$candidate_api_key_file" "$empty_rotation_key_file" \
    "$tls_dir"/* "$candidate_tls_dir"/* "$tls_trust_bundle"

kubectl apply --server-side --field-manager=loadbalancerpro-qualification -f "$workload_manifest"
create_immutable_api_key_secret() {
    local secret_name="$1" primary_file="$2" rotation_file="$3"
    kubectl create secret generic "$secret_name" --namespace "$namespace" \
        --from-file=api-key="$primary_file" --from-file=rotation-key="$rotation_file" \
        --dry-run=client -o json | jq '.immutable = true' | kubectl create -f -
}
create_immutable_api_key_secret "$baseline_api_key_secret" "$api_key_file" "$empty_rotation_key_file"
create_immutable_api_key_secret "$overlap_api_key_secret" "$api_key_file" "$candidate_api_key_file"
create_immutable_api_key_secret "$candidate_api_key_secret" "$candidate_api_key_file" "$empty_rotation_key_file"
kubectl get "secret/$baseline_api_key_secret" "secret/$overlap_api_key_secret" \
    "secret/$candidate_api_key_secret" --namespace "$namespace" -o json \
    | jq '.items |= map(. + {dataKeys: (.data | keys)}
        | del(.data) | del(.metadata.managedFields))' \
    > "$output_dir/api-key-secret-metadata.json"
jq -e --arg baseline "$baseline_api_key_secret" --arg overlap "$overlap_api_key_secret" \
    --arg candidate "$candidate_api_key_secret" '
      (.items | length) == 3
      and ([.items[].metadata.name] | sort == ([$baseline, $overlap, $candidate] | sort))
      and all(.items[]; .immutable == true and .type == "Opaque"
        and .dataKeys == ["api-key", "rotation-key"])' \
    "$output_dir/api-key-secret-metadata.json" >/dev/null || {
    echo "Versioned Kubernetes API-key Secrets were not immutable bounded-overlap objects" >&2; exit 1;
}
create_immutable_tls_secret() {
    local secret_name="$1" directory="$2"
    kubectl create secret generic "$secret_name" --namespace "$namespace" \
        --from-file=tls.crt="$directory/certificate.pem" \
        --from-file=tls.key="$directory/private-key.pem" \
        --from-file=ca.crt="$directory/ca.pem" --dry-run=client -o json \
        | jq '.immutable = true | .type = "kubernetes.io/tls"' \
        | kubectl create -f -
}
create_immutable_tls_secret "$baseline_tls_secret" "$tls_dir"
create_immutable_tls_secret "$candidate_tls_secret" "$candidate_tls_dir"
kubectl get "secret/$baseline_tls_secret" "secret/$candidate_tls_secret" --namespace "$namespace" -o json \
    | jq 'del(.items[].data) | del(.items[].metadata.managedFields)' \
    > "$output_dir/tls-secret-metadata.json"
jq -e '(.items | length) == 2 and all(.items[]; .immutable == true and .type == "kubernetes.io/tls")' \
    "$output_dir/tls-secret-metadata.json" >/dev/null || {
    echo "Versioned Kubernetes TLS Secrets were not immutable TLS objects" >&2; exit 1;
}
kubectl patch deployment loadbalancerpro --namespace "$namespace" --type merge \
    -p "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"loadbalancerpro.io/source-revision\":\"$source_revision\"}}}}}"
for deployment in backend-a backend-b loadbalancerpro; do
    kubectl rollout status deployment/"$deployment" --namespace "$namespace" --timeout=240s
done

capture_state() {
    local prefix="$1"
    kubectl get nodes -o wide > "$output_dir/${prefix}-nodes.txt"
    kubectl get deployment,pod,service,poddisruptionbudget,networkpolicy --namespace "$namespace" -o wide \
        > "$output_dir/${prefix}-workloads.txt"
    kubectl get deployment,replicaset,pod --namespace "$namespace" -o json \
        > "$output_dir/${prefix}-workloads.json"
    kubectl get endpointslice --namespace "$namespace" -o json > "$output_dir/${prefix}-endpointslices.json"
}

ready_proxy_count() {
    kubectl get pod --namespace "$namespace" -l app.kubernetes.io/name=loadbalancerpro -o json \
        | jq '[.items[] | select(.metadata.deletionTimestamp == null)
            | select(.status.phase == "Running")
            | select(any(.status.conditions[]?; .type == "Ready" and .status == "True"))] | length'
}

ready_endpoint_count() {
    kubectl get endpointslice --namespace "$namespace" \
        -l kubernetes.io/service-name=loadbalancerpro -o json \
        | jq '[.items[].endpoints[]
            | select(.conditions.ready == true and .conditions.terminating != true)] | length'
}

ready_backend_endpoint_count() {
    local service_name="$1"
    kubectl get endpointslice --namespace "$namespace" \
        -l kubernetes.io/service-name="$service_name" -o json \
        | jq '[.items[].endpoints[]
            | select(.conditions.ready == true and .conditions.terminating != true)] | length'
}

abrupt_source_pod_count() {
    kubectl get pod --namespace "$namespace" -o json \
        | jq --argjson names "$abrupt_forced_pod_names_json" \
            '[.items[] | select(.metadata.name as $name | $names | index($name))] | length'
}

wait_for_count() {
    local description="$1" expected="$2" command_name="$3" timeout_seconds="$4"
    local deadline=$((SECONDS + timeout_seconds)) actual=unknown
    while (( SECONDS < deadline )); do
        actual="$($command_name)"
        if [[ "$actual" == "$expected" ]]; then return 0; fi
        sleep 2
    done
    echo "Timed out waiting for $description=$expected; observed $actual" >&2
    return 1
}

wait_for_backend_endpoint_count() {
    local service_name="$1" expected="$2" timeout_seconds="$3"
    local deadline=$((SECONDS + timeout_seconds)) actual=unknown
    while (( SECONDS < deadline )); do
        actual="$(ready_backend_endpoint_count "$service_name")"
        [[ "$actual" == "$expected" ]] && return 0
        sleep 1
    done
    echo "$service_name ready endpoint count did not become $expected; last observed $actual" >&2
    return 1
}

sample_transition_continuity() {
    local phase="$1"
    local output="$output_dir/${phase}-continuity.csv"
    printf 'epoch_seconds,ready_proxy_pods,ready_service_endpoints,total_proxy_pods\n' > "$output"
    while [[ ! -f "$rollout_stop_file" ]]; do
        local ready_pods ready_endpoints total_pods
        ready_pods="$(ready_proxy_count)"
        ready_endpoints="$(ready_endpoint_count)"
        total_pods="$(kubectl get pod --namespace "$namespace" \
            -l app.kubernetes.io/name=loadbalancerpro -o json | jq '.items | length')"
        printf '%s,%s,%s,%s\n' "$(date +%s)" "$ready_pods" "$ready_endpoints" "$total_pods" >> "$output"
        if (( ready_pods < 2 || ready_endpoints < 2 )); then
            echo "$phase transition dropped below two ready proxy pods or Service endpoints" >&2
            return 1
        fi
        sleep 1
    done
}

collect_distribution() {
    local phase="$1"
    local require_positive="${2:-true}"
    local pods_json pod pod_total pod_backend_a pod_backend_b metrics_file
    local backend_a_total=0 backend_b_total=0
    local rows_file="$work_dir/${phase}-distribution-rows.jsonl"
    local -a phase_proxy_pods=()
    : > "$rows_file"
    pods_json="$(kubectl get pod --namespace "$namespace" \
        -l app.kubernetes.io/name=loadbalancerpro -o json)"
    mapfile -t phase_proxy_pods < <(jq -r '.items[]
        | select(.metadata.deletionTimestamp == null)
        | select(.status.phase == "Running")
        | select(any(.status.conditions[]?; .type == "Ready" and .status == "True"))
        | .metadata.name' <<< "$pods_json" | sort)
    [[ ${#phase_proxy_pods[@]} -eq 2 ]] || {
        echo "Expected two ready proxy replicas for $phase metrics proof" >&2
        return 1
    }
    for pod in "${phase_proxy_pods[@]}"; do
        metrics_file="$output_dir/${phase}-${pod}-metrics.txt"
        kubectl exec --namespace "$namespace" "$pod" -- sh -c '
          curl --fail --silent --show-error --cacert /run/tls/ca.pem \
            --header "X-API-Key: $(cat /run/secrets/loadbalancerpro.api.key)" \
            --resolve "${LBP_TLS_HOSTNAME}:8080:127.0.0.1" \
            "https://${LBP_TLS_HOSTNAME}:8080/actuator/prometheus"
        ' > "$metrics_file"
        pod_total="$(awk '$1 ~ /^lbp_proxy_requests_total(\{|$)/ { total += $2 } END { printf "%.0f", total + 0 }' "$metrics_file")"
        [[ "$pod_total" =~ ^[0-9]+$ ]] || {
            echo "$pod returned an invalid $phase request counter" >&2
            return 1
        }
        if [[ "$require_positive" == true && ! "$pod_total" =~ ^[1-9][0-9]*$ ]]; then
            echo "$pod did not serve $phase traffic" >&2
            return 1
        fi
        pod_backend_a="$(awk '$1 ~ /^lbp_proxy_requests_total\{/ && $1 ~ /upstream="backend-a"/ { total += $2 } END { printf "%.0f", total + 0 }' "$metrics_file")"
        pod_backend_b="$(awk '$1 ~ /^lbp_proxy_requests_total\{/ && $1 ~ /upstream="backend-b"/ { total += $2 } END { printf "%.0f", total + 0 }' "$metrics_file")"
        backend_a_total=$((backend_a_total + pod_backend_a))
        backend_b_total=$((backend_b_total + pod_backend_b))
        jq -n --arg pod "$pod" --argjson requests "$pod_total" \
            --argjson backendARequests "$pod_backend_a" --argjson backendBRequests "$pod_backend_b" \
            '{pod: $pod, requests: $requests,
              backendARequests: $backendARequests, backendBRequests: $backendBRequests}' >> "$rows_file"
    done
    (( backend_a_total > 0 && backend_b_total > 0 )) || {
        echo "Both configured backends must serve $phase traffic" >&2
        return 1
    }
    jq -s --arg phase "$phase" --argjson backendARequests "$backend_a_total" \
        --argjson backendBRequests "$backend_b_total" \
        '{phase: $phase, bothProxyReplicasServed: true,
          backendARequests: $backendARequests, backendBRequests: $backendBRequests,
          pods: .}' "$rows_file" > "$output_dir/${phase}-distribution.json"
}

assert_two_zone_proxy_pods() {
    local pods_json="$1" description="$2"
    [[ "$(jq 'length' <<< "$pods_json")" == 2 ]] || {
        echo "$description did not converge to two ready proxy pods" >&2
        return 1
    }
    [[ "$(ready_proxy_count)" == 2 && "$(ready_endpoint_count)" == 2 ]] || {
        echo "$description did not publish exactly two ready pods and Service endpoints" >&2
        return 1
    }
    [[ "$(jq '[.[].spec.nodeName] | unique | length' <<< "$pods_json")" == 2 ]] || {
        echo "$description pods were not placed on distinct workers" >&2
        return 1
    }
    local zone_count
    zone_count="$(jq -r '.[].spec.nodeName' <<< "$pods_json" \
        | while read -r node; do kubectl get node "$node" \
            -o jsonpath='{.metadata.labels.topology\.kubernetes\.io/zone}{"\n"}'; done \
        | sort -u | wc -l | tr -d ' ')"
    [[ "$zone_count" == 2 ]] || {
        echo "$description pods were not placed in distinct zones" >&2
        return 1
    }
    local pod
    while read -r pod; do
        [[ "$(kubectl exec --namespace "$namespace" "$pod" -- id -u)" == 10001 ]] || {
            echo "$description pod $pod is not running with UID 10001" >&2
            return 1
        }
    done < <(jq -r '.[].metadata.name' <<< "$pods_json" | sort)
}

prove_post_transition_distribution() {
    local phase="$1" seconds="$2" minimum_success="$3" proof_field="$4" failure_message="$5"
    local attack_targets="${6:-$targets}"
    collect_distribution "${phase}-before"
    run_attack "$phase" "$seconds" "$minimum_success" "$attack_targets"
    collect_distribution "$phase"
    jq -n --arg phase "$phase" --arg proofField "$proof_field" \
        --slurpfile before "$output_dir/${phase}-before-distribution.json" \
        --slurpfile after "$output_dir/${phase}-distribution.json" '
          ($before[0]) as $before | ($after[0]) as $after |
          {phase: $phase,
           backendARequestDelta: ($after.backendARequests - $before.backendARequests),
           backendBRequestDelta: ($after.backendBRequests - $before.backendBRequests),
           pods: [$after.pods[] as $current
             | ($before.pods[] | select(.pod == $current.pod)) as $prior
             | {pod: $current.pod, requestDelta: ($current.requests - $prior.requests)}]}
          + {($proofField): true}
        ' > "$output_dir/${phase}-distribution-delta.json"
    jq -e '(.pods | length) == 2
        and all(.pods[]; .requestDelta > 0)
        and .backendARequestDelta > 0
        and .backendBRequestDelta > 0' "$output_dir/${phase}-distribution-delta.json" >/dev/null || {
        echo "$failure_message" >&2
        return 1
    }
}

proxy_pods_json="$(kubectl get pod --namespace "$namespace" -l app.kubernetes.io/name=loadbalancerpro -o json)"
initial_ready_proxy_pods_json="$(jq --arg revision "$source_revision" '[.items[]
    | select(.metadata.deletionTimestamp == null)
    | select(.status.phase == "Running")
    | select(any(.status.conditions[]?; .type == "Ready" and .status == "True"))
    | select(.metadata.annotations["loadbalancerpro.io/source-revision"] == $revision)]' \
    <<< "$proxy_pods_json")"
[[ "$(jq 'length' <<< "$initial_ready_proxy_pods_json")" == 2 ]] || {
    echo "Expected two ready initial proxy pods for the exact source revision" >&2; exit 1;
}
[[ "$(jq '[.[].spec.nodeName] | unique | length' <<< "$initial_ready_proxy_pods_json")" == 2 ]] || {
    echo "Proxy replicas were not placed on distinct workers" >&2; exit 1;
}
mapfile -t initial_proxy_pods < <(jq -r '.[].metadata.name' <<< "$initial_ready_proxy_pods_json" | sort)
for pod in "${initial_proxy_pods[@]}"; do
    [[ "$(kubectl exec --namespace "$namespace" "$pod" -- id -u)" == 10001 ]] || {
        echo "$pod is not running with the enforced non-root UID 10001" >&2; exit 1;
    }
done
proxy_zones="$(jq -r '.[].spec.nodeName' <<< "$initial_ready_proxy_pods_json" \
    | while read -r node; do kubectl get node "$node" -o jsonpath='{.metadata.labels.topology\.kubernetes\.io/zone}{"\n"}'; done \
    | sort -u | wc -l | tr -d ' ')"
[[ "$proxy_zones" == 2 ]] || { echo "Proxy replicas were not placed in distinct zones" >&2; exit 1; }
[[ "$(ready_endpoint_count)" == 2 ]] || { echo "Proxy Service did not publish two ready endpoints" >&2; exit 1; }
initial_proxy_uids_json="$(jq '[.[].metadata.uid] | sort' <<< "$initial_ready_proxy_pods_json")"
initial_proxy_runtime_image_ids_json="$(jq '[.[].status.containerStatuses[]?
    | select(.name == "loadbalancerpro") | .imageID] | unique | sort' \
    <<< "$initial_ready_proxy_pods_json")"
[[ "$(jq 'length' <<< "$initial_proxy_uids_json")" == 2 ]] || {
    echo "Expected two initial proxy pod UIDs" >&2; exit 1;
}
[[ "$(jq 'length' <<< "$initial_proxy_runtime_image_ids_json")" == 1 ]] || {
    echo "Initial proxy pods did not report one immutable runtime image ID" >&2; exit 1;
}
[[ "$(jq --arg secret "$baseline_tls_secret" '[.[] | any(.spec.volumes[]?;
    .name == "server-tls" and .secret.secretName == $secret)] | all' \
    <<< "$initial_ready_proxy_pods_json")" == true ]] || {
    echo "Initial proxy pods do not reference the immutable baseline TLS Secret" >&2; exit 1;
}
[[ "$(jq --arg secret "$baseline_api_key_secret" '[.[] | any(.spec.volumes[]?;
    .name == "api-key" and .secret.secretName == $secret)] | all' \
    <<< "$initial_ready_proxy_pods_json")" == true ]] || {
    echo "Initial proxy pods do not reference the immutable baseline API-key Secret" >&2; exit 1;
}
capture_state initial

api_key="$(<"$api_key_file")"
targets="$work_dir/targets.txt"
candidate_targets="$work_dir/candidate-targets.txt"
printf 'GET https://127.0.0.1:%s/proxy/kubernetes/topology\nX-API-Key: %s\n\n' \
    "$host_port" "$api_key" > "$targets"
printf 'GET https://127.0.0.1:%s/proxy/kubernetes/topology\nX-API-Key: %s\n\n' \
    "$host_port" "$(<"$candidate_api_key_file")" > "$candidate_targets"

api_key_status() {
    local key_file="$1"
    curl --silent --show-error --cacert "$tls_dir/ca.pem" --connect-timeout 3 --max-time 10 \
        --output /dev/null --write-out '%{http_code}' \
        --header "X-API-Key: $(<"$key_file")" \
        "https://127.0.0.1:${host_port}/api/proxy/status"
}

assert_api_key_status() {
    local key_file="$1" expected="$2" description="$3" observed
    observed="$(api_key_status "$key_file")"
    [[ "$observed" == "$expected" ]] || {
        echo "$description returned HTTP $observed instead of $expected" >&2
        return 1
    }
}

curl_with_ca() {
    local ca_file="$1"
    curl --silent --show-error --fail --cacert "$ca_file" --connect-timeout 3 --max-time 10 \
        --header "X-API-Key: $api_key" \
        "https://127.0.0.1:${host_port}/proxy/kubernetes/tls-identity" > /dev/null
}

assert_ca_trusts_endpoint() {
    local ca_file="$1" description="$2"
    curl_with_ca "$ca_file" || {
        echo "$description did not trust the served Kubernetes TLS identity" >&2
        return 1
    }
}

assert_ca_rejected_by_endpoint() {
    local ca_file="$1" description="$2"
    if curl_with_ca "$ca_file" >/dev/null 2>&1; then
        echo "$description unexpectedly trusted the served Kubernetes TLS identity" >&2
        return 1
    fi
}

served_certificate_fingerprint() {
    local ca_file="$1"
    openssl s_client -connect "127.0.0.1:${host_port}" -servername "$tls_hostname" \
        -CAfile "$ca_file" -verify_return_error </dev/null 2>/dev/null \
        | openssl x509 -noout -fingerprint -sha256 \
        | awk -F= '{print tolower($2)}' | tr -d ':'
}

assert_served_certificate() {
    local expected_fingerprint="$1" ca_file="$2" description="$3"
    local observed_fingerprint attempt
    for attempt in 1 2 3 4 5 6; do
        observed_fingerprint="$(served_certificate_fingerprint "$ca_file")"
        [[ "$observed_fingerprint" == "$expected_fingerprint" ]] || {
            echo "$description served unexpected certificate fingerprint $observed_fingerprint" >&2
            return 1
        }
    done
}

assert_ca_trusts_endpoint "$tls_dir/ca.pem" "Baseline CA"
assert_ca_rejected_by_endpoint "$candidate_tls_dir/ca.pem" "Candidate-only CA before rotation"
assert_served_certificate "$baseline_certificate_fingerprint" "$tls_dir/ca.pem" "Baseline TLS identity"
assert_api_key_status "$api_key_file" 200 "Baseline API key before rotation"
assert_api_key_status "$candidate_api_key_file" 401 "Candidate API key before overlap"

report_attack() {
    local name="$1" minimum_success="$2"
    local maximum_p99_millis="${3:-$(jq -r '.objectives.maximumP99Millis' "$profile")}"
    local binary="$work_dir/${name}.bin"
    vegeta report -type=json "$binary" > "$output_dir/${name}-client.json"
    vegeta report -type=text "$binary" > "$output_dir/${name}-client.txt"
    local maximum_p99_nanos
    maximum_p99_nanos="$((maximum_p99_millis * 1000000))"
    jq -e --argjson minimum "$minimum_success" --argjson maximumP99 "$maximum_p99_nanos" '
      .requests > 0
      and .success >= $minimum
      and .latencies["99th"] <= $maximumP99
      and ((.status_codes["200"] // 0) >= (.requests * $minimum))
    ' "$output_dir/${name}-client.json" >/dev/null || {
        echo "$name traffic did not satisfy success/status/p99 objectives" >&2
        cat "$output_dir/${name}-client.txt" >&2
        return 1
    }
}

run_attack() {
    local name="$1" seconds="$2" minimum_success="$3"
    local attack_targets="${4:-$targets}"
    local maximum_p99_millis="${5:-$(jq -r '.objectives.maximumP99Millis' "$profile")}"
    vegeta attack -duration="${seconds}s" -rate="${rate}/s" -timeout=5s -keepalive=false -http2=false \
        -root-certs="$tls_trust_bundle" -targets="$attack_targets" > "$work_dir/${name}.bin"
    report_attack "$name" "$minimum_success" "$maximum_p99_millis"
}

run_api_key_secret_transition() {
    local phase="$1" secret_name="$2" annotation_name="$3" attack_targets="$4"
    local prior_pods_json="$5"
    local duration_seconds maximum_seconds minimum_success token patch started_epoch elapsed_seconds
    local sample_count minimum_ready_pods minimum_ready_endpoints pods_json ready_pods_json
    local prior_uids_json current_uids_json retained_prior_uids runtime_image_ids_json pod
    duration_seconds="$(jq -r '.workload.apiKeyTransitionSeconds' "$profile")"
    maximum_seconds="$(jq -r '.objectives.maximumApiKeyTransitionSeconds' "$profile")"
    minimum_success="$(jq -r '.objectives.minimumApiKeyTransitionSuccessRatio' "$profile")"
    token="$(printf '%s\n' \
        "$source_revision|$secret_name|$default_run_id|$phase" | sha256sum | awk '{print $1}')"

    rm -f -- "$rollout_stop_file"
    sample_transition_continuity "$phase" &
    rollout_sampler_pid=$!
    vegeta attack -duration="${duration_seconds}s" -rate="${rate}/s" -timeout=5s \
        -keepalive=false -http2=false -root-certs="$tls_trust_bundle" -targets="$attack_targets" \
        > "$work_dir/${phase}.bin" &
    attack_pid=$!
    sleep 3
    started_epoch="$(date +%s)"
    patch="$(jq -cn --arg secret "$secret_name" --arg token "$token" \
        --arg annotation "$annotation_name" \
        '{spec:{template:{metadata:{annotations:{($annotation):$token,
          "loadbalancerpro.io/qualification-api-key-secret":$secret}},
          spec:{volumes:[{name:"api-key",secret:{secretName:$secret,items:[
            {key:"api-key",path:"loadbalancerpro.api.key"},
            {key:"rotation-key",path:"loadbalancerpro.api.rotation-key"}]}}]}}}}')"
    kubectl patch deployment loadbalancerpro --namespace "$namespace" --type strategic -p "$patch"
    kubectl rollout status deployment/loadbalancerpro --namespace "$namespace" \
        --timeout="${maximum_seconds}s"
    elapsed_seconds=$(( $(date +%s) - started_epoch ))
    (( elapsed_seconds <= maximum_seconds )) || {
        echo "$phase exceeded the API-key transition objective" >&2; return 1;
    }
    if ! wait "$attack_pid"; then
        attack_pid=""
        echo "$phase traffic attack failed" >&2
        return 1
    fi
    attack_pid=""
    touch "$rollout_stop_file"
    if ! wait "$rollout_sampler_pid"; then
        rollout_sampler_pid=""
        echo "$phase endpoint-continuity sampler failed" >&2
        return 1
    fi
    rollout_sampler_pid=""
    report_attack "$phase" "$minimum_success"
    sample_count="$(awk -F, 'NR > 1 { count++ } END { print count + 0 }' \
        "$output_dir/${phase}-continuity.csv")"
    minimum_ready_pods="$(awk -F, 'NR > 1 && (minimum == "" || $2 < minimum) { minimum = $2 }
        END { print minimum + 0 }' "$output_dir/${phase}-continuity.csv")"
    minimum_ready_endpoints="$(awk -F, 'NR > 1 && (minimum == "" || $3 < minimum) { minimum = $3 }
        END { print minimum + 0 }' "$output_dir/${phase}-continuity.csv")"
    (( sample_count >= 5 && minimum_ready_pods >= 2 && minimum_ready_endpoints >= 2 )) || {
        echo "$phase continuity evidence was incomplete" >&2; return 1;
    }

    pods_json="$(kubectl get pod --namespace "$namespace" \
        -l app.kubernetes.io/name=loadbalancerpro -o json)"
    ready_pods_json="$(jq --arg token "$token" --arg annotation "$annotation_name" \
        --arg secret "$secret_name" '[.items[]
          | select(.metadata.deletionTimestamp == null)
          | select(.status.phase == "Running")
          | select(any(.status.conditions[]?; .type == "Ready" and .status == "True"))
          | select(.metadata.annotations[$annotation] == $token)
          | select(any(.spec.volumes[]?; .name == "api-key" and .secret.secretName == $secret))]' \
        <<< "$pods_json")"
    assert_two_zone_proxy_pods "$ready_pods_json" "$phase"
    prior_uids_json="$(jq '[.[].metadata.uid] | sort' <<< "$prior_pods_json")"
    current_uids_json="$(jq '[.[].metadata.uid] | sort' <<< "$ready_pods_json")"
    retained_prior_uids="$(jq -n --argjson prior "$prior_uids_json" \
        --argjson current "$current_uids_json" '[ $current[] | select(. as $uid | $prior | index($uid)) ] | length')"
    [[ "$retained_prior_uids" == 0 ]] || {
        echo "$phase retained a prior API-key pod UID" >&2; return 1;
    }
    runtime_image_ids_json="$(jq '[.[].status.containerStatuses[]?
        | select(.name == "loadbalancerpro") | .imageID] | unique | sort' <<< "$ready_pods_json")"
    [[ "$runtime_image_ids_json" == "$initial_proxy_runtime_image_ids_json" ]] || {
        echo "$phase changed the immutable runtime image ID" >&2; return 1;
    }
    while read -r pod; do
        kubectl wait --for=delete "pod/$pod" --namespace "$namespace" --timeout="${maximum_seconds}s"
    done < <(jq -r '.[].metadata.name' <<< "$prior_pods_json" | sort)
    capture_state "$phase"

    api_key_transition_token="$token"
    api_key_transition_elapsed_seconds="$elapsed_seconds"
    api_key_transition_sample_count="$sample_count"
    api_key_transition_minimum_ready_pods="$minimum_ready_pods"
    api_key_transition_minimum_ready_endpoints="$minimum_ready_endpoints"
    api_key_transition_ready_pods_json="$ready_pods_json"
    api_key_transition_uids_json="$current_uids_json"
}

baseline_seconds="$(jq -r '.workload.baselineSeconds' "$profile")"
run_attack baseline "$baseline_seconds" "$(jq -r '.objectives.minimumBaselineSuccessRatio' "$profile")"
collect_distribution baseline

rollout_duration_seconds="$(jq -r '.workload.rolloutSeconds' "$profile")"
maximum_rollout_seconds="$(jq -r '.objectives.maximumRolloutSeconds' "$profile")"
rollout_token="$(printf '%s\n' "$source_revision|$candidate_image_id|$default_run_id|candidate-rollout" \
    | sha256sum | awk '{print $1}')"
rm -f -- "$rollout_stop_file"
sample_transition_continuity rollout &
rollout_sampler_pid=$!
vegeta attack -duration="${rollout_duration_seconds}s" -rate="${rate}/s" -timeout=5s \
    -keepalive=false -http2=false -root-certs="$tls_trust_bundle" -targets="$targets" \
    > "$work_dir/rollout.bin" &
attack_pid=$!
sleep 3
rollout_started_epoch="$(date +%s)"
rollout_patch="$(jq -cn --arg image "$candidate_image" --arg token "$rollout_token" \
    --arg release "$candidate_release_id" \
    '{spec:{template:{metadata:{annotations:{"loadbalancerpro.io/qualification-rollout":$token,
      "loadbalancerpro.io/qualification-release":$release}},
      spec:{containers:[{name:"loadbalancerpro",image:$image}]}}}}')"
kubectl patch deployment loadbalancerpro --namespace "$namespace" --type strategic -p "$rollout_patch"
kubectl rollout status deployment/loadbalancerpro --namespace "$namespace" \
    --timeout="${maximum_rollout_seconds}s"
rollout_elapsed_seconds=$(( $(date +%s) - rollout_started_epoch ))
(( rollout_elapsed_seconds <= maximum_rollout_seconds )) || {
    echo "Rolling replacement exceeded the rollout objective" >&2; exit 1;
}
if ! wait "$attack_pid"; then
    attack_pid=""
    echo "Rolling replacement traffic attack failed" >&2
    exit 1
fi
attack_pid=""
: > "$rollout_stop_file"
if ! wait "$rollout_sampler_pid"; then
    rollout_sampler_pid=""
    echo "Rolling replacement endpoint-continuity sampler failed" >&2
    exit 1
fi
rollout_sampler_pid=""
report_attack rollout "$(jq -r '.objectives.minimumRolloutSuccessRatio' "$profile")"
rollout_sample_count="$(awk -F, 'NR > 1 { count++ } END { print count + 0 }' \
    "$output_dir/rollout-continuity.csv")"
rollout_min_ready_pods="$(awk -F, 'NR > 1 && (minimum == "" || $2 < minimum) { minimum = $2 }
    END { print minimum + 0 }' "$output_dir/rollout-continuity.csv")"
rollout_min_ready_endpoints="$(awk -F, 'NR > 1 && (minimum == "" || $3 < minimum) { minimum = $3 }
    END { print minimum + 0 }' "$output_dir/rollout-continuity.csv")"
(( rollout_sample_count >= 5 && rollout_min_ready_pods >= 2 && rollout_min_ready_endpoints >= 2 )) || {
    echo "Rolling replacement continuity evidence was incomplete" >&2; exit 1;
}

replacement_proxy_pods_json="$(kubectl get pod --namespace "$namespace" \
    -l app.kubernetes.io/name=loadbalancerpro -o json)"
replacement_ready_proxy_pods_json="$(jq --arg token "$rollout_token" '[.items[]
    | select(.metadata.deletionTimestamp == null)
    | select(.status.phase == "Running")
    | select(any(.status.conditions[]?; .type == "Ready" and .status == "True"))
    | select(.metadata.annotations["loadbalancerpro.io/qualification-rollout"] == $token)]' \
    <<< "$replacement_proxy_pods_json")"
[[ "$(jq 'length' <<< "$replacement_ready_proxy_pods_json")" == 2 ]] || {
    echo "Rolling replacement did not converge to two ready replacement pods" >&2; exit 1;
}
[[ "$(jq --arg image "$candidate_image" '[.[] | any(.spec.containers[]?;
    .name == "loadbalancerpro" and .image == $image)] | all' \
    <<< "$replacement_ready_proxy_pods_json")" == true ]] || {
    echo "Candidate proxy pods do not reference the expected local candidate image" >&2; exit 1;
}
[[ "$(ready_proxy_count)" == 2 && "$(ready_endpoint_count)" == 2 ]] || {
    echo "Rolling replacement did not restore exactly two ready pods and Service endpoints" >&2; exit 1;
}
replacement_proxy_uids_json="$(jq '[.[].metadata.uid] | sort' <<< "$replacement_ready_proxy_pods_json")"
rollout_old_uid_overlap="$(jq -n --argjson prior "$initial_proxy_uids_json" \
    --argjson replacement "$replacement_proxy_uids_json" \
    '[ $prior[] as $uid | $replacement[] | select(. == $uid) ] | length')"
[[ "$rollout_old_uid_overlap" == 0 ]] || {
    echo "Rolling replacement retained an initial proxy pod UID" >&2; exit 1;
}
replacement_proxy_runtime_image_ids_json="$(jq '[.[].status.containerStatuses[]?
    | select(.name == "loadbalancerpro") | .imageID] | unique | sort' \
    <<< "$replacement_ready_proxy_pods_json")"
[[ "$(jq 'length' <<< "$replacement_proxy_runtime_image_ids_json")" == 1 ]] || {
    echo "Candidate proxy pods did not converge to one immutable runtime image ID" >&2; exit 1;
}
[[ "$replacement_proxy_runtime_image_ids_json" != "$initial_proxy_runtime_image_ids_json" ]] || {
    echo "Candidate rollout did not change the immutable runtime image ID" >&2; exit 1;
}
[[ "$(jq '[.[].spec.nodeName] | unique | length' <<< "$replacement_ready_proxy_pods_json")" == 2 ]] || {
    echo "Replacement proxy pods were not restored to distinct workers" >&2; exit 1;
}
replacement_proxy_zones="$(jq -r '.[].spec.nodeName' <<< "$replacement_ready_proxy_pods_json" \
    | while read -r node; do kubectl get node "$node" \
        -o jsonpath='{.metadata.labels.topology\.kubernetes\.io/zone}{"\n"}'; done \
    | sort -u | wc -l | tr -d ' ')"
[[ "$replacement_proxy_zones" == 2 ]] || {
    echo "Replacement proxy pods were not restored to distinct zones" >&2; exit 1;
}
mapfile -t replacement_proxy_pods < <(jq -r '.[].metadata.name' \
    <<< "$replacement_ready_proxy_pods_json" | sort)
for pod in "${replacement_proxy_pods[@]}"; do
    [[ "$(kubectl exec --namespace "$namespace" "$pod" -- id -u)" == 10001 ]] || {
        echo "$pod replacement is not running with UID 10001" >&2; exit 1;
    }
done
capture_state post-rollout
collect_distribution post-rollout-before
run_attack post-rollout "$(jq -r '.workload.postRolloutSeconds' "$profile")" \
    "$(jq -r '.objectives.minimumPostRolloutSuccessRatio' "$profile")"
collect_distribution post-rollout
jq -n --slurpfile before "$output_dir/post-rollout-before-distribution.json" \
    --slurpfile after "$output_dir/post-rollout-distribution.json" '
      ($before[0]) as $before | ($after[0]) as $after |
      {phase: "post-rollout", bothReplacementProxyReplicasServed: true,
       backendARequestDelta: ($after.backendARequests - $before.backendARequests),
       backendBRequestDelta: ($after.backendBRequests - $before.backendBRequests),
       pods: [$after.pods[] as $current
         | ($before.pods[] | select(.pod == $current.pod)) as $prior
         | {pod: $current.pod, requestDelta: ($current.requests - $prior.requests)}]}
    ' > "$output_dir/post-rollout-distribution-delta.json"
jq -e '(.pods | length) == 2
    and all(.pods[]; .requestDelta > 0)
    and .backendARequestDelta > 0
    and .backendBRequestDelta > 0' "$output_dir/post-rollout-distribution-delta.json" >/dev/null || {
    echo "Both replacement proxies and both backends must serve post-rollout traffic" >&2; exit 1;
}

rollback_duration_seconds="$(jq -r '.workload.rollbackSeconds' "$profile")"
maximum_rollback_seconds="$(jq -r '.objectives.maximumRollbackSeconds' "$profile")"
rollback_token="$(printf '%s\n' "$source_revision|$proxy_image_id|$default_run_id|baseline-rollback" \
    | sha256sum | awk '{print $1}')"
baseline_release_id="baseline-${source_revision:0:12}"
rm -f -- "$rollout_stop_file"
sample_transition_continuity rollback &
rollout_sampler_pid=$!
vegeta attack -duration="${rollback_duration_seconds}s" -rate="${rate}/s" -timeout=5s \
    -keepalive=false -http2=false -root-certs="$tls_trust_bundle" -targets="$targets" \
    > "$work_dir/rollback.bin" &
attack_pid=$!
sleep 3
rollback_started_epoch="$(date +%s)"
rollback_patch="$(jq -cn --arg image "$proxy_image" --arg token "$rollback_token" \
    --arg release "$baseline_release_id" \
    '{spec:{template:{metadata:{annotations:{"loadbalancerpro.io/qualification-rollout":$token,
      "loadbalancerpro.io/qualification-release":$release}},
      spec:{containers:[{name:"loadbalancerpro",image:$image}]}}}}')"
kubectl patch deployment loadbalancerpro --namespace "$namespace" --type strategic -p "$rollback_patch"
kubectl rollout status deployment/loadbalancerpro --namespace "$namespace" \
    --timeout="${maximum_rollback_seconds}s"
rollback_elapsed_seconds=$(( $(date +%s) - rollback_started_epoch ))
(( rollback_elapsed_seconds <= maximum_rollback_seconds )) || {
    echo "Baseline rollback exceeded the rollback objective" >&2; exit 1;
}
if ! wait "$attack_pid"; then
    attack_pid=""
    echo "Baseline rollback traffic attack failed" >&2
    exit 1
fi
attack_pid=""
: > "$rollout_stop_file"
if ! wait "$rollout_sampler_pid"; then
    rollout_sampler_pid=""
    echo "Baseline rollback endpoint-continuity sampler failed" >&2
    exit 1
fi
rollout_sampler_pid=""
report_attack rollback "$(jq -r '.objectives.minimumRollbackSuccessRatio' "$profile")"
rollback_sample_count="$(awk -F, 'NR > 1 { count++ } END { print count + 0 }' \
    "$output_dir/rollback-continuity.csv")"
rollback_min_ready_pods="$(awk -F, 'NR > 1 && (minimum == "" || $2 < minimum) { minimum = $2 }
    END { print minimum + 0 }' "$output_dir/rollback-continuity.csv")"
rollback_min_ready_endpoints="$(awk -F, 'NR > 1 && (minimum == "" || $3 < minimum) { minimum = $3 }
    END { print minimum + 0 }' "$output_dir/rollback-continuity.csv")"
(( rollback_sample_count >= 5 && rollback_min_ready_pods >= 2 && rollback_min_ready_endpoints >= 2 )) || {
    echo "Baseline rollback continuity evidence was incomplete" >&2; exit 1;
}

rollback_proxy_pods_json="$(kubectl get pod --namespace "$namespace" \
    -l app.kubernetes.io/name=loadbalancerpro -o json)"
rollback_ready_proxy_pods_json="$(jq --arg token "$rollback_token" '[.items[]
    | select(.metadata.deletionTimestamp == null)
    | select(.status.phase == "Running")
    | select(any(.status.conditions[]?; .type == "Ready" and .status == "True"))
    | select(.metadata.annotations["loadbalancerpro.io/qualification-rollout"] == $token)]' \
    <<< "$rollback_proxy_pods_json")"
[[ "$(jq 'length' <<< "$rollback_ready_proxy_pods_json")" == 2 ]] || {
    echo "Baseline rollback did not converge to two ready restored pods" >&2; exit 1;
}
[[ "$(jq --arg image "$proxy_image" '[.[] | any(.spec.containers[]?;
    .name == "loadbalancerpro" and .image == $image)] | all' \
    <<< "$rollback_ready_proxy_pods_json")" == true ]] || {
    echo "Restored proxy pods do not reference the expected baseline image" >&2; exit 1;
}
[[ "$(ready_proxy_count)" == 2 && "$(ready_endpoint_count)" == 2 ]] || {
    echo "Baseline rollback did not restore exactly two ready pods and Service endpoints" >&2; exit 1;
}
rollback_proxy_uids_json="$(jq '[.[].metadata.uid] | sort' <<< "$rollback_ready_proxy_pods_json")"
rollback_candidate_uid_overlap="$(jq -n --argjson candidate "$replacement_proxy_uids_json" \
    --argjson restored "$rollback_proxy_uids_json" \
    '[ $candidate[] as $uid | $restored[] | select(. == $uid) ] | length')"
rollback_initial_uid_overlap="$(jq -n --argjson initial "$initial_proxy_uids_json" \
    --argjson restored "$rollback_proxy_uids_json" \
    '[ $initial[] as $uid | $restored[] | select(. == $uid) ] | length')"
[[ "$rollback_candidate_uid_overlap" == 0 && "$rollback_initial_uid_overlap" == 0 ]] || {
    echo "Baseline rollback retained a prior proxy pod UID" >&2; exit 1;
}
rollback_proxy_runtime_image_ids_json="$(jq '[.[].status.containerStatuses[]?
    | select(.name == "loadbalancerpro") | .imageID] | unique | sort' \
    <<< "$rollback_ready_proxy_pods_json")"
[[ "$rollback_proxy_runtime_image_ids_json" == "$initial_proxy_runtime_image_ids_json" ]] || {
    echo "Baseline rollback did not restore the initial immutable runtime image ID" >&2; exit 1;
}
[[ "$(jq '[.[].spec.nodeName] | unique | length' <<< "$rollback_ready_proxy_pods_json")" == 2 ]] || {
    echo "Restored baseline proxy pods were not placed on distinct workers" >&2; exit 1;
}
rollback_proxy_zones="$(jq -r '.[].spec.nodeName' <<< "$rollback_ready_proxy_pods_json" \
    | while read -r node; do kubectl get node "$node" \
        -o jsonpath='{.metadata.labels.topology\.kubernetes\.io/zone}{"\n"}'; done \
    | sort -u | wc -l | tr -d ' ')"
[[ "$rollback_proxy_zones" == 2 ]] || {
    echo "Restored baseline proxy pods were not placed in distinct zones" >&2; exit 1;
}
mapfile -t rollback_proxy_pods < <(jq -r '.[].metadata.name' <<< "$rollback_ready_proxy_pods_json" | sort)
for pod in "${rollback_proxy_pods[@]}"; do
    [[ "$(kubectl exec --namespace "$namespace" "$pod" -- id -u)" == 10001 ]] || {
        echo "$pod restored baseline is not running with UID 10001" >&2; exit 1;
    }
done
capture_state post-rollback
collect_distribution post-rollback-before
run_attack post-rollback "$(jq -r '.workload.postRollbackSeconds' "$profile")" \
    "$(jq -r '.objectives.minimumPostRollbackSuccessRatio' "$profile")"
collect_distribution post-rollback
jq -n --slurpfile before "$output_dir/post-rollback-before-distribution.json" \
    --slurpfile after "$output_dir/post-rollback-distribution.json" '
      ($before[0]) as $before | ($after[0]) as $after |
      {phase: "post-rollback", bothRestoredBaselineProxyReplicasServed: true,
       backendARequestDelta: ($after.backendARequests - $before.backendARequests),
       backendBRequestDelta: ($after.backendBRequests - $before.backendBRequests),
       pods: [$after.pods[] as $current
         | ($before.pods[] | select(.pod == $current.pod)) as $prior
         | {pod: $current.pod, requestDelta: ($current.requests - $prior.requests)}]}
    ' > "$output_dir/post-rollback-distribution-delta.json"
jq -e '(.pods | length) == 2
    and all(.pods[]; .requestDelta > 0)
    and .backendARequestDelta > 0
    and .backendBRequestDelta > 0' "$output_dir/post-rollback-distribution-delta.json" >/dev/null || {
    echo "Both restored baseline proxies and both backends must serve post-rollback traffic" >&2; exit 1;
}

certificate_rotation_duration_seconds="$(jq -r '.workload.certificateRotationSeconds' "$profile")"
maximum_certificate_rotation_seconds="$(jq -r '.objectives.maximumCertificateRotationSeconds' "$profile")"
certificate_rotation_token="$(printf '%s\n' \
    "$source_revision|$baseline_certificate_fingerprint|$candidate_certificate_fingerprint|$default_run_id|certificate-rotation" \
    | sha256sum | awk '{print $1}')"
rm -f -- "$rollout_stop_file"
sample_transition_continuity certificate-rotation &
rollout_sampler_pid=$!
vegeta attack -duration="${certificate_rotation_duration_seconds}s" -rate="${rate}/s" -timeout=5s \
    -keepalive=false -http2=false -root-certs="$tls_trust_bundle" -targets="$targets" \
    > "$work_dir/certificate-rotation.bin" &
attack_pid=$!
sleep 3
certificate_rotation_started_epoch="$(date +%s)"
certificate_rotation_patch="$(jq -cn --arg secret "$candidate_tls_secret" \
    --arg token "$certificate_rotation_token" \
    '{spec:{template:{metadata:{annotations:{"loadbalancerpro.io/qualification-tls-rotation":$token,
      "loadbalancerpro.io/qualification-tls-secret":$secret}},
      spec:{volumes:[{name:"server-tls",secret:{secretName:$secret,items:[
        {key:"tls.crt",path:"certificate.pem"},{key:"tls.key",path:"private-key.pem"},
        {key:"ca.crt",path:"ca.pem"}]}}]}}}}')"
kubectl patch deployment loadbalancerpro --namespace "$namespace" --type strategic \
    -p "$certificate_rotation_patch"
kubectl rollout status deployment/loadbalancerpro --namespace "$namespace" \
    --timeout="${maximum_certificate_rotation_seconds}s"
certificate_rotation_elapsed_seconds=$(( $(date +%s) - certificate_rotation_started_epoch ))
(( certificate_rotation_elapsed_seconds <= maximum_certificate_rotation_seconds )) || {
    echo "Certificate rotation exceeded the rotation objective" >&2; exit 1;
}
if ! wait "$attack_pid"; then
    attack_pid=""
    echo "Certificate rotation traffic attack failed" >&2
    exit 1
fi
attack_pid=""
: > "$rollout_stop_file"
if ! wait "$rollout_sampler_pid"; then
    rollout_sampler_pid=""
    echo "Certificate rotation endpoint-continuity sampler failed" >&2
    exit 1
fi
rollout_sampler_pid=""
report_attack certificate-rotation \
    "$(jq -r '.objectives.minimumCertificateRotationSuccessRatio' "$profile")"
certificate_rotation_sample_count="$(awk -F, 'NR > 1 { count++ } END { print count + 0 }' \
    "$output_dir/certificate-rotation-continuity.csv")"
certificate_rotation_min_ready_pods="$(awk -F, \
    'NR > 1 && (minimum == "" || $2 < minimum) { minimum = $2 } END { print minimum + 0 }' \
    "$output_dir/certificate-rotation-continuity.csv")"
certificate_rotation_min_ready_endpoints="$(awk -F, \
    'NR > 1 && (minimum == "" || $3 < minimum) { minimum = $3 } END { print minimum + 0 }' \
    "$output_dir/certificate-rotation-continuity.csv")"
(( certificate_rotation_sample_count >= 5 && certificate_rotation_min_ready_pods >= 2 \
    && certificate_rotation_min_ready_endpoints >= 2 )) || {
    echo "Certificate rotation continuity evidence was incomplete" >&2; exit 1;
}

certificate_rotation_proxy_pods_json="$(kubectl get pod --namespace "$namespace" \
    -l app.kubernetes.io/name=loadbalancerpro -o json)"
certificate_rotation_ready_proxy_pods_json="$(jq --arg token "$certificate_rotation_token" \
    --arg secret "$candidate_tls_secret" '[.items[]
      | select(.metadata.deletionTimestamp == null)
      | select(.status.phase == "Running")
      | select(any(.status.conditions[]?; .type == "Ready" and .status == "True"))
      | select(.metadata.annotations["loadbalancerpro.io/qualification-tls-rotation"] == $token)
      | select(any(.spec.volumes[]?; .name == "server-tls" and .secret.secretName == $secret))]' \
    <<< "$certificate_rotation_proxy_pods_json")"
assert_two_zone_proxy_pods "$certificate_rotation_ready_proxy_pods_json" "Certificate rotation"
certificate_rotation_proxy_uids_json="$(jq '[.[].metadata.uid] | sort' \
    <<< "$certificate_rotation_ready_proxy_pods_json")"
certificate_rotation_prior_uid_overlap="$(jq -n --argjson prior "$rollback_proxy_uids_json" \
    --argjson rotated "$certificate_rotation_proxy_uids_json" \
    '[ $prior[] as $uid | $rotated[] | select(. == $uid) ] | length')"
[[ "$certificate_rotation_prior_uid_overlap" == 0 ]] || {
    echo "Certificate rotation retained a baseline-certificate pod UID" >&2; exit 1;
}
certificate_rotation_runtime_image_ids_json="$(jq '[.[].status.containerStatuses[]?
    | select(.name == "loadbalancerpro") | .imageID] | unique | sort' \
    <<< "$certificate_rotation_ready_proxy_pods_json")"
[[ "$certificate_rotation_runtime_image_ids_json" == "$initial_proxy_runtime_image_ids_json" ]] || {
    echo "Certificate rotation changed the immutable runtime image ID" >&2; exit 1;
}
mapfile -t certificate_rotation_prior_pods < <(jq -r '.[].metadata.name' \
    <<< "$rollback_ready_proxy_pods_json" | sort)
for pod in "${certificate_rotation_prior_pods[@]}"; do
    kubectl wait --for=delete "pod/$pod" --namespace "$namespace" \
        --timeout="${maximum_certificate_rotation_seconds}s"
done
assert_ca_trusts_endpoint "$candidate_tls_dir/ca.pem" "Candidate CA after rotation"
assert_ca_rejected_by_endpoint "$tls_dir/ca.pem" "Baseline-only CA after rotation"
assert_served_certificate "$candidate_certificate_fingerprint" "$candidate_tls_dir/ca.pem" \
    "Rotated TLS identity"
capture_state post-certificate-rotation
prove_post_transition_distribution post-certificate-rotation \
    "$(jq -r '.workload.postCertificateRotationSeconds' "$profile")" \
    "$(jq -r '.objectives.minimumPostCertificateRotationSuccessRatio' "$profile")" \
    bothRotatedCertificateProxyReplicasServed \
    "Both rotated-certificate proxies and both backends must serve post-rotation traffic"

certificate_rollback_duration_seconds="$(jq -r '.workload.certificateRollbackSeconds' "$profile")"
maximum_certificate_rollback_seconds="$(jq -r '.objectives.maximumCertificateRollbackSeconds' "$profile")"
certificate_rollback_token="$(printf '%s\n' \
    "$source_revision|$candidate_certificate_fingerprint|$baseline_certificate_fingerprint|$default_run_id|certificate-rollback" \
    | sha256sum | awk '{print $1}')"
rm -f -- "$rollout_stop_file"
sample_transition_continuity certificate-rollback &
rollout_sampler_pid=$!
vegeta attack -duration="${certificate_rollback_duration_seconds}s" -rate="${rate}/s" -timeout=5s \
    -keepalive=false -http2=false -root-certs="$tls_trust_bundle" -targets="$targets" \
    > "$work_dir/certificate-rollback.bin" &
attack_pid=$!
sleep 3
certificate_rollback_started_epoch="$(date +%s)"
certificate_rollback_patch="$(jq -cn --arg secret "$baseline_tls_secret" \
    --arg token "$certificate_rollback_token" \
    '{spec:{template:{metadata:{annotations:{"loadbalancerpro.io/qualification-tls-rollback":$token,
      "loadbalancerpro.io/qualification-tls-secret":$secret}},
      spec:{volumes:[{name:"server-tls",secret:{secretName:$secret,items:[
        {key:"tls.crt",path:"certificate.pem"},{key:"tls.key",path:"private-key.pem"},
        {key:"ca.crt",path:"ca.pem"}]}}]}}}}')"
kubectl patch deployment loadbalancerpro --namespace "$namespace" --type strategic \
    -p "$certificate_rollback_patch"
kubectl rollout status deployment/loadbalancerpro --namespace "$namespace" \
    --timeout="${maximum_certificate_rollback_seconds}s"
certificate_rollback_elapsed_seconds=$(( $(date +%s) - certificate_rollback_started_epoch ))
(( certificate_rollback_elapsed_seconds <= maximum_certificate_rollback_seconds )) || {
    echo "Certificate rollback exceeded the rollback objective" >&2; exit 1;
}
if ! wait "$attack_pid"; then
    attack_pid=""
    echo "Certificate rollback traffic attack failed" >&2
    exit 1
fi
attack_pid=""
: > "$rollout_stop_file"
if ! wait "$rollout_sampler_pid"; then
    rollout_sampler_pid=""
    echo "Certificate rollback endpoint-continuity sampler failed" >&2
    exit 1
fi
rollout_sampler_pid=""
report_attack certificate-rollback \
    "$(jq -r '.objectives.minimumCertificateRollbackSuccessRatio' "$profile")"
certificate_rollback_sample_count="$(awk -F, 'NR > 1 { count++ } END { print count + 0 }' \
    "$output_dir/certificate-rollback-continuity.csv")"
certificate_rollback_min_ready_pods="$(awk -F, \
    'NR > 1 && (minimum == "" || $2 < minimum) { minimum = $2 } END { print minimum + 0 }' \
    "$output_dir/certificate-rollback-continuity.csv")"
certificate_rollback_min_ready_endpoints="$(awk -F, \
    'NR > 1 && (minimum == "" || $3 < minimum) { minimum = $3 } END { print minimum + 0 }' \
    "$output_dir/certificate-rollback-continuity.csv")"
(( certificate_rollback_sample_count >= 5 && certificate_rollback_min_ready_pods >= 2 \
    && certificate_rollback_min_ready_endpoints >= 2 )) || {
    echo "Certificate rollback continuity evidence was incomplete" >&2; exit 1;
}

certificate_rollback_proxy_pods_json="$(kubectl get pod --namespace "$namespace" \
    -l app.kubernetes.io/name=loadbalancerpro -o json)"
certificate_rollback_ready_proxy_pods_json="$(jq --arg token "$certificate_rollback_token" \
    --arg secret "$baseline_tls_secret" '[.items[]
      | select(.metadata.deletionTimestamp == null)
      | select(.status.phase == "Running")
      | select(any(.status.conditions[]?; .type == "Ready" and .status == "True"))
      | select(.metadata.annotations["loadbalancerpro.io/qualification-tls-rollback"] == $token)
      | select(any(.spec.volumes[]?; .name == "server-tls" and .secret.secretName == $secret))]' \
    <<< "$certificate_rollback_proxy_pods_json")"
assert_two_zone_proxy_pods "$certificate_rollback_ready_proxy_pods_json" "Certificate rollback"
certificate_rollback_proxy_uids_json="$(jq '[.[].metadata.uid] | sort' \
    <<< "$certificate_rollback_ready_proxy_pods_json")"
certificate_rollback_candidate_uid_overlap="$(jq -n \
    --argjson candidate "$certificate_rotation_proxy_uids_json" \
    --argjson restored "$certificate_rollback_proxy_uids_json" \
    '[ $candidate[] as $uid | $restored[] | select(. == $uid) ] | length')"
certificate_rollback_prior_uid_overlap="$(jq -n --argjson prior "$rollback_proxy_uids_json" \
    --argjson restored "$certificate_rollback_proxy_uids_json" \
    '[ $prior[] as $uid | $restored[] | select(. == $uid) ] | length')"
[[ "$certificate_rollback_candidate_uid_overlap" == 0 \
    && "$certificate_rollback_prior_uid_overlap" == 0 ]] || {
    echo "Certificate rollback retained a prior TLS pod UID" >&2; exit 1;
}
certificate_rollback_runtime_image_ids_json="$(jq '[.[].status.containerStatuses[]?
    | select(.name == "loadbalancerpro") | .imageID] | unique | sort' \
    <<< "$certificate_rollback_ready_proxy_pods_json")"
[[ "$certificate_rollback_runtime_image_ids_json" == "$initial_proxy_runtime_image_ids_json" ]] || {
    echo "Certificate rollback changed the immutable runtime image ID" >&2; exit 1;
}
mapfile -t certificate_rollback_prior_pods < <(jq -r '.[].metadata.name' \
    <<< "$certificate_rotation_ready_proxy_pods_json" | sort)
for pod in "${certificate_rollback_prior_pods[@]}"; do
    kubectl wait --for=delete "pod/$pod" --namespace "$namespace" \
        --timeout="${maximum_certificate_rollback_seconds}s"
done
assert_ca_trusts_endpoint "$tls_dir/ca.pem" "Baseline CA after certificate rollback"
assert_ca_rejected_by_endpoint "$candidate_tls_dir/ca.pem" "Candidate-only CA after certificate rollback"
assert_served_certificate "$baseline_certificate_fingerprint" "$tls_dir/ca.pem" \
    "Restored baseline TLS identity"
capture_state post-certificate-rollback
prove_post_transition_distribution post-certificate-rollback \
    "$(jq -r '.workload.postCertificateRollbackSeconds' "$profile")" \
    "$(jq -r '.objectives.minimumPostCertificateRollbackSuccessRatio' "$profile")" \
    bothRestoredCertificateProxyReplicasServed \
    "Both restored-certificate proxies and both backends must serve post-certificate-rollback traffic"

post_api_key_transition_seconds="$(jq -r '.workload.postApiKeyTransitionSeconds' "$profile")"
minimum_post_api_key_transition_success_ratio="$(jq -r \
    '.objectives.minimumPostApiKeyTransitionSuccessRatio' "$profile")"

run_api_key_secret_transition api-key-overlap "$overlap_api_key_secret" \
    loadbalancerpro.io/qualification-api-key-overlap "$targets" \
    "$certificate_rollback_ready_proxy_pods_json"
api_key_overlap_token="$api_key_transition_token"
api_key_overlap_elapsed_seconds="$api_key_transition_elapsed_seconds"
api_key_overlap_sample_count="$api_key_transition_sample_count"
api_key_overlap_minimum_ready_pods="$api_key_transition_minimum_ready_pods"
api_key_overlap_minimum_ready_endpoints="$api_key_transition_minimum_ready_endpoints"
api_key_overlap_pods_json="$api_key_transition_ready_pods_json"
api_key_overlap_uids_json="$api_key_transition_uids_json"
assert_api_key_status "$api_key_file" 200 "Baseline API key during overlap"
assert_api_key_status "$candidate_api_key_file" 200 "Candidate API key during overlap"
prove_post_transition_distribution post-api-key-overlap "$post_api_key_transition_seconds" \
    "$minimum_post_api_key_transition_success_ratio" bothApiKeysAcceptedDuringOverlap \
    "Both overlap proxies and both backends must serve baseline-key traffic" "$targets"

run_api_key_secret_transition api-key-commit "$candidate_api_key_secret" \
    loadbalancerpro.io/qualification-api-key-commit "$candidate_targets" \
    "$api_key_overlap_pods_json"
api_key_commit_token="$api_key_transition_token"
api_key_commit_elapsed_seconds="$api_key_transition_elapsed_seconds"
api_key_commit_sample_count="$api_key_transition_sample_count"
api_key_commit_minimum_ready_pods="$api_key_transition_minimum_ready_pods"
api_key_commit_minimum_ready_endpoints="$api_key_transition_minimum_ready_endpoints"
api_key_commit_pods_json="$api_key_transition_ready_pods_json"
api_key_commit_uids_json="$api_key_transition_uids_json"
assert_api_key_status "$candidate_api_key_file" 200 "Candidate API key after commit"
assert_api_key_status "$api_key_file" 401 "Retired baseline API key after commit"
prove_post_transition_distribution post-api-key-commit "$post_api_key_transition_seconds" \
    "$minimum_post_api_key_transition_success_ratio" baselineApiKeyRetired \
    "Both committed-key proxies and both backends must serve candidate-key traffic" "$candidate_targets"

run_api_key_secret_transition api-key-rollback-overlap "$overlap_api_key_secret" \
    loadbalancerpro.io/qualification-api-key-rollback-overlap "$candidate_targets" \
    "$api_key_commit_pods_json"
api_key_rollback_overlap_token="$api_key_transition_token"
api_key_rollback_overlap_elapsed_seconds="$api_key_transition_elapsed_seconds"
api_key_rollback_overlap_sample_count="$api_key_transition_sample_count"
api_key_rollback_overlap_minimum_ready_pods="$api_key_transition_minimum_ready_pods"
api_key_rollback_overlap_minimum_ready_endpoints="$api_key_transition_minimum_ready_endpoints"
api_key_rollback_overlap_pods_json="$api_key_transition_ready_pods_json"
api_key_rollback_overlap_uids_json="$api_key_transition_uids_json"
assert_api_key_status "$api_key_file" 200 "Baseline API key during rollback overlap"
assert_api_key_status "$candidate_api_key_file" 200 "Candidate API key during rollback overlap"
prove_post_transition_distribution post-api-key-rollback-overlap "$post_api_key_transition_seconds" \
    "$minimum_post_api_key_transition_success_ratio" bothApiKeysAcceptedDuringRollbackOverlap \
    "Both rollback-overlap proxies and both backends must serve candidate-key traffic" "$candidate_targets"

run_api_key_secret_transition api-key-rollback-commit "$baseline_api_key_secret" \
    loadbalancerpro.io/qualification-api-key-rollback-commit "$targets" \
    "$api_key_rollback_overlap_pods_json"
api_key_rollback_commit_token="$api_key_transition_token"
api_key_rollback_commit_elapsed_seconds="$api_key_transition_elapsed_seconds"
api_key_rollback_commit_sample_count="$api_key_transition_sample_count"
api_key_rollback_commit_minimum_ready_pods="$api_key_transition_minimum_ready_pods"
api_key_rollback_commit_minimum_ready_endpoints="$api_key_transition_minimum_ready_endpoints"
api_key_rollback_commit_pods_json="$api_key_transition_ready_pods_json"
api_key_rollback_commit_uids_json="$api_key_transition_uids_json"
assert_api_key_status "$api_key_file" 200 "Restored baseline API key after rollback"
assert_api_key_status "$candidate_api_key_file" 401 "Retired candidate API key after rollback"
prove_post_transition_distribution post-api-key-rollback-commit "$post_api_key_transition_seconds" \
    "$minimum_post_api_key_transition_success_ratio" candidateApiKeyRetiredAfterRollback \
    "Both restored-key proxies and both backends must serve baseline-key traffic" "$targets"

jq -n \
    --arg baselineSecret "$baseline_api_key_secret" \
    --arg overlapSecret "$overlap_api_key_secret" \
    --arg candidateSecret "$candidate_api_key_secret" \
    --arg overlapToken "$api_key_overlap_token" \
    --arg commitToken "$api_key_commit_token" \
    --arg rollbackOverlapToken "$api_key_rollback_overlap_token" \
    --arg rollbackCommitToken "$api_key_rollback_commit_token" \
    --argjson initialPodUids "$certificate_rollback_proxy_uids_json" \
    --argjson overlapPodUids "$api_key_overlap_uids_json" \
    --argjson commitPodUids "$api_key_commit_uids_json" \
    --argjson rollbackOverlapPodUids "$api_key_rollback_overlap_uids_json" \
    --argjson rollbackCommitPodUids "$api_key_rollback_commit_uids_json" \
    --argjson overlapSeconds "$api_key_overlap_elapsed_seconds" \
    --argjson overlapSamples "$api_key_overlap_sample_count" \
    --argjson overlapMinimumReadyPods "$api_key_overlap_minimum_ready_pods" \
    --argjson overlapMinimumReadyEndpoints "$api_key_overlap_minimum_ready_endpoints" \
    --argjson commitSeconds "$api_key_commit_elapsed_seconds" \
    --argjson commitSamples "$api_key_commit_sample_count" \
    --argjson commitMinimumReadyPods "$api_key_commit_minimum_ready_pods" \
    --argjson commitMinimumReadyEndpoints "$api_key_commit_minimum_ready_endpoints" \
    --argjson rollbackOverlapSeconds "$api_key_rollback_overlap_elapsed_seconds" \
    --argjson rollbackOverlapSamples "$api_key_rollback_overlap_sample_count" \
    --argjson rollbackOverlapMinimumReadyPods "$api_key_rollback_overlap_minimum_ready_pods" \
    --argjson rollbackOverlapMinimumReadyEndpoints "$api_key_rollback_overlap_minimum_ready_endpoints" \
    --argjson rollbackCommitSeconds "$api_key_rollback_commit_elapsed_seconds" \
    --argjson rollbackCommitSamples "$api_key_rollback_commit_sample_count" \
    --argjson rollbackCommitMinimumReadyPods "$api_key_rollback_commit_minimum_ready_pods" \
    --argjson rollbackCommitMinimumReadyEndpoints "$api_key_rollback_commit_minimum_ready_endpoints" \
    --slurpfile overlapDistribution "$output_dir/post-api-key-overlap-distribution-delta.json" \
    --slurpfile commitDistribution "$output_dir/post-api-key-commit-distribution-delta.json" \
    --slurpfile rollbackOverlapDistribution \
        "$output_dir/post-api-key-rollback-overlap-distribution-delta.json" \
    --slurpfile rollbackCommitDistribution \
        "$output_dir/post-api-key-rollback-commit-distribution-delta.json" '
      {verificationModel: "required primary plus at most one operator-bounded rotation key",
       dynamicSecretReload: false,
       secrets: {immutable: true, baseline: $baselineSecret, overlap: $overlapSecret,
         candidate: $candidateSecret},
       assertions: {candidateRejectedBeforeOverlap: true, bothAcceptedDuringOverlap: true,
         baselineRejectedAfterCommit: true, bothAcceptedDuringRollbackOverlap: true,
         candidateRejectedAfterRollbackCommit: true, secretValuesAbsentFromEvidence: true},
       overlap: {triggerAnnotation: $overlapToken, priorPodUids: $initialPodUids,
         podUids: $overlapPodUids, retainedPriorPodUids: 0, runtimeImageUnchanged: true,
         transitionSeconds: $overlapSeconds, continuitySamples: $overlapSamples,
         minimumReadyProxyPods: $overlapMinimumReadyPods,
         minimumReadyServiceEndpoints: $overlapMinimumReadyEndpoints,
         traffic: $overlapDistribution[0]},
       commit: {triggerAnnotation: $commitToken, priorPodUids: $overlapPodUids,
         podUids: $commitPodUids, retainedPriorPodUids: 0, runtimeImageUnchanged: true,
         transitionSeconds: $commitSeconds, continuitySamples: $commitSamples,
         minimumReadyProxyPods: $commitMinimumReadyPods,
         minimumReadyServiceEndpoints: $commitMinimumReadyEndpoints,
         traffic: $commitDistribution[0]},
       rollbackOverlap: {triggerAnnotation: $rollbackOverlapToken, priorPodUids: $commitPodUids,
         podUids: $rollbackOverlapPodUids, retainedPriorPodUids: 0, runtimeImageUnchanged: true,
         transitionSeconds: $rollbackOverlapSeconds, continuitySamples: $rollbackOverlapSamples,
         minimumReadyProxyPods: $rollbackOverlapMinimumReadyPods,
         minimumReadyServiceEndpoints: $rollbackOverlapMinimumReadyEndpoints,
         traffic: $rollbackOverlapDistribution[0]},
       rollbackCommit: {triggerAnnotation: $rollbackCommitToken,
         priorPodUids: $rollbackOverlapPodUids, podUids: $rollbackCommitPodUids,
         retainedPriorPodUids: 0, runtimeImageUnchanged: true,
         transitionSeconds: $rollbackCommitSeconds, continuitySamples: $rollbackCommitSamples,
         minimumReadyProxyPods: $rollbackCommitMinimumReadyPods,
         minimumReadyServiceEndpoints: $rollbackCommitMinimumReadyEndpoints,
         traffic: $rollbackCommitDistribution[0]}}' \
    > "$output_dir/api-key-rotation.json"

failed_node="$(jq -r '.[0].spec.nodeName' <<< "$api_key_rollback_commit_pods_json")"
[[ "$failed_node" == "${cluster_name}-worker" || "$failed_node" == "${cluster_name}-worker2" ]] || {
    echo "Refusing to drain unexpected node $failed_node" >&2; exit 1;
}
transition_seconds="$(jq -r '.workload.transitionSeconds' "$profile")"
vegeta attack -duration="${transition_seconds}s" -rate="${rate}/s" -timeout=5s -keepalive=false -http2=false \
    -root-certs="$tls_trust_bundle" -targets="$targets" > "$work_dir/transition.bin" &
attack_pid=$!
sleep 3
kubectl cordon "$failed_node"
kubectl drain "$failed_node" --ignore-daemonsets --delete-emptydir-data --timeout=120s \
    --pod-selector='loadbalancerpro.io/backend=backend-a'
wait_for_backend_endpoint_count backend-a 1 30
kubectl drain "$failed_node" --ignore-daemonsets --delete-emptydir-data --timeout=120s \
    --pod-selector='loadbalancerpro.io/backend=backend-b'
wait_for_backend_endpoint_count backend-b 1 30
kubectl drain "$failed_node" --ignore-daemonsets --delete-emptydir-data --timeout=120s \
    --pod-selector='app.kubernetes.io/name=loadbalancerpro'
wait_for_count 'ready Service endpoints during planned drain' 1 ready_endpoint_count 30
kubectl drain "$failed_node" --ignore-daemonsets --delete-emptydir-data --timeout=120s
docker stop "$failed_node" >/dev/null
stopped_node="$failed_node"
wait "$attack_pid"
attack_pid=""
report_attack transition "$(jq -r '.objectives.minimumTransitionSuccessRatio' "$profile")"
wait_for_count 'ready proxy replicas while one worker is stopped' 1 ready_proxy_count 90
wait_for_count 'ready Service endpoints while one worker is stopped' 1 ready_endpoint_count 90
capture_state degraded
run_attack degraded "$(jq -r '.workload.degradedSeconds' "$profile")" \
    "$(jq -r '.objectives.minimumDegradedSuccessRatio' "$profile")"

recovery_started_epoch="$(date +%s)"
docker start "$stopped_node" >/dev/null
stopped_node=""
maximum_recovery_seconds="$(jq -r '.objectives.maximumRecoverySeconds' "$profile")"
kubectl wait --for=condition=Ready node/"$failed_node" --timeout="${maximum_recovery_seconds}s"
kubectl uncordon "$failed_node"
for deployment in backend-a backend-b loadbalancerpro; do
    kubectl rollout status deployment/"$deployment" --namespace "$namespace" --timeout="${maximum_recovery_seconds}s"
done
wait_for_count 'recovered proxy replicas' 2 ready_proxy_count "$maximum_recovery_seconds"
wait_for_count 'recovered Service endpoints' 2 ready_endpoint_count "$maximum_recovery_seconds"
recovery_seconds=$(( $(date +%s) - recovery_started_epoch ))
(( recovery_seconds <= maximum_recovery_seconds )) || { echo "Worker recovery exceeded the objective" >&2; exit 1; }
capture_state recovered
collect_distribution recovered-before false
run_attack recovered "$(jq -r '.workload.recoveredSeconds' "$profile")" \
    "$(jq -r '.objectives.minimumRecoveredSuccessRatio' "$profile")"
collect_distribution recovered
jq -n --slurpfile before "$output_dir/recovered-before-distribution.json" \
    --slurpfile after "$output_dir/recovered-distribution.json" '
      ($before[0]) as $before | ($after[0]) as $after |
      {phase: "planned-recovered", bothRecoveredProxyReplicasServed: true,
       backendARequestDelta: ($after.backendARequests - $before.backendARequests),
       backendBRequestDelta: ($after.backendBRequests - $before.backendBRequests),
       pods: [$after.pods[] as $current
         | ($before.pods[] | select(.pod == $current.pod)) as $prior
         | {pod: $current.pod, requestDelta: ($current.requests - $prior.requests)}]}
    ' > "$output_dir/recovered-distribution-delta.json"
jq -e '(.pods | length) == 2
    and all(.pods[]; .requestDelta > 0)
    and .backendARequestDelta > 0
    and .backendBRequestDelta > 0' "$output_dir/recovered-distribution-delta.json" >/dev/null || {
    echo "Both recovered proxies and both backends must serve traffic after planned loss" >&2; exit 1;
}

abrupt_ready_proxy_pods_json="$(kubectl get pod --namespace "$namespace" \
    -l app.kubernetes.io/name=loadbalancerpro -o json | jq '[.items[]
      | select(.metadata.deletionTimestamp == null)
      | select(.status.phase == "Running")
      | select(any(.status.conditions[]?; .type == "Ready" and .status == "True"))]')"
[[ "$(jq 'length' <<< "$abrupt_ready_proxy_pods_json")" == 2 ]] || {
    echo "Expected two ready proxies before abrupt worker loss" >&2; exit 1;
}
abrupt_node="$failed_node"
[[ "$abrupt_node" == "${cluster_name}-worker" || "$abrupt_node" == "${cluster_name}-worker2" ]] || {
    echo "Refusing to stop unexpected abrupt-loss node $abrupt_node" >&2; exit 1;
}
abrupt_failed_proxy_uid="$(jq -r --arg node "$abrupt_node" \
    '.[] | select(.spec.nodeName == $node) | .metadata.uid' <<< "$abrupt_ready_proxy_pods_json")"
[[ "$abrupt_failed_proxy_uid" =~ ^[0-9a-f-]{36}$ ]] || {
    echo "Unable to bind the proxy pod identity on the abrupt-loss worker" >&2; exit 1;
}
abrupt_node_pods_json="$(kubectl get pod --namespace "$namespace" -o json \
    | jq --arg node "$abrupt_node" '[.items[]
      | select(.metadata.deletionTimestamp == null)
      | select(.spec.nodeName == $node)
      | select(.metadata.labels["app.kubernetes.io/name"] == "loadbalancerpro"
          or .metadata.labels["app.kubernetes.io/name"] == "fixture-backend")]')"
[[ "$(jq 'length' <<< "$abrupt_node_pods_json")" == 3 ]] || {
    echo "Expected exactly one proxy and two backend pods on the abrupt-loss worker" >&2; exit 1;
}
abrupt_forced_pod_names_json="$(jq '[.[].metadata.name] | sort' <<< "$abrupt_node_pods_json")"
mapfile -t abrupt_node_pods < <(jq -r '.[].metadata.name' <<< "$abrupt_node_pods_json" | sort)

abrupt_transition_seconds="$(jq -r '.workload.abruptTransitionSeconds' "$profile")"
maximum_abrupt_endpoint_withdrawal_seconds="$(jq -r \
    '.objectives.maximumAbruptEndpointWithdrawalSeconds' "$profile")"
vegeta attack -duration="${abrupt_transition_seconds}s" -rate="${rate}/s" -timeout=5s \
    -keepalive=false -http2=false -root-certs="$tls_trust_bundle" -targets="$targets" \
    > "$work_dir/abrupt-transition.bin" &
attack_pid=$!
sleep 3
abrupt_failure_started_epoch="$(date +%s)"
docker kill "$abrupt_node" >/dev/null
stopped_node="$abrupt_node"
[[ "$(docker inspect --format '{{.State.Running}}' "$abrupt_node")" == false ]] || {
    echo "Abrupt-loss worker container is still running" >&2; exit 1;
}
kubectl taint node "$abrupt_node" \
    node.kubernetes.io/out-of-service=qualification-abrupt-worker-loss:NoExecute --overwrite
kubectl delete pod --namespace "$namespace" "${abrupt_node_pods[@]}" \
    --ignore-not-found --force --grace-period=0 --wait=false
wait_for_count 'abrupt-loss source pods remaining in the API' 0 abrupt_source_pod_count \
    "$maximum_abrupt_endpoint_withdrawal_seconds"
wait_for_count 'ready proxy replicas after abrupt worker loss' 1 ready_proxy_count \
    "$maximum_abrupt_endpoint_withdrawal_seconds"
wait_for_count 'ready Service endpoints after abrupt worker loss' 1 ready_endpoint_count \
    "$maximum_abrupt_endpoint_withdrawal_seconds"
abrupt_endpoint_withdrawal_seconds=$(( $(date +%s) - abrupt_failure_started_epoch ))
(( abrupt_endpoint_withdrawal_seconds <= maximum_abrupt_endpoint_withdrawal_seconds )) || {
    echo "Abrupt worker-loss endpoint withdrawal exceeded the objective" >&2; exit 1;
}
if ! wait "$attack_pid"; then
    attack_pid=""
    echo "Abrupt worker-loss transition traffic attack failed" >&2
    exit 1
fi
attack_pid=""
report_attack abrupt-transition \
    "$(jq -r '.objectives.minimumAbruptTransitionSuccessRatio' "$profile")" \
    "$(jq -r '.objectives.maximumAbruptTransitionP99Millis' "$profile")"
capture_state abrupt-degraded
run_attack abrupt-degraded "$(jq -r '.workload.abruptDegradedSeconds' "$profile")" \
    "$(jq -r '.objectives.minimumAbruptDegradedSuccessRatio' "$profile")" "$targets" \
    "$(jq -r '.objectives.maximumAbruptDegradedP99Millis' "$profile")"

abrupt_recovery_started_epoch="$(date +%s)"
docker start "$stopped_node" >/dev/null
stopped_node=""
maximum_abrupt_recovery_seconds="$(jq -r '.objectives.maximumAbruptRecoverySeconds' "$profile")"
kubectl wait --for=condition=Ready node/"$abrupt_node" \
    --timeout="${maximum_abrupt_recovery_seconds}s"
kubectl taint node "$abrupt_node" node.kubernetes.io/out-of-service:NoExecute-
for deployment in backend-a backend-b loadbalancerpro; do
    kubectl rollout status deployment/"$deployment" --namespace "$namespace" \
        --timeout="${maximum_abrupt_recovery_seconds}s"
done
wait_for_count 'abrupt-loss recovered proxy replicas' 2 ready_proxy_count \
    "$maximum_abrupt_recovery_seconds"
wait_for_count 'abrupt-loss recovered Service endpoints' 2 ready_endpoint_count \
    "$maximum_abrupt_recovery_seconds"
abrupt_recovery_seconds=$(( $(date +%s) - abrupt_recovery_started_epoch ))
(( abrupt_recovery_seconds <= maximum_abrupt_recovery_seconds )) || {
    echo "Abrupt worker-loss recovery exceeded the objective" >&2; exit 1;
}
abrupt_recovered_proxy_pods_json="$(kubectl get pod --namespace "$namespace" \
    -l app.kubernetes.io/name=loadbalancerpro -o json | jq '[.items[]
      | select(.metadata.deletionTimestamp == null)
      | select(.status.phase == "Running")
      | select(any(.status.conditions[]?; .type == "Ready" and .status == "True"))]')"
[[ "$(jq '[.[].spec.nodeName] | unique | length' <<< "$abrupt_recovered_proxy_pods_json")" == 2 ]] || {
    echo "Abrupt-loss recovery did not restore distinct workers" >&2; exit 1;
}
abrupt_failed_uid_overlap="$(jq -n --arg prior "$abrupt_failed_proxy_uid" \
    --argjson recovered "$abrupt_recovered_proxy_pods_json" \
    '[ $recovered[].metadata.uid | select(. == $prior) ] | length')"
[[ "$abrupt_failed_uid_overlap" == 0 ]] || {
    echo "Abrupt-loss recovery retained the failed worker pod UID" >&2; exit 1;
}
capture_state abrupt-recovered
collect_distribution abrupt-recovered-before false
run_attack abrupt-recovered "$(jq -r '.workload.abruptRecoveredSeconds' "$profile")" \
    "$(jq -r '.objectives.minimumAbruptRecoveredSuccessRatio' "$profile")"
collect_distribution abrupt-recovered
jq -n --slurpfile before "$output_dir/abrupt-recovered-before-distribution.json" \
    --slurpfile after "$output_dir/abrupt-recovered-distribution.json" '
      ($before[0]) as $before | ($after[0]) as $after |
      {phase: "abrupt-recovered", bothRecoveredProxyReplicasServed: true,
       backendARequestDelta: ($after.backendARequests - $before.backendARequests),
       backendBRequestDelta: ($after.backendBRequests - $before.backendBRequests),
       pods: [$after.pods[] as $current
         | ($before.pods[] | select(.pod == $current.pod)) as $prior
         | {pod: $current.pod, requestDelta: ($current.requests - $prior.requests)}]}
    ' > "$output_dir/abrupt-recovered-distribution-delta.json"
jq -e '(.pods | length) == 2
    and all(.pods[]; .requestDelta > 0)
    and .backendARequestDelta > 0
    and .backendBRequestDelta > 0' "$output_dir/abrupt-recovered-distribution-delta.json" >/dev/null || {
    echo "Both recovered proxies and both backends must serve traffic after abrupt loss" >&2; exit 1;
}

kubectl version -o json > "$output_dir/kubernetes-version.json"
kind version > "$output_dir/kind-version.txt"
docker image inspect "$proxy_image_id" > "$output_dir/proxy-baseline-image.json"
docker image inspect "$candidate_image_id" > "$output_dir/proxy-candidate-image.json"
sha256sum "$profile" "$cluster_config" "$workload_manifest" "$candidate_dockerfile" \
    > "$output_dir/input-sha256.txt"
baseline_distribution_json="$(<"$output_dir/baseline-distribution.json")"
post_rollout_distribution_delta_json="$(<"$output_dir/post-rollout-distribution-delta.json")"
post_rollback_distribution_delta_json="$(<"$output_dir/post-rollback-distribution-delta.json")"
post_certificate_rotation_distribution_delta_json="$(<"$output_dir/post-certificate-rotation-distribution-delta.json")"
post_certificate_rollback_distribution_delta_json="$(<"$output_dir/post-certificate-rollback-distribution-delta.json")"
api_key_rotation_json="$(<"$output_dir/api-key-rotation.json")"
recovered_distribution_delta_json="$(<"$output_dir/recovered-distribution-delta.json")"
abrupt_recovered_distribution_delta_json="$(<"$output_dir/abrupt-recovered-distribution-delta.json")"
jq -n \
    --arg profileId "$profile_id" \
    --arg sourceRevision "$source_revision" \
    --arg proxyImageId "$proxy_image_id" \
    --arg candidateImageId "$candidate_image_id" \
    --arg fixtureImageId "$fixture_image_id" \
    --arg candidateReleaseId "$candidate_release_id" \
    --arg rolloutToken "$rollout_token" \
    --arg rollbackToken "$rollback_token" \
    --arg baselineTlsSecret "$baseline_tls_secret" \
    --arg candidateTlsSecret "$candidate_tls_secret" \
    --arg baselineCertificateFingerprint "$baseline_certificate_fingerprint" \
    --arg candidateCertificateFingerprint "$candidate_certificate_fingerprint" \
    --arg certificateRotationToken "$certificate_rotation_token" \
    --arg certificateRollbackToken "$certificate_rollback_token" \
    --arg drainedWorker "$failed_node" \
    --arg abruptWorker "$abrupt_node" \
    --arg abruptFailedProxyUid "$abrupt_failed_proxy_uid" \
    --argjson abruptForcedPodNames "$abrupt_forced_pod_names_json" \
    --argjson priorPodUids "$initial_proxy_uids_json" \
    --argjson candidatePodUids "$replacement_proxy_uids_json" \
    --argjson restoredPodUids "$rollback_proxy_uids_json" \
    --argjson baselineRuntimeImageIds "$initial_proxy_runtime_image_ids_json" \
    --argjson candidateRuntimeImageIds "$replacement_proxy_runtime_image_ids_json" \
    --argjson restoredRuntimeImageIds "$rollback_proxy_runtime_image_ids_json" \
    --argjson certificateRotationPodUids "$certificate_rotation_proxy_uids_json" \
    --argjson certificateRollbackPodUids "$certificate_rollback_proxy_uids_json" \
    --argjson certificateRotationRuntimeImageIds "$certificate_rotation_runtime_image_ids_json" \
    --argjson certificateRollbackRuntimeImageIds "$certificate_rollback_runtime_image_ids_json" \
    --argjson rolloutSeconds "$rollout_elapsed_seconds" \
    --argjson rolloutSamples "$rollout_sample_count" \
    --argjson rolloutMinimumReadyPods "$rollout_min_ready_pods" \
    --argjson rolloutMinimumReadyEndpoints "$rollout_min_ready_endpoints" \
    --argjson rollbackSeconds "$rollback_elapsed_seconds" \
    --argjson rollbackSamples "$rollback_sample_count" \
    --argjson rollbackMinimumReadyPods "$rollback_min_ready_pods" \
    --argjson rollbackMinimumReadyEndpoints "$rollback_min_ready_endpoints" \
    --argjson certificateRotationSeconds "$certificate_rotation_elapsed_seconds" \
    --argjson certificateRotationSamples "$certificate_rotation_sample_count" \
    --argjson certificateRotationMinimumReadyPods "$certificate_rotation_min_ready_pods" \
    --argjson certificateRotationMinimumReadyEndpoints "$certificate_rotation_min_ready_endpoints" \
    --argjson certificateRollbackSeconds "$certificate_rollback_elapsed_seconds" \
    --argjson certificateRollbackSamples "$certificate_rollback_sample_count" \
    --argjson certificateRollbackMinimumReadyPods "$certificate_rollback_min_ready_pods" \
    --argjson certificateRollbackMinimumReadyEndpoints "$certificate_rollback_min_ready_endpoints" \
    --argjson recoverySeconds "$recovery_seconds" \
    --argjson abruptEndpointWithdrawalSeconds "$abrupt_endpoint_withdrawal_seconds" \
    --argjson abruptRecoverySeconds "$abrupt_recovery_seconds" \
    --argjson baselineDistribution "$baseline_distribution_json" \
    --argjson postRolloutDistribution "$post_rollout_distribution_delta_json" \
    --argjson postRollbackDistribution "$post_rollback_distribution_delta_json" \
    --argjson postCertificateRotationDistribution "$post_certificate_rotation_distribution_delta_json" \
    --argjson postCertificateRollbackDistribution "$post_certificate_rollback_distribution_delta_json" \
    --argjson apiKeyRotation "$api_key_rotation_json" \
    --argjson recoveredDistribution "$recovered_distribution_delta_json" \
    --argjson abruptRecoveredDistribution "$abrupt_recovered_distribution_delta_json" \
    '{schemaVersion: 6, result: "pass", evidenceBoundary: "disposable loopback kind metadata-only content-distinct image rollout and baseline rollback, versioned immutable inbound-server TLS Secret rotation and identity rollback, bounded two-key API credential overlap/commit/rollback, planned worker loss, and operator-remediated abrupt worker-container loss; not dynamic Secret reload, not an ingress-controller, and not automatic infrastructure-failure detection, application-layer release compatibility, registry/source binding, external certificate-authority, client trust-distribution, external secret-manager, or deployment-capacity proof",
      profileId: $profileId, repositoryRevision: $sourceRevision,
      images: {identityType: "local Docker content-addressed image ID",
        baseline: {contentId: $proxyImageId},
        candidate: {releaseId: $candidateReleaseId, contentId: $candidateImageId},
        fixtureContentId: $fixtureImageId, applicationLayersIdentical: true},
      topology: {workers: 2, zones: 2, initialProxyReplicas: 2, postRolloutProxyReplicas: 2,
        postRollbackProxyReplicas: 2,
         postCertificateRotationProxyReplicas: 2, postCertificateRollbackProxyReplicas: 2,
         postApiKeyOverlapProxyReplicas: 2, postApiKeyCommitProxyReplicas: 2,
         postApiKeyRollbackOverlapProxyReplicas: 2, postApiKeyRollbackCommitProxyReplicas: 2,
        degradedProxyReplicas: 1, recoveredProxyReplicas: 2,
        abruptDegradedProxyReplicas: 1, abruptRecoveredProxyReplicas: 2},
      traffic: {bothProxyReplicasServed: true, baseline: $baselineDistribution,
        rollout: "pass", postRollout: $postRolloutDistribution,
        rollback: "pass", postRollback: $postRollbackDistribution,
        certificateRotation: "pass", postCertificateRotation: $postCertificateRotationDistribution,
         certificateRollback: "pass", postCertificateRollback: $postCertificateRollbackDistribution,
         apiKeyOverlap: "pass", apiKeyCommit: "pass",
         apiKeyRollbackOverlap: "pass", apiKeyRollbackCommit: "pass",
        drainTransition: "pass", degraded: "pass", recovered: $recoveredDistribution,
        abruptTransition: "pass", abruptDegraded: "pass",
        abruptRecovered: $abruptRecoveredDistribution},
      rolloutExercise: {triggerAnnotation: $rolloutToken, contentDistinctRuntimeImageId: true,
        fromRuntimeImageIds: $baselineRuntimeImageIds,
        toRuntimeImageIds: $candidateRuntimeImageIds,
        priorPodUids: $priorPodUids, candidatePodUids: $candidatePodUids,
        retainedPriorPodUids: 0,
        rolloutSeconds: $rolloutSeconds, continuitySamples: $rolloutSamples,
        minimumReadyProxyPods: $rolloutMinimumReadyPods,
        minimumReadyServiceEndpoints: $rolloutMinimumReadyEndpoints},
      rollbackExercise: {triggerAnnotation: $rollbackToken, restoredInitialRuntimeImageId: true,
        fromRuntimeImageIds: $candidateRuntimeImageIds,
        toRuntimeImageIds: $restoredRuntimeImageIds,
        candidatePodUids: $candidatePodUids, restoredPodUids: $restoredPodUids,
        retainedCandidatePodUids: 0, retainedInitialPodUids: 0,
        rollbackSeconds: $rollbackSeconds, continuitySamples: $rollbackSamples,
        minimumReadyProxyPods: $rollbackMinimumReadyPods,
        minimumReadyServiceEndpoints: $rollbackMinimumReadyEndpoints},
      tlsRotationExercise: {
        identityType: "generated one-day leaf fingerprint bound to an independently generated local CA",
        secrets: {immutable: true, baseline: $baselineTlsSecret, candidate: $candidateTlsSecret},
        baseline: {leafSha256Fingerprint: $baselineCertificateFingerprint,
          authority: "CN=LoadBalancerPro Kubernetes Qualification CA A"},
        candidate: {leafSha256Fingerprint: $candidateCertificateFingerprint,
          authority: "CN=LoadBalancerPro Kubernetes Qualification CA B"},
        trustRollover: {continuousTrafficBundleContainsBothAuthorities: true,
          candidateOnlyRejectedBeforeRotation: true,
          baselineOnlyRejectedAfterRotation: true,
          candidateOnlyRejectedAfterRollback: true},
        rotation: {triggerAnnotation: $certificateRotationToken,
          priorPodUids: $restoredPodUids, rotatedPodUids: $certificateRotationPodUids,
          retainedPriorPodUids: 0, runtimeImageIds: $certificateRotationRuntimeImageIds,
          runtimeImageUnchanged: true, servedCandidateFingerprint: true,
          rotationSeconds: $certificateRotationSeconds, continuitySamples: $certificateRotationSamples,
          minimumReadyProxyPods: $certificateRotationMinimumReadyPods,
          minimumReadyServiceEndpoints: $certificateRotationMinimumReadyEndpoints},
        rollback: {triggerAnnotation: $certificateRollbackToken,
          rotatedPodUids: $certificateRotationPodUids, restoredPodUids: $certificateRollbackPodUids,
          retainedRotatedPodUids: 0, retainedPriorBaselinePodUids: 0,
          runtimeImageIds: $certificateRollbackRuntimeImageIds, runtimeImageUnchanged: true,
          restoredBaselineFingerprint: true,
          rollbackSeconds: $certificateRollbackSeconds, continuitySamples: $certificateRollbackSamples,
          minimumReadyProxyPods: $certificateRollbackMinimumReadyPods,
          minimumReadyServiceEndpoints: $certificateRollbackMinimumReadyEndpoints}},
      apiKeyRotationExercise: $apiKeyRotation,
      workerExercise: {planned: {drainedAndStopped: $drainedWorker, recoverySeconds: $recoverySeconds},
        abrupt: {stoppedWithoutDrain: $abruptWorker,
          remediation: "verified-down out-of-service:NoExecute taint plus forced API deletion",
          apiForcedPodNames: $abruptForcedPodNames,
          failedProxyPodUid: $abruptFailedProxyUid, retainedFailedProxyPodUids: 0,
          endpointWithdrawalSeconds: $abruptEndpointWithdrawalSeconds,
          recoverySeconds: $abruptRecoverySeconds}}}' \
    > "$output_dir/summary.json"

for secret_file in "$api_key_file" "$candidate_api_key_file"; do
    if grep -R -F -q -- "$(<"$secret_file")" "$output_dir"; then
        echo "Kubernetes API-key value leaked into evidence" >&2
        exit 1
    fi
done

printf 'Kubernetes two-zone image rollout/rollback, immutable TLS identity rotation/rollback, bounded API-key rotation/rollback, planned-loss, and abrupt-loss proof passed; evidence: %s\n' "$output_dir"
