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
  exit 0
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required for the Unix fixture backend helper." >&2
  echo "Use docs/PROXY_DEMO_STACK.md for manual fixture alternatives." >&2
  exit 1
fi

TMP_DIR="$(mktemp -d)"
BACKEND_A_PID=""
BACKEND_B_PID=""

cleanup() {
  if [[ -n "$BACKEND_A_PID" ]]; then
    kill "$BACKEND_A_PID" >/dev/null 2>&1 || true
  fi
  if [[ -n "$BACKEND_B_PID" ]]; then
    kill "$BACKEND_B_PID" >/dev/null 2>&1 || true
  fi
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT INT TERM

cat > "$TMP_DIR/fixture_backend.py" <<'PY'
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import sys

name = sys.argv[1]
port = int(sys.argv[2])
healthy = True

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        global healthy
        if self.path == "/fixture/health/fail":
            healthy = False
            self.respond(200, f"{name} fixture health set to failing")
        elif self.path == "/fixture/health/ok":
            healthy = True
            self.respond(200, f"{name} fixture health set to healthy")
        elif self.path == "/health":
            self.respond(200 if healthy else 503, f"{name} health={healthy}")
        else:
            self.respond(200, f"{name} handled GET {self.path}")

    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length).decode("utf-8", errors="replace")
        self.respond(200, f"{name} handled POST {self.path} body={body}")

    def respond(self, status, body):
        encoded = body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("X-Fixture-Upstream", name)
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def log_message(self, format, *args):
        return

server = ThreadingHTTPServer(("127.0.0.1", port), Handler)
server.serve_forever()
PY

python3 "$TMP_DIR/fixture_backend.py" backend-a "$BACKEND_A_PORT" &
BACKEND_A_PID="$!"
python3 "$TMP_DIR/fixture_backend.py" backend-b "$BACKEND_B_PORT" &
BACKEND_B_PID="$!"

sleep 0.5

PROFILE="$(profile_for_mode "$MODE")"
PROXY_ARGS="--spring.profiles.active=$PROFILE"
if [[ "$BACKEND_A_PORT" != "18081" ]]; then
  PROXY_ARGS="$PROXY_ARGS --loadbalancerpro.proxy.upstreams[0].url=http://127.0.0.1:$BACKEND_A_PORT"
fi
if [[ "$BACKEND_B_PORT" != "18082" ]]; then
  PROXY_ARGS="$PROXY_ARGS --loadbalancerpro.proxy.upstreams[1].url=http://127.0.0.1:$BACKEND_B_PORT"
fi

cat <<EOF
Started local demo backends:
  backend-a http://127.0.0.1:$BACKEND_A_PORT
  backend-b http://127.0.0.1:$BACKEND_B_PORT

Selected strategy demo mode: $MODE
Checked-in Spring profile: $PROFILE
Profile file: src/main/resources/application-$PROFILE.properties

Start LoadBalancerPro in a second terminal:
  mvn spring-boot:run "-Dspring-boot.run.arguments=$PROXY_ARGS"

Try:
EOF

case "$MODE" in
  weighted-round-robin)
    cat <<'EOF'
  # Expected first four selected upstreams with weights 3:1: backend-a, backend-a, backend-b, backend-a
  curl -i http://127.0.0.1:8080/proxy/weighted?step=1
  curl -i http://127.0.0.1:8080/proxy/weighted?step=2
  curl -i http://127.0.0.1:8080/proxy/weighted?step=3
  curl -i http://127.0.0.1:8080/proxy/weighted?step=4
EOF
    ;;
  failover)
    cat <<EOF
  curl http://127.0.0.1:$BACKEND_B_PORT/fixture/health/fail
  curl -i http://127.0.0.1:8080/proxy/failover?step=1
  curl -s http://127.0.0.1:8080/api/proxy/status
  curl http://127.0.0.1:$BACKEND_B_PORT/fixture/health/ok
  curl -i http://127.0.0.1:8080/proxy/failover?step=2
EOF
    ;;
  *)
    cat <<'EOF'
  # Expected first four selected upstreams: backend-a, backend-b, backend-a, backend-b
  curl -i http://127.0.0.1:8080/proxy/demo?step=1
  curl -i http://127.0.0.1:8080/proxy/demo?step=2
  curl -i http://127.0.0.1:8080/proxy/demo?step=3
  curl -i http://127.0.0.1:8080/proxy/demo?step=4
EOF
    ;;
esac

cat <<'EOF'
  curl -s http://127.0.0.1:8080/api/proxy/status
  Browser status page: http://localhost:8080/proxy-status.html

Press Enter to stop demo backends.
EOF

read -r _
