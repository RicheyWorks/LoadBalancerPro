#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 4 ]]; then
    echo "Usage: $0 EVIDENCE_DIR SOURCE_SHA WORKFLOW_SHA DRY_RUN_IMAGE_TAG" >&2
    exit 2
fi

evidence_dir="$1"
source_sha="$2"
workflow_sha="$3"
dry_run_tag="$4"

for command_name in docker jq sha256sum; do
    command -v "$command_name" >/dev/null 2>&1 || {
        echo "$command_name is required to validate image SBOM evidence" >&2
        exit 2
    }
done

[[ "$source_sha" =~ ^[0-9a-f]{40}$ ]] || {
    echo "Source SHA must be a full lowercase Git commit ID" >&2
    exit 2
}
[[ "$workflow_sha" =~ ^[0-9a-f]{40}$ ]] || {
    echo "Workflow SHA must be a full lowercase Git commit ID" >&2
    exit 2
}
[[ -d "$evidence_dir" && ! -L "$evidence_dir" ]] || {
    echo "Evidence directory must be a non-symlink directory" >&2
    exit 2
}

sbom="$evidence_dir/image-sbom.cdx.json"
image_id_file="$evidence_dir/image-id.txt"
summary="$evidence_dir/dry-run-summary.md"
checksum_file="$evidence_dir/image-sbom.cdx.sha256"
binding="$evidence_dir/image-sbom-binding.json"
for evidence_file in "$sbom" "$image_id_file" "$summary"; do
    [[ -s "$evidence_file" && ! -L "$evidence_file" ]] || {
        echo "Required image SBOM evidence file is missing, empty, or a symlink: $evidence_file" >&2
        exit 2
    }
done
for generated_file in "$checksum_file" "$binding"; do
    [[ ! -L "$generated_file" ]] || {
        echo "Refusing to replace symlinked image SBOM evidence: $generated_file" >&2
        exit 2
    }
done

jq -e '
  .bomFormat == "CycloneDX"
  and (.specVersion | type == "string" and length > 0)
  and (.serialNumber | type == "string" and startswith("urn:uuid:"))
  and .version == 1
  and (.metadata | type == "object")
  and (.components | type == "array" and length > 0)
' "$sbom" >/dev/null || {
    echo "Image SBOM is not a populated CycloneDX document" >&2
    exit 1
}

recorded_image_id="$(tr -d '\r\n' < "$image_id_file")"
current_image_id="$(docker image inspect "$dry_run_tag" --format '{{.Id}}')"
[[ "$recorded_image_id" =~ ^sha256:[0-9a-f]{64}$ ]] || {
    echo "Recorded dry-run image ID is not an immutable Docker content ID" >&2
    exit 1
}
[[ "$current_image_id" == "$recorded_image_id" ]] || {
    echo "Dry-run image identity changed before SBOM binding" >&2
    exit 1
}

sbom_sha256="$(sha256sum "$sbom" | awk '{print $1}')"
printf '%s  %s\n' "$sbom_sha256" "$(basename "$sbom")" > "$checksum_file"
(cd "$evidence_dir" && sha256sum --check --strict "$(basename "$checksum_file")")

jq -n \
    --arg sourceCommitSha "$source_sha" \
    --arg workflowSha "$workflow_sha" \
    --arg buildImageTag "loadbalancerpro:ci" \
    --arg dryRunImageTag "$dry_run_tag" \
    --arg imageId "$recorded_image_id" \
    --arg sbomPath "$(basename "$sbom")" \
    --arg sbomSha256 "$sbom_sha256" \
    '{schemaVersion:1,
      sourceCommitSha:$sourceCommitSha,
      workflowSha:$workflowSha,
      buildImageTag:$buildImageTag,
      dryRunImageTag:$dryRunImageTag,
      imageId:$imageId,
      sbom:{format:"CycloneDX", path:$sbomPath, sha256:$sbomSha256},
      published:false,
      signed:false}' > "$binding"

jq -e \
    --arg sourceCommitSha "$source_sha" \
    --arg workflowSha "$workflow_sha" \
    --arg imageId "$recorded_image_id" \
    --arg dryRunImageTag "$dry_run_tag" \
    --arg sbomSha256 "$sbom_sha256" '
      .schemaVersion == 1
      and .sourceCommitSha == $sourceCommitSha
      and .workflowSha == $workflowSha
      and .buildImageTag == "loadbalancerpro:ci"
      and .dryRunImageTag == $dryRunImageTag
      and .imageId == $imageId
      and .sbom.format == "CycloneDX"
      and .sbom.sha256 == $sbomSha256
      and .published == false
      and .signed == false
    ' "$binding" >/dev/null

{
    echo ""
    echo "## Image SBOM"
    echo ""
    echo "- CycloneDX image SBOM: \`$(basename "$sbom")\`"
    echo "- SBOM checksum: \`$(basename "$checksum_file")\`"
    echo "- Image/SBOM binding: \`$(basename "$binding")\`"
    echo "- The binding records the exact local image ID, source SHA, workflow SHA, and SBOM SHA-256."
} >> "$summary"

printf 'Validated CycloneDX image SBOM evidence for local image %s.\n' "$recorded_image_id"
