#!/usr/bin/env bash
set -euo pipefail

forks="${LBP_ACCESS_LOG_BENCHMARK_FORKS:-5}"
output_dir="${LBP_ACCESS_LOG_BENCHMARK_OUTPUT:-target/access-log-benchmark}"

if ! [[ "$forks" =~ ^[0-9]+$ ]] || (( forks < 3 || forks > 20 )); then
  echo "LBP_ACCESS_LOG_BENCHMARK_FORKS must be an integer from 3 through 20." >&2
  exit 2
fi

mkdir -p "$output_dir"
cat > "$output_dir/BOUNDARY.txt" <<'EOF'
These fresh-JVM local or hosted-runner samples are diagnostic regression evidence only.
They do not prove the production <5% target, throughput, latency, SLOs, or production readiness.
EOF

for ((fork = 1; fork <= forks; fork++)); do
  result="$output_dir/fork-${fork}.json"
  log="$output_dir/fork-${fork}.log"
  mvn -B \
    -Dtest=ReverseProxyAccessLogBenchmark \
    -DforkCount=1 \
    -DreuseForks=false \
    -Djacoco.skip=true \
    "-Dloadbalancerpro.access-log-benchmark.output=$result" \
    test 2>&1 | tee "$log"
  test -s "$result"
done

printf '{\n  "formatVersion": 1,\n  "forks": %s,\n  "boundary": "non-production diagnostic evidence",\n  "results": [\n' "$forks" \
  > "$output_dir/manifest.json"
for ((fork = 1; fork <= forks; fork++)); do
  separator=','
  if (( fork == forks )); then
    separator=''
  fi
  printf '    "fork-%s.json"%s\n' "$fork" "$separator" >> "$output_dir/manifest.json"
done
printf '  ]\n}\n' >> "$output_dir/manifest.json"

echo "Access-log benchmark artifacts: $output_dir"
