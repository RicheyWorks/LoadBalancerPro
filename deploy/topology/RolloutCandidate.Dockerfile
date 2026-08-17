# Creates a content-distinct rollout candidate from an already-built exact proxy image.
# The application layers and entrypoint remain unchanged; only immutable proof metadata differs.
ARG BASE_IMAGE
FROM ${BASE_IMAGE}
ARG ROLLOUT_RELEASE_ID
LABEL com.richeyworks.loadbalancerpro.rollout.release-id="${ROLLOUT_RELEASE_ID}" \
      com.richeyworks.loadbalancerpro.rollout.proof-kind="metadata-only-local-candidate"
