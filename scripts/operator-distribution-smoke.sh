#!/usr/bin/env bash
set -euo pipefail

PACKAGE="false"
RUN_JAR_SMOKE="false"
PORT="18080"

usage() {
  cat <<'USAGE'
LoadBalancerPro release-free operator distribution smoke kit

Usage:
  scripts/operator-distribution-smoke.sh
  scripts/operator-distribution-smoke.sh --package
  scripts/operator-distribution-smoke.sh --package --run-jar-smoke

Options:
  --package          Run mvn -B -DskipTests package and verify the executable jar exists.
  --run-jar-smoke    Start the packaged jar on loopback and check health/status pages.
  --port PORT        Loopback port for jar smoke, default 18080.
  -h, --help         Show this help.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --package)
      PACKAGE="true"
      shift
      ;;
    --run-jar-smoke)
      RUN_JAR_SMOKE="true"
      shift
      ;;
    --port)
      PORT="${2:-}"
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

assert_path_exists() {
  local path="$1"
  if [[ ! -e "$path" ]]; then
    echo "Required smoke asset is missing: $path" >&2
    exit 1
  fi
  echo "OK: $path"
}

find_executable_jar() {
  local jar
  if [[ ! -d target ]]; then
    echo "Executable jar not found because target/ does not exist. Run with --package or run mvn -B -DskipTests package first." >&2
    exit 1
  fi
  jar="$(find target -maxdepth 1 -type f -name 'LoadBalancerPro-*.jar' \
    ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*-tests.jar' ! -name '*.original.jar' \
    | sort | tail -n 1)"
  if [[ -z "$jar" ]]; then
    echo "Executable jar not found under target/. Run with --package or run mvn -B -DskipTests package first." >&2
    exit 1
  fi
  printf '%s\n' "$jar"
}

wait_for_url() {
  local url="$1"
  local attempts=30
  local attempt
  for attempt in $(seq 1 "$attempts"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "OK: $url"
      return 0
    fi
    sleep 1
  done
  echo "Timed out waiting for $url" >&2
  return 1
}

echo "LoadBalancerPro release-free operator distribution smoke kit"
echo "No tag, release, or asset creation is performed by this script."
echo

required_paths=(
  "pom.xml"
  "src/main/resources/static/proxy-status.html"
  "src/main/resources/static/load-balancing-cockpit.html"
  "src/main/resources/application.properties"
  "src/main/resources/application-proxy-demo-round-robin.properties"
  "src/main/resources/application-proxy-demo-weighted-round-robin.properties"
  "src/main/resources/application-proxy-demo-failover.properties"
  "docs/examples/proxy/application-proxy-real-backend-example.properties"
  "docs/examples/proxy/application-proxy-real-backend-weighted-example.properties"
  "docs/examples/proxy/application-proxy-real-backend-failover-example.properties"
  "docs/OPERATOR_DISTRIBUTION_SMOKE_KIT.md"
  "docs/OPERATOR_PACKAGING.md"
  "docs/PROXY_DEMO_FIXTURE_LAUNCHER.md"
  "docs/PROXY_DEMO_STACK.md"
)

for path in "${required_paths[@]}"; do
  assert_path_exists "$path"
done

cat <<COMMANDS

Package command:
  mvn -B -DskipTests package

Maven exec fixture launcher:
  mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin"

Packaged jar startup:
  java -jar target/LoadBalancerPro-2.4.2.jar --server.address=127.0.0.1 --server.port=$PORT --spring.profiles.active=local

Proxy status checks:
  curl -fsS http://127.0.0.1:$PORT/api/health
  curl -fsS http://127.0.0.1:$PORT/proxy-status.html
  curl -fsS http://127.0.0.1:$PORT/api/proxy/status

Demo profiles:
  proxy-demo-round-robin
  proxy-demo-weighted-round-robin
  proxy-demo-failover
COMMANDS

JAR_PATH=""
if [[ "$PACKAGE" == "true" ]]; then
  echo
  echo "Running package smoke:"
  mvn -B -DskipTests package
  JAR_PATH="$(find_executable_jar)"
  echo "OK: packaged jar $JAR_PATH"
elif [[ "$RUN_JAR_SMOKE" == "true" ]]; then
  JAR_PATH="$(find_executable_jar)"
fi

if [[ "$RUN_JAR_SMOKE" == "true" ]]; then
  if ! command -v curl >/dev/null 2>&1; then
    echo "curl is required for --run-jar-smoke." >&2
    exit 1
  fi
  if [[ -z "$JAR_PATH" ]]; then
    JAR_PATH="$(find_executable_jar)"
  fi
  echo
  echo "Running packaged jar HTTP smoke on 127.0.0.1:$PORT"
  LOG_FILE="${TMPDIR:-/tmp}/loadbalancerpro-distribution-smoke.log"
  java -jar "$JAR_PATH" \
    --server.address=127.0.0.1 \
    --server.port="$PORT" \
    --spring.profiles.active=local \
    >"$LOG_FILE" 2>&1 &
  APP_PID="$!"
  trap 'kill "$APP_PID" >/dev/null 2>&1 || true' EXIT
  wait_for_url "http://127.0.0.1:$PORT/api/health"
  wait_for_url "http://127.0.0.1:$PORT/proxy-status.html"
  wait_for_url "http://127.0.0.1:$PORT/api/proxy/status"
  kill "$APP_PID" >/dev/null 2>&1 || true
  trap - EXIT
  echo "Stopped packaged jar smoke process."
fi
