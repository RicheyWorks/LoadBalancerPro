#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
profile="$script_dir/staging-profile.example.json"
validator="$script_dir/validate-staging-target.py"

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

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lbp-staging-validator.XXXXXX")"
cleanup() {
    local status=$?
    trap - EXIT
    case "$work_dir" in
        "${TMPDIR:-/tmp}"/lbp-staging-validator.*) rm -rf -- "$work_dir" ;;
        *) echo "Refusing to remove unexpected validator test path: $work_dir" >&2 ;;
    esac
    exit "$status"
}
trap cleanup EXIT

"$python_command" "$validator" --profile "$profile" >/dev/null

assert_rejected() {
    local name="$1" filter="$2" candidate
    candidate="$work_dir/$name.json"
    jq "$filter" "$profile" > "$candidate"
    if "$python_command" "$validator" --profile "$candidate" >/dev/null 2>&1; then
        echo "Validator accepted unsafe staging mutation: $name" >&2
        exit 1
    fi
}

assert_rejected public-cidr '.target.allowedCidrs = ["0.0.0.0/0"]'
assert_rejected production-host '.target.host = "api.prod.example.com"'
assert_rejected production-sni '.target.tlsServerName = "api.live.example.com"'
assert_rejected production-environment '.environment.name = "production-east"'
assert_rejected production-traffic '.environment.productionTrafficAuthorized = true'
assert_rejected embedded-secret '.target.apiKey = "must-not-be-here"'
assert_rejected plaintext-target '.target.scheme = "http"'
assert_rejected missing-reset-hook 'del(.hooks.reset)'
assert_rejected route-escape '.workload.routeMix[0].path = "/actuator/prometheus"'

printf 'Staging validator accepted the example boundary and rejected nine unsafe mutations.\n'
