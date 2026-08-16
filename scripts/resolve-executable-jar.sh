#!/usr/bin/env bash
set -euo pipefail

EXPECTED_ONLY="false"
LAB="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --expected-only)
      EXPECTED_ONLY="true"
      ;;
    --lab)
      LAB="true"
      ;;
    *)
      echo "Usage: scripts/resolve-executable-jar.sh [--expected-only] [--lab]" >&2
      exit 1
      ;;
  esac
  shift
done

if [[ ! -f pom.xml ]]; then
  echo "pom.xml not found; run the executable-JAR resolver from the repository root." >&2
  exit 1
fi

MAVEN_ARGS=(-q -DforceStdout -Dstyle.color=never)
if [[ "$LAB" == "true" ]]; then
  MAVEN_ARGS+=(-P lab)
fi
MAVEN_ARGS+=(help:evaluate -Dexpression=project.build.finalName)

FINAL_NAME="$(
  mvn "${MAVEN_ARGS[@]}" |
    tail -n 1 |
    tr -d '\r'
)"

if [[ -z "$FINAL_NAME" || ! "$FINAL_NAME" =~ ^[A-Za-z0-9._+-]+$ ]]; then
  echo "Maven returned an invalid project.build.finalName: $FINAL_NAME" >&2
  exit 1
fi

JAR_PATH="target/${FINAL_NAME}.jar"
if [[ "$EXPECTED_ONLY" != "true" && ! -f "$JAR_PATH" ]]; then
  PACKAGE_COMMAND="mvn -B -DskipTests package"
  if [[ "$LAB" == "true" ]]; then
    PACKAGE_COMMAND="mvn -B -P lab -DskipTests package"
  fi
  echo "Expected executable jar not found: $JAR_PATH. Run $PACKAGE_COMMAND first." >&2
  exit 1
fi

printf '%s\n' "$JAR_PATH"
