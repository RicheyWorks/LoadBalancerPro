#!/usr/bin/env bash
set -euo pipefail

EXPECTED_ONLY="false"

if [[ "${1:-}" == "--expected-only" ]]; then
  EXPECTED_ONLY="true"
  shift
fi

if [[ $# -ne 0 ]]; then
  echo "Usage: scripts/resolve-executable-jar.sh [--expected-only]" >&2
  exit 1
fi

if [[ ! -f pom.xml ]]; then
  echo "pom.xml not found; run the executable-JAR resolver from the repository root." >&2
  exit 1
fi

FINAL_NAME="$(
  mvn -q -DforceStdout -Dstyle.color=never help:evaluate \
    -Dexpression=project.build.finalName |
    tail -n 1 |
    tr -d '\r'
)"

if [[ -z "$FINAL_NAME" || ! "$FINAL_NAME" =~ ^[A-Za-z0-9._+-]+$ ]]; then
  echo "Maven returned an invalid project.build.finalName: $FINAL_NAME" >&2
  exit 1
fi

JAR_PATH="target/${FINAL_NAME}.jar"
if [[ "$EXPECTED_ONLY" != "true" && ! -f "$JAR_PATH" ]]; then
  echo "Expected executable jar not found: $JAR_PATH. Run mvn -B -DskipTests package first." >&2
  exit 1
fi

printf '%s\n' "$JAR_PATH"
