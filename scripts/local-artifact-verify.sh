#!/usr/bin/env bash
set -euo pipefail

JAR_PATH=""
BUILD="false"

usage() {
  cat <<'USAGE'
LoadBalancerPro local artifact verification

Usage:
  scripts/local-artifact-verify.sh
  scripts/local-artifact-verify.sh --build
  scripts/local-artifact-verify.sh --jar PATH

Options:
  --build       Run mvn -B -DskipTests package before verification.
  --jar PATH    Inspect an explicit jar instead of the current Maven project artifact.
  -h, --help    Show this help.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build)
      BUILD="true"
      shift
      ;;
    --jar)
      JAR_PATH="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

checksum() {
  local path="$1"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$path"
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$path"
  else
    echo "sha256sum or shasum is required for checksum verification." >&2
    exit 1
  fi
}

assert_entry() {
  local entries="$1"
  local entry="$2"
  if ! grep -Fxq "$entry" "$entries"; then
    echo "Required jar entry is missing: $entry" >&2
    exit 1
  fi
  echo "OK: $entry"
}

assert_absent() {
  local entries="$1"
  local pattern="$2"
  local description="$3"
  if grep -Eiq "$pattern" "$entries"; then
    echo "Forbidden production artifact content: $description" >&2
    grep -Ei "$pattern" "$entries" >&2
    exit 1
  fi
  echo "ABSENT: $description"
}

echo "LoadBalancerPro local artifact verification"
echo "Release-free: no tags, releases, assets, or release workflow changes."
echo "CI artifact parity: packaged-artifact-smoke contains artifact-smoke-summary.txt, artifact-sha256.txt, and jar-resource-list.txt."
echo

if [[ "$BUILD" == "true" ]]; then
  echo "Running local package build:"
  mvn -B -DskipTests package
fi

if [[ -z "$JAR_PATH" ]]; then
  JAR_PATH="$(bash scripts/resolve-executable-jar.sh)"
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Jar not found: $JAR_PATH. Run with --build or run mvn -B -DskipTests package first." >&2
  exit 1
fi

echo "SHA-256:"
checksum "$JAR_PATH"
echo

ENTRY_FILE="$(mktemp)"
trap 'rm -f "$ENTRY_FILE"' EXIT
jar tf "$JAR_PATH" > "$ENTRY_FILE"

echo "Required jar entries:"
required_entries=(
  "META-INF/MANIFEST.MF"
  "BOOT-INF/classes/static/proxy-status.html"
  "BOOT-INF/classes/static/load-balancing-cockpit.html"
  "BOOT-INF/classes/application-proxy-demo-round-robin.properties"
  "BOOT-INF/classes/application-proxy-demo-weighted-round-robin.properties"
  "BOOT-INF/classes/application-proxy-demo-failover.properties"
  "BOOT-INF/classes/application-proxy-prod.properties"
  "BOOT-INF/classes/com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.class"
)

for entry in "${required_entries[@]}"; do
  assert_entry "$ENTRY_FILE" "$entry"
done

echo
echo "Forbidden jar entries:"
assert_absent "$ENTRY_FILE" '^BOOT-INF/classes/.*(Test|Tests)\.class$' "test classes"
assert_absent "$ENTRY_FILE" '\.(exe|dll|msi|dmg|pkg|deb|rpm|appimage)$' "unexpected native installers or executables"
assert_absent "$ENTRY_FILE" '\.(pem|p12|pfx|jks|keystore|key)$' "embedded key, certificate, or trust material"

cat <<COMMANDS

Packaged jar startup:
  java -jar $JAR_PATH --server.address=127.0.0.1 --server.port=8080 --spring.profiles.active=local

Status and static page checks:
  curl -fsS http://127.0.0.1:8080/api/health
  curl -fsS http://127.0.0.1:8080/proxy-status.html
  curl -fsS http://127.0.0.1:8080/load-balancing-cockpit.html
  curl -fsS http://127.0.0.1:8080/api/proxy/status

Maven exec fixture launcher:
  mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin"

GitHub Actions artifact:
  packaged-artifact-smoke

Do not commit generated jars, checksums, manifests, or smoke output.
COMMANDS
