#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
profile_template="$script_dir/kubernetes-staging-adapter-profile.example.json"
compiler="$script_dir/prepare-kubernetes-staging-adapters.py"

python_command=""
for candidate in python3 python; do
    if command -v "$candidate" >/dev/null 2>&1 \
       && "$candidate" -c 'import sys; raise SystemExit(sys.version_info < (3, 9))' >/dev/null 2>&1; then
        python_command="$candidate"
        break
    fi
done
[[ -n "$python_command" ]] || { echo "Python 3.9 or newer is required" >&2; exit 2; }
for command_name in jq sha256sum; do
    command -v "$command_name" >/dev/null 2>&1 || { echo "$command_name is required" >&2; exit 2; }
done

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-kubernetes-adapter-contract.XXXXXX")"
cleanup() {
    local status=$?
    trap - EXIT
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-kubernetes-adapter-contract.*)
            chmod -R u+w -- "$work_dir" 2>/dev/null || true
            rm -rf -- "$work_dir"
            ;;
        *) echo "Refusing to remove unexpected adapter contract path: $work_dir" >&2 ;;
    esac
    exit "$status"
}
trap cleanup EXIT

kubectl_fixture="$work_dir/kubectl"
cat > "$kubectl_fixture" <<'SH'
#!/usr/bin/env sh
exit 0
SH
chmod 0500 "$kubectl_fixture"
kubectl_sha="$(sha256sum "$kubectl_fixture" | awk '{print $1}')"
reload_sha="$(printf '%s' '{"routes":[{"name":"capacity","pathPrefix":"/proxy/capacity"}]}' | sha256sum | awk '{print $1}')"
drain_sha="$(printf '%s' '{"routes":[{"name":"capacity","pathPrefix":"/proxy/capacity","targets":[]}]}' | sha256sum | awk '{print $1}')"
reset_sha="$reload_sha"

reviewed="$work_dir/reviewed.json"
jq --arg kubectlSha "$kubectl_sha" --arg reloadSha "$reload_sha" \
  --arg drainSha "$drain_sha" --arg resetSha "$reset_sha" '
  .adapterId="kubernetes-staging-contract"
  | .review={status:"reviewed",approvedBy:"contract-reviewer",approvedAt:"2026-01-01T00:00:00Z"}
  | .kubectl.executableSha256=$kubectlSha
  | .cluster.context="contract-staging-context"
  | .cluster.apiServer="https://api.staging.internal"
  | .cluster.namespace="lbp-contract-staging"
  | .cluster.namespaceUid="123e4567-e89b-42d3-a456-426614174000"
  | .proxy.localApi.tlsHost="lbp.staging.internal"
  | .stagingBinding.changeTicket="CHG-CONTRACT-1"
  | .stagingBinding.artifact.registryRepository="registry.contract.internal/loadbalancerpro"
  | .stagingBinding.artifact.prior.imageDigest="sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  | .stagingBinding.artifact.prior.sourceRevision="cccccccccccccccccccccccccccccccccccccccc"
  | .stagingBinding.artifact.candidate.imageDigest="sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
  | .stagingBinding.artifact.candidate.sourceRevision="dddddddddddddddddddddddddddddddddddddddd"
  | .faults.reload.payloadSha256=$reloadSha
  | .faults.drain.payloadSha256=$drainSha
  | .faults.reset.payloadSha256=$resetSha
' "$profile_template" > "$reviewed"

"$python_command" "$compiler" --mode validate --profile "$profile_template" >/dev/null
"$python_command" "$compiler" --mode validate --profile "$reviewed" >/dev/null

compiled="$work_dir/compiled"
"$python_command" "$compiler" --mode build --profile "$reviewed" --output "$compiled" > "$work_dir/build-result.json"
jq -e '
  .executionAuthorized == true
  and .networkAccessPerformed == false
  and .clusterMutationPerformed == false
  and (.actions | length == 10)
  and (.capacitySamplerSha256 == .inspectorSha256)
' "$work_dir/build-result.json" >/dev/null
[[ "$(find "$compiled/actions" -maxdepth 1 -type f -name '*.sh' | wc -l | tr -d ' ')" == "10" ]]
expected_sha="$(jq -r '.capacitySamplerSha256' "$compiled/bindings.json")"
for executable in "$compiled"/actions/*.sh "$compiled/capacity-sampler.sh" "$compiled/inspect.sh"; do
    [[ ! -L "$executable" && -x "$executable" ]]
    [[ "$(sha256sum "$executable" | awk '{print $1}')" == "$expected_sha" ]]
done

if "$python_command" "$compiler" --mode build --profile "$profile_template" \
    --output "$work_dir/unreviewed" >/dev/null 2>&1; then
    echo "Unreviewed adapter profile unexpectedly entered build mode" >&2
    exit 1
fi

rejections=0
assert_rejected() {
    local name="$1" filter="$2"
    local mutated="$work_dir/$name.json" output="$work_dir/$name-output"
    jq "$filter" "$reviewed" > "$mutated"
    if "$python_command" "$compiler" --mode build --profile "$mutated" --output "$output" >/dev/null 2>&1; then
        echo "Unsafe Kubernetes adapter mutation unexpectedly accepted: $name" >&2
        exit 1
    fi
    [[ ! -e "$output" ]] || { echo "Rejected mutation left generated output: $name" >&2; exit 1; }
    rejections=$(( rejections + 1 ))
}

assert_rejected unknown-field '.credential="forbidden"'
assert_rejected wrong-platform '.platform="nomad"'
assert_rejected production-context '.cluster.context="customer-production"'
assert_rejected implicit-context '.cluster.context="shared-cluster"'
assert_rejected default-namespace '.cluster.namespace="default"'
assert_rejected production-api '.cluster.apiServer="https://api.production.internal"'
assert_rejected plaintext-api '.cluster.apiServer="http://api.staging.internal"'
assert_rejected placeholder-namespace-uid '.cluster.namespaceUid="00000000-0000-0000-0000-000000000000"'
assert_rejected wrong-environment-label '.cluster.environmentLabel.value="test"'
assert_rejected placeholder-change-ticket '.stagingBinding.changeTicket="replace-with-ticket"'
assert_rejected duplicate-artifact-digest '.stagingBinding.artifact.candidate.imageDigest=.stagingBinding.artifact.prior.imageDigest'
assert_rejected placeholder-kubectl '.kubectl.executableSha256=("0" * 64)'
assert_rejected excessive-command-window '.kubectl.maximumCommandSeconds=300'
assert_rejected fault-targets-proxy '.faults.slow.deployment=.proxy.deployment'
assert_rejected duplicate-payload '.faults.drain=.faults.reload'
assert_rejected placeholder-payload-hash '.faults.reload.payloadSha256=("0" * 64)'
assert_rejected unchanged-certificate '.faults.certificateRotation.candidateSecret=.faults.certificateRotation.baselineSecret'
assert_rejected one-upstream '.capacity.upstreamIds=["backend-a"]'
assert_rejected secret-path-escape '.proxy.localApi.apiKeyPath="/tmp/api-key"'

[[ "$rejections" == "19" ]]
printf 'Kubernetes adapter compiler produced 12 pinned executables and rejected %s unsafe profiles without cluster access.\n' "$rejections"
