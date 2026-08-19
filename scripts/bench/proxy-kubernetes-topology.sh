#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
cluster_config="$repo_root/deploy/kubernetes/kind-cluster.yaml"
workload_manifest="$repo_root/deploy/kubernetes/qualification.yaml"
example_profile="$script_dir/kubernetes-topology-profile.example.json"

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
for required_file in "$cluster_config" "$workload_manifest" "$profile"; do
    [[ -f "$required_file" ]] || { echo "Missing required file: $required_file" >&2; exit 2; }
done

jq -e '
  .schemaVersion == 3
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
  and (.workload.transitionSeconds | type == "number" and . >= 15 and . <= 120 and floor == .)
  and (.workload.degradedSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.recoveredSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.abruptTransitionSeconds | type == "number" and . >= 15 and . <= 120 and floor == .)
  and (.workload.abruptDegradedSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.abruptRecoveredSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.objectives.minimumBaselineSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumRolloutSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumPostRolloutSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumTransitionSuccessRatio | type == "number" and . >= 0.90 and . <= 1)
  and (.objectives.minimumDegradedSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumRecoveredSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumAbruptTransitionSuccessRatio | type == "number" and . >= 0.90 and . <= 1)
  and (.objectives.minimumAbruptDegradedSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumAbruptRecoveredSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.maximumP99Millis | type == "number" and . >= 100 and . <= 5000 and floor == .)
  and (.objectives.maximumAbruptTransitionP99Millis | type == "number" and . >= 1000 and . <= 6000 and floor == .)
  and (.objectives.maximumRolloutSeconds | type == "number" and . >= 20 and . <= 120 and floor == .)
  and (.objectives.maximumRecoverySeconds | type == "number" and . >= 30 and . <= 300 and floor == .)
  and (.objectives.maximumAbruptEndpointWithdrawalSeconds | type == "number" and . >= 5 and . <= 30 and floor == .)
  and (.objectives.maximumAbruptRecoverySeconds | type == "number" and . >= 30 and . <= 300 and floor == .)
  and .workload.rolloutSeconds >= (.objectives.maximumRolloutSeconds + 5)
  and .workload.abruptTransitionSeconds >= (.objectives.maximumAbruptEndpointWithdrawalSeconds + 5)
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
    'minDomains: 2' \
    'runAsUser: 10001' \
    'readOnlyRootFilesystem: true' \
    'nodePort: 30443' \
    'kind: PodDisruptionBudget' \
    'kind: NetworkPolicy'; do
    grep -Fq "$invariant" "$workload_manifest" || { echo "Kubernetes workload is missing: $invariant" >&2; exit 2; }
done
if grep -Eq '(BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|api-key:[[:space:]]+[^[:space:]]+)' "$workload_manifest"; then
    echo "Kubernetes workload must not contain private keys or API-key values" >&2
    exit 2
fi

if [[ "$mode" == "validate" ]]; then
    printf 'Validated disposable two-worker/two-zone Kubernetes topology contract %s.\n' "$(jq -r '.profileId' "$profile")"
    printf 'Validated proof cases: service-distribution per-replica-metrics rolling-replacement endpoint-continuity pod-identity-turnover post-rollout-distribution planned-worker-drain stopped-worker degraded-service worker-recovery abrupt-worker-stop out-of-service-remediation abrupt-endpoint-withdrawal abrupt-recovery\n'
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
api_key_file="$work_dir/loadbalancerpro-api-key"
tls_dir="$work_dir/tls"
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
fixture_image=loadbalancerpro:kubernetes-fixture
proxy_source="${LBP_KUBERNETES_PROXY_SOURCE_IMAGE:-}"
fixture_source="${LBP_KUBERNETES_FIXTURE_SOURCE_IMAGE:-}"
if [[ -n "$proxy_source" ]]; then
    docker image inspect "$proxy_source" >/dev/null
    docker tag "$proxy_source" "$proxy_image"
else
    docker build --tag "$proxy_image" "$repo_root"
fi
if [[ -n "$fixture_source" ]]; then
    docker image inspect "$fixture_source" >/dev/null
    docker tag "$fixture_source" "$fixture_image"
else
    docker build --file "$repo_root/deploy/fixture/Dockerfile" --tag "$fixture_image" "$repo_root"
fi
proxy_image_id="$(docker image inspect --format '{{.Id}}' "$proxy_image")"
fixture_image_id="$(docker image inspect --format '{{.Id}}' "$fixture_image")"
for image_id in "$proxy_image_id" "$fixture_image_id"; do
    [[ "$image_id" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "Qualification image has no exact local content ID" >&2; exit 1; }
done
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

kind load docker-image "$proxy_image" "$fixture_image" --name "$cluster_name"
mkdir -p "$tls_dir"
openssl rand -hex 24 > "$api_key_file"
ca_key="$work_dir/ca-key.pem"
server_csr="$work_dir/server.csr"
server_extensions="$work_dir/server-extensions.cnf"
MSYS2_ARG_CONV_EXCL='/CN=LoadBalancerPro Kubernetes Qualification CA' \
openssl req -x509 -newkey rsa:2048 -sha256 -days 1 -nodes \
    -subj '/CN=LoadBalancerPro Kubernetes Qualification CA' \
    -addext 'basicConstraints=critical,CA:TRUE' -addext 'keyUsage=critical,keyCertSign,cRLSign' \
    -keyout "$ca_key" -out "$tls_dir/ca.pem" >/dev/null 2>&1
MSYS2_ARG_CONV_EXCL='/CN=lbp-kubernetes.local' \
openssl req -newkey rsa:2048 -sha256 -nodes -subj '/CN=lbp-kubernetes.local' \
    -keyout "$tls_dir/private-key.pem" -out "$server_csr" >/dev/null 2>&1
printf '%s\n' \
    'subjectAltName=DNS:lbp-kubernetes.local,DNS:loadbalancerpro,DNS:loadbalancerpro.lbp-kubernetes-smoke.svc,IP:127.0.0.1' \
    'basicConstraints=critical,CA:FALSE' \
    'keyUsage=critical,digitalSignature,keyEncipherment' \
    'extendedKeyUsage=serverAuth' > "$server_extensions"
openssl x509 -req -sha256 -days 1 -in "$server_csr" -CA "$tls_dir/ca.pem" -CAkey "$ca_key" \
    -set_serial 1 -extfile "$server_extensions" -out "$tls_dir/certificate.pem" >/dev/null 2>&1
chmod 0600 "$api_key_file" "$tls_dir"/*

kubectl apply --server-side --field-manager=loadbalancerpro-qualification -f "$workload_manifest"
kubectl create secret generic loadbalancerpro-api-key --namespace "$namespace" \
    --from-file=api-key="$api_key_file"
kubectl create secret generic loadbalancerpro-server-tls --namespace "$namespace" \
    --from-file=tls.crt="$tls_dir/certificate.pem" \
    --from-file=tls.key="$tls_dir/private-key.pem" \
    --from-file=ca.crt="$tls_dir/ca.pem"
source_revision="$(git -C "$repo_root" rev-parse HEAD)"
[[ "$source_revision" =~ ^[0-9a-f]{40}$ ]] || { echo "Unable to bind source revision" >&2; exit 1; }
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

sample_rollout_continuity() {
    local output="$output_dir/rollout-continuity.csv"
    printf 'epoch_seconds,ready_proxy_pods,ready_service_endpoints,total_proxy_pods\n' > "$output"
    while [[ ! -f "$rollout_stop_file" ]]; do
        local ready_pods ready_endpoints total_pods
        ready_pods="$(ready_proxy_count)"
        ready_endpoints="$(ready_endpoint_count)"
        total_pods="$(kubectl get pod --namespace "$namespace" \
            -l app.kubernetes.io/name=loadbalancerpro -o json | jq '.items | length')"
        printf '%s,%s,%s,%s\n' "$(date +%s)" "$ready_pods" "$ready_endpoints" "$total_pods" >> "$output"
        if (( ready_pods < 2 || ready_endpoints < 2 )); then
            echo "Rolling replacement dropped below two ready proxy pods or Service endpoints" >&2
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
capture_state initial

api_key="$(<"$api_key_file")"
targets="$work_dir/targets.txt"
printf 'GET https://127.0.0.1:%s/proxy/kubernetes/topology\nX-API-Key: %s\n\n' \
    "$host_port" "$api_key" > "$targets"

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
    vegeta attack -duration="${seconds}s" -rate="${rate}/s" -timeout=5s -keepalive=false -http2=false \
        -root-certs="$tls_dir/ca.pem" -targets="$targets" > "$work_dir/${name}.bin"
    report_attack "$name" "$minimum_success"
}

baseline_seconds="$(jq -r '.workload.baselineSeconds' "$profile")"
run_attack baseline "$baseline_seconds" "$(jq -r '.objectives.minimumBaselineSuccessRatio' "$profile")"
collect_distribution baseline

rollout_duration_seconds="$(jq -r '.workload.rolloutSeconds' "$profile")"
maximum_rollout_seconds="$(jq -r '.objectives.maximumRolloutSeconds' "$profile")"
rollout_token="$(printf '%s\n' "$source_revision|$default_run_id|rolling-replacement" \
    | sha256sum | awk '{print $1}')"
rm -f -- "$rollout_stop_file"
sample_rollout_continuity &
rollout_sampler_pid=$!
vegeta attack -duration="${rollout_duration_seconds}s" -rate="${rate}/s" -timeout=5s \
    -keepalive=false -http2=false -root-certs="$tls_dir/ca.pem" -targets="$targets" \
    > "$work_dir/rollout.bin" &
attack_pid=$!
sleep 3
rollout_started_epoch="$(date +%s)"
kubectl patch deployment loadbalancerpro --namespace "$namespace" --type merge \
    -p "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"loadbalancerpro.io/qualification-rollout\":\"$rollout_token\"}}}}}"
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
[[ "$replacement_proxy_runtime_image_ids_json" == "$initial_proxy_runtime_image_ids_json" ]] || {
    echo "Rolling replacement changed the immutable runtime image ID" >&2; exit 1;
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

failed_node="$(jq -r '.[0].spec.nodeName' <<< "$replacement_ready_proxy_pods_json")"
[[ "$failed_node" == "${cluster_name}-worker" || "$failed_node" == "${cluster_name}-worker2" ]] || {
    echo "Refusing to drain unexpected node $failed_node" >&2; exit 1;
}
transition_seconds="$(jq -r '.workload.transitionSeconds' "$profile")"
vegeta attack -duration="${transition_seconds}s" -rate="${rate}/s" -timeout=5s -keepalive=false -http2=false \
    -root-certs="$tls_dir/ca.pem" -targets="$targets" > "$work_dir/transition.bin" &
attack_pid=$!
sleep 3
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

abrupt_transition_seconds="$(jq -r '.workload.abruptTransitionSeconds' "$profile")"
maximum_abrupt_endpoint_withdrawal_seconds="$(jq -r \
    '.objectives.maximumAbruptEndpointWithdrawalSeconds' "$profile")"
vegeta attack -duration="${abrupt_transition_seconds}s" -rate="${rate}/s" -timeout=5s \
    -keepalive=false -http2=false -root-certs="$tls_dir/ca.pem" -targets="$targets" \
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
    "$(jq -r '.objectives.minimumAbruptDegradedSuccessRatio' "$profile")"

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
sha256sum "$profile" "$cluster_config" "$workload_manifest" > "$output_dir/input-sha256.txt"
baseline_distribution_json="$(<"$output_dir/baseline-distribution.json")"
post_rollout_distribution_delta_json="$(<"$output_dir/post-rollout-distribution-delta.json")"
recovered_distribution_delta_json="$(<"$output_dir/recovered-distribution-delta.json")"
abrupt_recovered_distribution_delta_json="$(<"$output_dir/abrupt-recovered-distribution-delta.json")"
jq -n \
    --arg profileId "$profile_id" \
    --arg sourceRevision "$source_revision" \
    --arg proxyImageId "$proxy_image_id" \
    --arg fixtureImageId "$fixture_image_id" \
    --arg rolloutToken "$rollout_token" \
    --arg drainedWorker "$failed_node" \
    --arg abruptWorker "$abrupt_node" \
    --arg abruptFailedProxyUid "$abrupt_failed_proxy_uid" \
    --argjson abruptForcedPodNames "$abrupt_forced_pod_names_json" \
    --argjson priorPodUids "$initial_proxy_uids_json" \
    --argjson replacementPodUids "$replacement_proxy_uids_json" \
    --argjson runtimeImageIds "$replacement_proxy_runtime_image_ids_json" \
    --argjson rolloutSeconds "$rollout_elapsed_seconds" \
    --argjson rolloutSamples "$rollout_sample_count" \
    --argjson minimumReadyPods "$rollout_min_ready_pods" \
    --argjson minimumReadyEndpoints "$rollout_min_ready_endpoints" \
    --argjson recoverySeconds "$recovery_seconds" \
    --argjson abruptEndpointWithdrawalSeconds "$abrupt_endpoint_withdrawal_seconds" \
    --argjson abruptRecoverySeconds "$abrupt_recovery_seconds" \
    --argjson baselineDistribution "$baseline_distribution_json" \
    --argjson postRolloutDistribution "$post_rollout_distribution_delta_json" \
    --argjson recoveredDistribution "$recovered_distribution_delta_json" \
    --argjson abruptRecoveredDistribution "$abrupt_recovered_distribution_delta_json" \
    '{schemaVersion: 3, result: "pass", evidenceBoundary: "disposable loopback kind same-image replacement, planned worker loss, and operator-remediated abrupt worker-container loss; not automatic infrastructure-failure detection, release compatibility, registry/source binding, deployment-ingress, or deployment-capacity proof",
      profileId: $profileId, repositoryRevision: $sourceRevision,
      images: {proxyContentId: $proxyImageId, fixtureContentId: $fixtureImageId},
      topology: {workers: 2, zones: 2, initialProxyReplicas: 2, postRolloutProxyReplicas: 2,
        degradedProxyReplicas: 1, recoveredProxyReplicas: 2,
        abruptDegradedProxyReplicas: 1, abruptRecoveredProxyReplicas: 2},
      traffic: {bothProxyReplicasServed: true, baseline: $baselineDistribution,
        rollout: "pass", postRollout: $postRolloutDistribution,
        drainTransition: "pass", degraded: "pass", recovered: $recoveredDistribution,
        abruptTransition: "pass", abruptDegraded: "pass",
        abruptRecovered: $abruptRecoveredDistribution},
      rolloutExercise: {triggerAnnotation: $rolloutToken, sameRuntimeImageId: true,
        runtimeImageIds: $runtimeImageIds, priorPodUids: $priorPodUids,
        replacementPodUids: $replacementPodUids, retainedPriorPodUids: 0,
        rolloutSeconds: $rolloutSeconds, continuitySamples: $rolloutSamples,
        minimumReadyProxyPods: $minimumReadyPods,
        minimumReadyServiceEndpoints: $minimumReadyEndpoints},
      workerExercise: {planned: {drainedAndStopped: $drainedWorker, recoverySeconds: $recoverySeconds},
        abrupt: {stoppedWithoutDrain: $abruptWorker,
          remediation: "node.kubernetes.io/out-of-service:NoExecute",
          outOfServiceForcedPodNames: $abruptForcedPodNames,
          failedProxyPodUid: $abruptFailedProxyUid, retainedFailedProxyPodUids: 0,
          endpointWithdrawalSeconds: $abruptEndpointWithdrawalSeconds,
          recoverySeconds: $abruptRecoverySeconds}}}' \
    > "$output_dir/summary.json"

printf 'Kubernetes two-zone live rollout, planned-loss, and abrupt-loss proof passed; evidence: %s\n' "$output_dir"
