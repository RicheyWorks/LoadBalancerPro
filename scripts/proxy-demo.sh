#!/usr/bin/env bash
set -euo pipefail

MODE="round-robin"
BACKEND_A_PORT="18081"
BACKEND_B_PORT="18082"

usage() {
  cat <<'USAGE'
LoadBalancerPro local proxy demo stack

Usage:
  scripts/proxy-demo.sh --mode round-robin
  scripts/proxy-demo.sh --mode weighted-round-robin
  scripts/proxy-demo.sh --mode failover
  scripts/proxy-demo.sh --mode status

Options:
  --mode MODE              round-robin, weighted-round-robin, failover, or status
  --backend-a-port PORT    backend-a loopback port, default 18081
  --backend-b-port PORT    backend-b loopback port, default 18082

The script starts loopback fixture backends only. It does not contact cloud services,
does not require public internet, and does not modify repository files.
It uses the Java fixture launcher from this project, so no Python, Node, or Docker
fixture runtime is required.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      MODE="${2:-}"
      shift 2
      ;;
    --backend-a-port)
      BACKEND_A_PORT="${2:-}"
      shift 2
      ;;
    --backend-b-port)
      BACKEND_B_PORT="${2:-}"
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

case "$MODE" in
  round-robin|weighted-round-robin|failover|status) ;;
  *)
    echo "Unsupported mode: $MODE" >&2
    usage >&2
    exit 1
    ;;
esac

profile_for_mode() {
  case "$1" in
    weighted-round-robin) echo "proxy-demo-weighted-round-robin" ;;
    failover) echo "proxy-demo-failover" ;;
    *) echo "proxy-demo-round-robin" ;;
  esac
}

print_status_commands() {
  cat <<'STATUS'
Proxy status UI:
  Browser: http://localhost:8080/proxy-status.html
  curl -s http://127.0.0.1:8080/api/proxy/status

Demo profiles:
  proxy-demo-round-robin
  proxy-demo-weighted-round-robin
  proxy-demo-failover
STATUS
}

if [[ "$MODE" == "status" ]]; then
  print_status_commands
  cat <<'STATUS'

Recommended cross-platform Java fixture launcher:
  mvn -q -DskipTests compile
  java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode round-robin
  java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode weighted-round-robin
  java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode failover
STATUS
  exit 0
fi

if ! command -v java >/dev/null 2>&1; then
  echo "java is required to run the Java fixture launcher." >&2
  echo "Use docs/PROXY_DEMO_STACK.md for manual fixture alternatives." >&2
  exit 1
fi

LAUNCHER_CLASS="target/classes/com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.class"
if [[ ! -f "$LAUNCHER_CLASS" ]]; then
  if ! command -v mvn >/dev/null 2>&1; then
    echo "mvn is required to compile the Java fixture launcher when target/classes is missing." >&2
    echo "Use docs/PROXY_DEMO_STACK.md for manual fixture alternatives." >&2
    exit 1
  fi
  echo "Compiled launcher class not found; compiling Java fixture launcher:"
  echo "  mvn -q -DskipTests compile"
  mvn -q -DskipTests compile
else
  echo "Using existing compiled launcher class: $LAUNCHER_CLASS"
fi

echo
echo "Starting cross-platform Java fixture launcher:"
echo "  java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode $MODE --backend-a-port $BACKEND_A_PORT --backend-b-port $BACKEND_B_PORT"
java -cp target/classes \
  com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher \
  --mode "$MODE" \
  --backend-a-port "$BACKEND_A_PORT" \
  --backend-b-port "$BACKEND_B_PORT"
