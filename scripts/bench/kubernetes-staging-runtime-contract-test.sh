#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
adapter_template="$script_dir/kubernetes-staging-adapter-profile.example.json"
staging_template="$script_dir/staging-profile.example.json"
capacity_template="$script_dir/staging-capacity-profile.example.json"
compiler="$script_dir/prepare-kubernetes-staging-adapters.py"
deployment_validator="$script_dir/validate-staging-deployment.py"
capacity_profile_validator="$script_dir/validate-staging-capacity.py"
sample_validator="$script_dir/validate-staging-capacity-sample.py"
target_validator="$script_dir/validate-staging-target.py"

python_command=""
for candidate in python3 python; do
    if command -v "$candidate" >/dev/null 2>&1 \
       && "$candidate" -c 'import sys; raise SystemExit(sys.version_info < (3, 9))' >/dev/null 2>&1; then
        python_command="$candidate"
        break
    fi
done
[[ -n "$python_command" ]] || { echo "Python 3.9 or newer is required" >&2; exit 2; }
for command_name in jq sha256sum timeout; do
    command -v "$command_name" >/dev/null 2>&1 || { echo "$command_name is required" >&2; exit 2; }
done

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-kubernetes-runtime-contract.XXXXXX")"
cleanup() {
    local status=$?
    trap - EXIT
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-kubernetes-runtime-contract.*)
            chmod -R u+w -- "$work_dir" 2>/dev/null || true
            rm -rf -- "$work_dir"
            ;;
        *) echo "Refusing to remove unexpected runtime contract path: $work_dir" >&2 ;;
    esac
    exit "$status"
}
trap cleanup EXIT

fixture_bin="$work_dir/bin"
mkdir "$fixture_bin"
chmod 0700 "$fixture_bin"
python_executable="$(command -v "$python_command")"
cp -- "$script_dir/kubernetes-staging-kubectl-fixture.py" "$fixture_bin/kubectl-fixture.py"
chmod 0400 "$fixture_bin/kubectl-fixture.py"
cat > "$fixture_bin/kubectl" <<SH
#!/usr/bin/env sh
exec "$python_executable" "$fixture_bin/kubectl-fixture.py" "\$@"
SH
chmod 0500 "$fixture_bin/kubectl"
kubectl_sha="$(sha256sum "$fixture_bin/kubectl" | awk '{print $1}')"
reload_payload='{"routes":[{"name":"capacity","pathPrefix":"/proxy/capacity"}]}'
drain_payload='{"routes":[{"name":"capacity","pathPrefix":"/proxy/capacity","targets":[]}]}'
reload_sha="$(printf '%s' "$reload_payload" | sha256sum | awk '{print $1}')"
drain_sha="$(printf '%s' "$drain_payload" | sha256sum | awk '{print $1}')"

state="$work_dir/state.json"
cat > "$state" <<'JSON'
{
  "imageReference": "registry.contract.internal/loadbalancerpro@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "revision": "cccccccccccccccccccccccccccccccccccccccc",
  "generation": 1,
  "podGeneration": 1,
  "tlsSecret": "loadbalancerpro-server-tls-a",
  "slowValue": "0",
  "failureReplicas": 1,
  "reloadCalls": 0
}
JSON
export LBP_KUBECTL_FIXTURE_STATE="$state"
export PATH="$fixture_bin:$PATH"

adapter_profile="$work_dir/adapter-profile.json"
jq --arg kubectlSha "$kubectl_sha" --arg reloadSha "$reload_sha" --arg drainSha "$drain_sha" '
  .adapterId="kubernetes-staging-runtime-contract"
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
  | .faults.reset.payloadSha256=$reloadSha
' "$adapter_template" > "$adapter_profile"

compiled="$work_dir/compiled"
"$python_command" "$compiler" --mode build --profile "$adapter_profile" --output "$compiled" \
    > "$work_dir/build.json"
"$python_command" "$compiled/inspect.sh" > "$work_dir/inspection.json"
jq -e '
  .clusterMutationPerformed == false
  and .context == "contract-staging-context"
  and .namespace == "lbp-contract-staging"
  and (.readyReplicaIds | length == 2)
  and (.configurationSha256 | test("^[0-9a-f]{64}$"))
  and (.ingressIdentitySha256 | test("^[0-9a-f]{64}$"))
' "$work_dir/inspection.json" >/dev/null

staging_profile="$work_dir/staging-profile.json"
jq --slurpfile inspection "$work_dir/inspection.json" --slurpfile bindings "$compiled/bindings.json" '
  .profileId="kubernetes-runtime-contract"
  | .environment.name="lbp-contract-staging"
  | .environment.changeTicket="CHG-CONTRACT-1"
  | .environment.billableImpactReviewed=true
  | .environment.cleanupAuthority="contract-harness"
  | .review={status:"reviewed",approvedBy:"contract-reviewer",approvedAt:"2026-01-01T00:00:00Z"}
  | .artifact.registryRepository="registry.contract.internal/loadbalancerpro"
  | .artifact.prior.imageDigest="sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  | .artifact.prior.sourceRevision="cccccccccccccccccccccccccccccccccccccccc"
  | .artifact.candidate.imageDigest="sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
  | .artifact.candidate.sourceRevision="dddddddddddddddddddddddddddddddddddddddd"
  | .deployment.configurationSha256=$inspection[0].configurationSha256
  | .deployment.ingressIdentitySha256=$inspection[0].ingressIdentitySha256
  | .target.host="lbp.staging.internal"
  | .target.tlsServerName="lbp.staging.internal"
  | .hooks=$bindings[0].actions
' "$staging_template" > "$staging_profile"
"$python_command" "$target_validator" --profile "$staging_profile" >/dev/null

export LBP_STAGING_REVIEWED_PROFILE="$staging_profile"
export LBP_STAGING_CHANGE_TICKET="CHG-CONTRACT-1"
export LBP_STAGING_PRIOR_IMAGE_REFERENCE="registry.contract.internal/loadbalancerpro@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
export LBP_STAGING_CANDIDATE_IMAGE_REFERENCE="registry.contract.internal/loadbalancerpro@sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
export LBP_STAGING_PRIOR_SOURCE_REVISION="cccccccccccccccccccccccccccccccccccccccc"
export LBP_STAGING_CANDIDATE_SOURCE_REVISION="dddddddddddddddddddddddddddddddddddddddd"

validate_snapshot() {
    local role="$1" phase="$2" output
    output="$work_dir/$role-$phase.json"
    LBP_STAGING_EXPECTED_PHASE="$phase" "$python_command" "$compiled/actions/$role.sh" > "$output"
    "$python_command" "$deployment_validator" --profile "$staging_profile" \
        --snapshot "$output" --phase "$phase" >/dev/null
}

update_state() {
    local filter="$1"
    jq "$filter" "$state" > "$state.next"
    mv -- "$state.next" "$state"
}

assert_rejected() {
    local name="$1"
    shift
    if "$@" >"$work_dir/$name.stdout" 2>"$work_dir/$name.stderr"; then
        echo "Unsafe Kubernetes runtime condition unexpectedly accepted: $name" >&2
        exit 1
    fi
}

validate_snapshot verify-deployment prior
validate_snapshot rollout-candidate candidate
jq -e '
  .imageReference == "registry.contract.internal/loadbalancerpro@sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
  and .revision == "dddddddddddddddddddddddddddddddddddddddd"
' "$state" >/dev/null

staging_sha="$(sha256sum "$staging_profile" | awk '{print $1}')"
sampler_sha="$(jq -r '.capacitySamplerSha256' "$compiled/bindings.json")"
capacity_profile="$work_dir/capacity-profile.json"
jq --arg stagingSha "$staging_sha" --arg samplerSha "$sampler_sha" '
  .profileId="kubernetes-runtime-capacity-contract"
  | .review={status:"reviewed",approvedBy:"capacity-contract-reviewer",approvedAt:"2026-01-01T00:00:00Z"}
  | .stagingBinding.stagingProfileSha256=$stagingSha
  | .telemetry.samplerSha256=$samplerSha
' "$capacity_template" > "$capacity_profile"
"$python_command" "$capacity_profile_validator" --staging-profile "$staging_profile" \
    --capacity-profile "$capacity_profile" --execution >/dev/null
LBP_STAGING_EXPECTED_PHASE=candidate timeout --foreground 3s "$python_command" "$compiled/capacity-sampler.sh" \
    > "$work_dir/capacity-sample.json"
"$python_command" "$sample_validator" --staging-profile "$staging_profile" \
    --capacity-profile "$capacity_profile" --sample "$work_dir/capacity-sample.json" >/dev/null

"$python_command" "$compiled/actions/slow.sh"
jq -e '.slowValue == "750"' "$state" >/dev/null
"$python_command" "$compiled/actions/reset.sh"
jq -e '.slowValue == "0"' "$state" >/dev/null
"$python_command" "$compiled/actions/failure.sh"
jq -e '.failureReplicas == 0' "$state" >/dev/null
"$python_command" "$compiled/actions/reset.sh"
jq -e '.failureReplicas == 1' "$state" >/dev/null

reload_before="$(jq -r '.reloadCalls' "$state")"
"$python_command" "$compiled/actions/reload.sh"
"$python_command" "$compiled/actions/drain.sh"
jq -e --argjson before "$reload_before" '.reloadCalls == ($before + 4)' "$state" >/dev/null
"$python_command" "$compiled/actions/reset.sh"

pods_before_restart="$(jq -r '.podGeneration' "$state")"
"$python_command" "$compiled/actions/restart.sh"
jq -e --argjson before "$pods_before_restart" '.podGeneration > $before' "$state" >/dev/null
"$python_command" "$compiled/actions/certificate-rotation.sh"
jq -e '.tlsSecret == "loadbalancerpro-server-tls-b"' "$state" >/dev/null
"$python_command" "$compiled/actions/reset.sh"
jq -e '
  .tlsSecret == "loadbalancerpro-server-tls-a"
  and .slowValue == "0"
  and .failureReplicas == 1
' "$state" >/dev/null
validate_snapshot verify-deployment candidate

fake_staging_profile="$work_dir/unreviewed-artifact-profile.json"
jq '
  .artifact.candidate.imageDigest="sha256:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
  | .artifact.candidate.sourceRevision="ffffffffffffffffffffffffffffffffffffffff"
' "$staging_profile" > "$fake_staging_profile"
assert_rejected unreviewed-artifact-binding env \
  LBP_STAGING_REVIEWED_PROFILE="$fake_staging_profile" \
  LBP_STAGING_CANDIDATE_IMAGE_REFERENCE="registry.contract.internal/loadbalancerpro@sha256:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee" \
  LBP_STAGING_CANDIDATE_SOURCE_REVISION="ffffffffffffffffffffffffffffffffffffffff" \
  LBP_STAGING_EXPECTED_PHASE=candidate "$python_command" "$compiled/actions/rollout-candidate.sh"

update_state '.currentContext="unreviewed-context"'
assert_rejected wrong-context env LBP_STAGING_EXPECTED_PHASE=candidate "$python_command" "$compiled/actions/verify-deployment.sh"
update_state 'del(.currentContext)'
update_state '.namespaceUid="123e4567-e89b-42d3-a456-426614174999"'
assert_rejected wrong-namespace-uid env LBP_STAGING_EXPECTED_PHASE=candidate "$python_command" "$compiled/actions/verify-deployment.sh"
update_state 'del(.namespaceUid)'
update_state '.configurationDrift=true'
assert_rejected configuration-drift env LBP_STAGING_EXPECTED_PHASE=candidate "$python_command" "$compiled/actions/verify-deployment.sh"
update_state 'del(.configurationDrift)'
update_state '.ingressDrift=true'
assert_rejected ingress-drift env LBP_STAGING_EXPECTED_PHASE=candidate "$python_command" "$compiled/actions/verify-deployment.sh"
update_state 'del(.ingressDrift)'
update_state '.maxUnavailable=1'
assert_rejected rollout-strategy-drift env LBP_STAGING_EXPECTED_PHASE=candidate "$python_command" "$compiled/actions/verify-deployment.sh"
update_state 'del(.maxUnavailable)'
update_state '.missingMetricPod=("loadbalancerpro-a-" + (.podGeneration | tostring))'
assert_rejected missing-reviewed-metric env LBP_STAGING_EXPECTED_PHASE=candidate "$python_command" "$compiled/actions/verify-deployment.sh"
update_state 'del(.missingMetricPod)'
update_state '.actionPayloadDrift=true'
assert_rejected action-payload-drift "$python_command" "$compiled/actions/reload.sh"
update_state 'del(.actionPayloadDrift)'

validate_snapshot rollback-prior rollback
chmod 0700 "$fixture_bin/kubectl"
printf '\n' >> "$fixture_bin/kubectl"
chmod 0500 "$fixture_bin/kubectl"
assert_rejected changed-kubectl-binary env LBP_STAGING_EXPECTED_PHASE=rollback "$python_command" "$compiled/actions/verify-deployment.sh"

printf '%s\n' \
  'Kubernetes runtime contract exercised rollout, rollback, faults, reset, restart, TLS rotation, capacity telemetry, and 9 fail-closed drift boundaries.'
