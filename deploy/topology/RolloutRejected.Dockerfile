# Deliberately unhealthy candidate used only to prove abort-before-promotion and restoration.
ARG BASE_IMAGE
FROM ${BASE_IMAGE}
ARG ROLLOUT_RELEASE_ID
LABEL com.richeyworks.loadbalancerpro.rollout.release-id="${ROLLOUT_RELEASE_ID}" \
      com.richeyworks.loadbalancerpro.rollout.proof-kind="deliberately-rejected-local-candidate"
ENTRYPOINT ["/bin/false"]
