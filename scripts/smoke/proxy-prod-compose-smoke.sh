#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
compose_file="$repo_root/deploy/docker-compose.proxy-prod.yml"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-proxy-prod.XXXXXX")"
project_name="lbp-p25-${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-0}-$$"
api_key="$(openssl rand -hex 24)"
tls_hostname="lbp.local"
proxy_port="${LBP_PROXY_PROD_PORT:-18443}"
api_key_file="$work_dir/loadbalancerpro-api-key"
tls_dir="$work_dir/tls"
trust_dir="$work_dir/trust"
identity_dir="$work_dir/identity"
config_dir="$work_dir/config"
image_archive="$work_dir/loadbalancerpro-image.tar"

cleanup() {
    local status=$?
    trap - EXIT
    docker compose -p "$project_name" -f "$compose_file" down --volumes --remove-orphans >/dev/null 2>&1 || true
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-proxy-prod.*)
            chmod -R u+w -- "$work_dir" 2>/dev/null || true
            rm -rf -- "$work_dir"
            ;;
        *) echo "Refusing to remove unexpected temporary path: $work_dir" >&2 ;;
    esac
    exit "$status"
}
trap cleanup EXIT

mkdir -p "$tls_dir" "$trust_dir" "$identity_dir" "$config_dir"
printf '%s' "$api_key" > "$api_key_file"
chmod 0444 "$api_key_file"
ca_private_key="$work_dir/ca-private-key.pem"
server_csr="$work_dir/server.csr"
server_extensions="$work_dir/server-extensions.cnf"
openssl req -x509 -newkey rsa:2048 -sha256 -days 1 -nodes \
    -subj "/CN=LoadBalancerPro Local Smoke CA" \
    -addext "basicConstraints=critical,CA:TRUE" \
    -addext "keyUsage=critical,keyCertSign,cRLSign" \
    -keyout "$ca_private_key" \
    -out "$tls_dir/ca.pem" >/dev/null 2>&1
openssl req -newkey rsa:2048 -sha256 -nodes \
    -subj "/CN=$tls_hostname" \
    -keyout "$tls_dir/private-key.pem" \
    -out "$server_csr" >/dev/null 2>&1
printf '%s\n' \
    "subjectAltName=DNS:$tls_hostname,IP:127.0.0.1" \
    "basicConstraints=critical,CA:FALSE" \
    "keyUsage=critical,digitalSignature,keyEncipherment" \
    "extendedKeyUsage=serverAuth" > "$server_extensions"
openssl x509 -req -sha256 -days 1 \
    -in "$server_csr" \
    -CA "$tls_dir/ca.pem" \
    -CAkey "$ca_private_key" \
    -set_serial 1 \
    -extfile "$server_extensions" \
    -out "$tls_dir/certificate.pem" >/dev/null 2>&1
chmod 0444 "$tls_dir/private-key.pem" "$tls_dir/certificate.pem" "$tls_dir/ca.pem"
chmod 0555 "$tls_dir" "$trust_dir" "$identity_dir" "$config_dir"

export LBP_API_KEY_FILE="$api_key_file"
export LBP_TLS_DIRECTORY="$tls_dir"
export LBP_TRUST_DIRECTORY="$trust_dir"
export LBP_IDENTITY_DIRECTORY="$identity_dir"
export LBP_CONFIG_DIRECTORY="$config_dir"
export LBP_TLS_HOSTNAME="$tls_hostname"
export LBP_PROXY_PROD_PORT="$proxy_port"
export LBP_PROXY_PROD_IMAGE="${LBP_PROXY_PROD_IMAGE:-loadbalancerpro:proxy-prod}"
export LBP_PROXY_PROD_FIXTURE_IMAGE="${LBP_PROXY_PROD_FIXTURE_IMAGE:-loadbalancerpro:proxy-prod-fixture}"

compose=(docker compose -p "$project_name" -f "$compose_file")
"${compose[@]}" config --quiet
if [[ "${LBP_PROXY_PROD_REUSE_IMAGE:-false}" == "true" ]]; then
    "${compose[@]}" build backend-a
    "${compose[@]}" up --no-build --detach
else
    "${compose[@]}" up --build --detach
fi

base_url="https://$tls_hostname:$proxy_port"
curl_tls=(--silent --show-error --cacert "$tls_dir/ca.pem" --resolve "$tls_hostname:$proxy_port:127.0.0.1")
for attempt in $(seq 1 120); do
    if curl "${curl_tls[@]}" --fail --header "X-API-Key: $api_key" \
        --output /dev/null "$base_url/actuator/health"; then
        break
    fi
    if [[ "$attempt" -eq 120 ]]; then
        "${compose[@]}" ps
        "${compose[@]}" logs loadbalancerpro
        echo "proxy-prod deployment did not become healthy" >&2
        exit 1
    fi
    sleep 1
done

unauthorized_status="$(curl "${curl_tls[@]}" --output /dev/null --write-out '%{http_code}' \
    "$base_url/api/proxy/status")"
[[ "$unauthorized_status" == "401" ]]
actuator_unauthorized_status="$(curl "${curl_tls[@]}" --output /dev/null --write-out '%{http_code}' \
    "$base_url/actuator/prometheus")"
[[ "$actuator_unauthorized_status" == "401" ]]

status_body="$work_dir/proxy-status.json"
curl "${curl_tls[@]}" --fail --header "X-API-Key: $api_key" \
    --output "$status_body" "$base_url/api/proxy/status"
grep -Eq '"proxyEnabled"[[:space:]]*:[[:space:]]*true' "$status_body"
for attempt in $(seq 1 30); do
    if grep -Eq '"effectiveHealthyBackendCount"[[:space:]]*:[[:space:]]*2' "$status_body"; then
        break
    fi
    sleep 1
    curl "${curl_tls[@]}" --fail --header "X-API-Key: $api_key" \
        --output "$status_body" "$base_url/api/proxy/status"
    if [[ "$attempt" -eq 30 ]]; then
        echo "both fixture backends did not become healthy" >&2
        exit 1
    fi
done

proxy_bodies="$work_dir/proxy-bodies.txt"
for request in 1 2 3 4; do
    curl "${curl_tls[@]}" --fail --header "X-API-Key: $api_key" \
        "$base_url/proxy/smoke?request=$request" >> "$proxy_bodies"
    printf '\n' >> "$proxy_bodies"
done
grep -Fq 'backend-a handled' "$proxy_bodies"
grep -Fq 'backend-b handled' "$proxy_bodies"

sized_response="$work_dir/sized-response.bin"
curl "${curl_tls[@]}" --fail --header "X-API-Key: $api_key" \
    --output "$sized_response" "$base_url/proxy/capacity?lbpResponseBytes=4096"
[[ "$(wc -c < "$sized_response" | tr -d '[:space:]')" == "4096" ]]

request_payload='streamed-proxy-request-body'
curl "${curl_tls[@]}" --fail --header "X-API-Key: $api_key" \
    --data-binary "$request_payload" --output /dev/null "$base_url/proxy/upload"

metrics_body="$work_dir/prometheus.txt"
curl "${curl_tls[@]}" --fail --header "X-API-Key: $api_key" \
    --output "$metrics_body" "$base_url/actuator/prometheus"
grep -Eq '^jvm_' "$metrics_body"
for metric in \
    lbp_proxy_requests_total \
    lbp_proxy_latency_seconds_count \
    lbp_proxy_inflight \
    lbp_proxy_attempts_total \
    lbp_proxy_retries_total \
    lbp_proxy_request_bytes_count \
    lbp_proxy_response_bytes_count \
    lbp_proxy_limit_rejections_total \
    lbp_proxy_sheds_total \
    lbp_proxy_health \
    lbp_proxy_cooldown_trips_total; do
    grep -Eq "^${metric}\\{" "$metrics_body"
done
grep -Eq '^lbp_proxy_request_bytes_sum\{[^}]*\}[[:space:]]+27(\.0)?$' "$metrics_body"
if grep -Fq "$api_key" "$metrics_body" \
        || grep -Fq 'request=1' "$metrics_body" \
        || grep -Fq 'streamed-proxy-request-body' "$metrics_body"; then
    echo "protected proxy metrics exposed request or credential material" >&2
    exit 1
fi

container_id="$("${compose[@]}" ps --quiet loadbalancerpro)"
[[ -n "$container_id" ]]
[[ "$(docker exec "$container_id" id -u)" != "0" ]]
[[ "$(docker inspect --format '{{.HostConfig.ReadonlyRootfs}}' "$container_id")" == "true" ]]
docker inspect --format '{{json .HostConfig.CapDrop}}' "$container_id" | grep -Fq 'ALL'
docker inspect --format '{{json .HostConfig.SecurityOpt}}' "$container_id" | grep -Fq 'no-new-privileges'
mounts="$(docker inspect --format '{{range .Mounts}}{{println .Destination .RW}}{{end}}' "$container_id")"
for read_only_mount in /run/secrets/loadbalancerpro.api.key /run/tls /run/trust /run/identity /run/config; do
    grep -Eq "^${read_only_mount//./\\.} false$" <<< "$mounts"
done
docker exec "$container_id" sh -c \
    'test ! -w /app && test ! -w /run/tls && test ! -w /run/trust && test -w /tmp && : > /tmp/lbp-write-boundary'

docker image save --output "$image_archive" "$LBP_PROXY_PROD_IMAGE"
private_key_marker="$(awk 'NR == 2 { print; exit }' "$tls_dir/private-key.pem")"
certificate_marker="$(awk 'NR == 2 { print; exit }' "$tls_dir/certificate.pem")"
if LC_ALL=C grep -aFq "$api_key" "$image_archive" \
        || LC_ALL=C grep -aFq "$private_key_marker" "$image_archive" \
        || LC_ALL=C grep -aFq "$certificate_marker" "$image_archive"; then
    echo "runtime-generated secret or TLS material was found in the application image" >&2
    exit 1
fi

slow_body="$work_dir/slow-response.txt"
slow_code="$work_dir/slow-status.txt"
curl "${curl_tls[@]}" --fail --header "X-API-Key: $api_key" \
    --output "$slow_body" --write-out '%{http_code}' \
    "$base_url/proxy/slow?millis=2500" > "$slow_code" &
slow_pid=$!
sleep 1
signal_started_at="$(date +%s)"
docker kill --signal=TERM "$container_id" >/dev/null
sleep 1
if curl "${curl_tls[@]}" --fail --max-time 1 --header "X-API-Key: $api_key" \
        --output /dev/null "$base_url/proxy/new-after-sigterm" 2>/dev/null; then
    echo "proxy accepted a new request after SIGTERM" >&2
    exit 1
fi
wait "$slow_pid"
[[ "$(cat "$slow_code")" == "200" ]]
grep -Eq 'backend-(a|b) handled' "$slow_body"

for attempt in $(seq 1 40); do
    if [[ "$(docker inspect --format '{{.State.Running}}' "$container_id")" == "false" ]]; then
        break
    fi
    if [[ "$attempt" -eq 40 ]]; then
        echo "proxy-prod container did not stop inside the bounded grace window" >&2
        exit 1
    fi
    sleep 1
done
signal_elapsed="$(( $(date +%s) - signal_started_at ))"
[[ "$signal_elapsed" -le 40 ]]
exit_code="$(docker inspect --format '{{.State.ExitCode}}' "$container_id")"
[[ "$exit_code" == "0" || "$exit_code" == "143" ]]

echo "proxy-prod Compose smoke passed: TLS, auth, two backends, health, bounded proxy metrics, hardening, external materials, and SIGTERM drain"
