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
  .schemaVersion == 1
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
  and (.workload.transitionSeconds | type == "number" and . >= 15 and . <= 120 and floor == .)
  and (.workload.degradedSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.workload.recoveredSeconds | type == "number" and . >= 5 and . <= 60 and floor == .)
  and (.objectives.minimumBaselineSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumTransitionSuccessRatio | type == "number" and . >= 0.90 and . <= 1)
  and (.objectives.minimumDegradedSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.minimumRecoveredSuccessRatio | type == "number" and . >= 0.95 and . <= 1)
  and (.objectives.maximumP99Millis | type == "number" and . >= 100 and . <= 5000 and floor == .)
  and (.objectives.maximumRecoverySeconds | type == "number" and . >= 30 and . <= 300 and floor == .)
' "$profile" >/dev/null || { echo "Kubernetes topology profile does not satisfy the executable contract" >&2; exit 2; }

for invariant in \
    'kindest/node:v1.34.3@sha256:08497ee19eace7b4b5348db5c6a1591d7752b164530a36f855cb0f2bdcbadd48' \
    'listenAddress: 127.0.0.1' \
    'containerPort: 30443' \
    'hostPort: 18460'; do
    grep -Fq "$invariant" "$cluster_config" || { echo "Kind cluster config is missing: $invariant" >&2; exit 2; }
done
for invariant in \
    'pod-security.kubernetes.io/enforce: restricted' \
    'automountServiceAccountToken: false' \
    'maxUnavailable: 0' \
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
    printf 'Validated proof cases: service-distribution per-replica-metrics planned-worker-drain stopped-worker degraded-service worker-recovery\n'
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
stopped_node=""
cluster_created=false
cleanup() {
    local status=$?
    trap - EXIT
    if [[ -n "$attack_pid" ]]; then kill "$attack_pid" >/dev/null 2>&1 || true; wait "$attack_pid" >/dev/null 2>&1 || true; fi
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
    kubectl get endpointslice --namespace "$namespace" -o json > "$output_dir/${prefix}-endpointslices.json"
}

ready_proxy_count() {
    kubectl get pod --namespace "$namespace" -l app.kubernetes.io/name=loadbalancerpro -o json \
        | jq '[.items[] | select(.status.phase == "Running")
            | select(any(.status.conditions[]?; .type == "Ready" and .status == "True"))] | length'
}

ready_endpoint_count() {
    kubectl get endpointslice --namespace "$namespace" \
        -l kubernetes.io/service-name=loadbalancerpro -o json \
        | jq '[.items[].endpoints[] | select(.conditions.ready == true)] | length'
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

proxy_pods_json="$(kubectl get pod --namespace "$namespace" -l app.kubernetes.io/name=loadbalancerpro -o json)"
[[ "$(jq '[.items[].spec.nodeName] | unique | length' <<< "$proxy_pods_json")" == 2 ]] || {
    echo "Proxy replicas were not placed on distinct workers" >&2; exit 1;
}
mapfile -t initial_proxy_pods < <(jq -r '.items[].metadata.name' <<< "$proxy_pods_json" | sort)
for pod in "${initial_proxy_pods[@]}"; do
    [[ "$(kubectl exec --namespace "$namespace" "$pod" -- id -u)" == 10001 ]] || {
        echo "$pod is not running with the enforced non-root UID 10001" >&2; exit 1;
    }
done
proxy_zones="$(jq -r '.items[].spec.nodeName' <<< "$proxy_pods_json" \
    | while read -r node; do kubectl get node "$node" -o jsonpath='{.metadata.labels.topology\.kubernetes\.io/zone}{"\n"}'; done \
    | sort -u | wc -l | tr -d ' ')"
[[ "$proxy_zones" == 2 ]] || { echo "Proxy replicas were not placed in distinct zones" >&2; exit 1; }
[[ "$(ready_endpoint_count)" == 2 ]] || { echo "Proxy Service did not publish two ready endpoints" >&2; exit 1; }
capture_state initial

api_key="$(<"$api_key_file")"
targets="$work_dir/targets.txt"
printf 'GET https://127.0.0.1:%s/proxy/kubernetes/topology\nX-API-Key: %s\n\n' \
    "$host_port" "$api_key" > "$targets"

report_attack() {
    local name="$1" minimum_success="$2"
    local binary="$work_dir/${name}.bin"
    vegeta report -type=json "$binary" > "$output_dir/${name}-client.json"
    vegeta report -type=text "$binary" > "$output_dir/${name}-client.txt"
    local maximum_p99_nanos
    maximum_p99_nanos="$(jq '.objectives.maximumP99Millis * 1000000' "$profile")"
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

mapfile -t proxy_pods < <(kubectl get pod --namespace "$namespace" -l app.kubernetes.io/name=loadbalancerpro \
    -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' | sort)
[[ ${#proxy_pods[@]} -eq 2 ]] || { echo "Expected two proxy replicas for metrics proof" >&2; exit 1; }
backend_a_total=0
backend_b_total=0
for pod in "${proxy_pods[@]}"; do
    metrics_file="$output_dir/baseline-${pod}-metrics.txt"
    kubectl exec --namespace "$namespace" "$pod" -- sh -c '
      curl --fail --silent --show-error --cacert /run/tls/ca.pem \
        --header "X-API-Key: $(cat /run/secrets/loadbalancerpro.api.key)" \
        --resolve "${LBP_TLS_HOSTNAME}:8080:127.0.0.1" \
        "https://${LBP_TLS_HOSTNAME}:8080/actuator/prometheus"
    ' > "$metrics_file"
    pod_total="$(awk '$1 ~ /^lbp_proxy_requests_total(\{|$)/ { total += $2 } END { printf "%.0f", total + 0 }' "$metrics_file")"
    [[ "$pod_total" =~ ^[1-9][0-9]*$ ]] || { echo "$pod did not serve baseline traffic" >&2; exit 1; }
    backend_a_total=$((backend_a_total + $(awk '$1 ~ /^lbp_proxy_requests_total\{/ && $1 ~ /upstream="backend-a"/ { total += $2 } END { printf "%.0f", total + 0 }' "$metrics_file")))
    backend_b_total=$((backend_b_total + $(awk '$1 ~ /^lbp_proxy_requests_total\{/ && $1 ~ /upstream="backend-b"/ { total += $2 } END { printf "%.0f", total + 0 }' "$metrics_file")))
done
(( backend_a_total > 0 && backend_b_total > 0 )) || { echo "Both configured backends must serve baseline traffic" >&2; exit 1; }

failed_node="$(jq -r '.items[0].spec.nodeName' <<< "$proxy_pods_json")"
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
run_attack recovered "$(jq -r '.workload.recoveredSeconds' "$profile")" \
    "$(jq -r '.objectives.minimumRecoveredSuccessRatio' "$profile")"

kubectl version -o json > "$output_dir/kubernetes-version.json"
kind version > "$output_dir/kind-version.txt"
sha256sum "$profile" "$cluster_config" "$workload_manifest" > "$output_dir/input-sha256.txt"
jq -n \
    --arg profileId "$profile_id" \
    --arg sourceRevision "$source_revision" \
    --arg proxyImageId "$proxy_image_id" \
    --arg fixtureImageId "$fixture_image_id" \
    --arg drainedWorker "$failed_node" \
    --argjson recoverySeconds "$recovery_seconds" \
    --argjson backendARequests "$backend_a_total" \
    --argjson backendBRequests "$backend_b_total" \
    '{schemaVersion: 1, result: "pass", evidenceBoundary: "disposable loopback kind mechanics; local image content IDs are not registry/source bindings or deployment-capacity proof",
      profileId: $profileId, repositoryRevision: $sourceRevision,
      images: {proxyContentId: $proxyImageId, fixtureContentId: $fixtureImageId},
      topology: {workers: 2, zones: 2, initialProxyReplicas: 2, degradedProxyReplicas: 1, recoveredProxyReplicas: 2},
      traffic: {bothProxyReplicasServed: true, backendARequests: $backendARequests, backendBRequests: $backendBRequests,
        baseline: "pass", drainTransition: "pass", degraded: "pass", recovered: "pass"},
      workerExercise: {drainedAndStopped: $drainedWorker, recoverySeconds: $recoverySeconds}}' \
    > "$output_dir/summary.json"

printf 'Kubernetes two-zone live topology proof passed; evidence: %s\n' "$output_dir"
