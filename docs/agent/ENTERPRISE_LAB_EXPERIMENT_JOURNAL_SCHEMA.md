# Enterprise Lab Experiment Journal Schema

## Implemented Boundary

`enterprise-lab-experiment-journal-event/v1` is the canonical, data-only envelope for durable single-instance
Enterprise Lab experiment evidence. `enterprise-lab-experiment-journal-payload/v1` is its bounded generic payload
schema. PR1 implements the immutable model, canonical codec, and content fingerprint only; it does not write files,
verify a multi-entry chain, replay an experiment, or reconcile startup state.

The envelope records sequence, experiment and scenario identity, event type, lifecycle states, logical cycle, injected
timestamp, configuration/decision/baseline/candidate/applied allocation fingerprint references, structured reason,
predecessor fingerprint, bounded metadata, payload schema and payload, plus the current content fingerprint.

## Canonicalization

- Encoding is strict UTF-8 JSON with no byte-order mark, pretty printing, or trailing newline.
- Envelope and reason field order is fixed by the codec.
- Metadata and payload object keys are sorted recursively.
- Payload arrays are canonicalized as unordered evidence collections and sorted by canonical element JSON. An
  order-sensitive payload must carry an explicit ordinal field in each element.
- Equivalent integer/decimal spellings are normalized before encoding.
- The current fingerprint is lowercase SHA-256 over canonical envelope content excluding only
  `currentEntryFingerprint`.
- Sequence 1 must reference `GENESIS`; every later sequence must reference a lowercase SHA-256 value.

Fingerprints detect content modification. They do not authenticate an author, prove signer identity or
non-repudiation, or make the eventual filesystem storage tamper-proof.

## Compatibility And Rejection

Every v1 envelope and structured reason field is required. Unknown envelope or reason fields are rejected. Unknown
event enum values, duplicate JSON fields, trailing JSON values, malformed UTF-8, unsupported event versions, and
unsupported payload versions fail closed. Fields inside the generic v1 payload are deliberately open data, subject to
the bounds and safety validation below; adding a new payload interpretation requires its own explicit version policy.

Limits are enforced before the future storage boundary:

- 65,536 bytes per complete canonical entry
- 32,768 bytes per canonical payload
- 1,000,000 maximum sequence and logical cycle
- 12 payload levels, 512 payload nodes, and 64 fields or collection values per container
- 2,048 characters per payload string and 128 characters per payload field name
- 16 metadata entries with bounded keys and values

Payloads must be JSON objects. The codec has no Java native serialization, polymorphic type activation, reflection-based
class loading, or executable deserialization behavior. Credential-like field names and values, authorization headers,
cookies, private-key material, and Java stack traces are rejected. Durable evidence must contain structured sanitized
reason data instead.

## Event Vocabulary

The v1 vocabulary covers arming, start, candidate allocation, lifecycle transitions, observation checkpoints, hold
evaluation, rollback request, restoration attempt and success, cancellation, completion, rollback, rejection, failure,
recovery action, and quarantine finding. Legal cross-entry lifecycle transitions and terminal behavior are verified by
the later read-only chain verifier rather than guessed by the codec.
