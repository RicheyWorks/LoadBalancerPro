# Enterprise Lab Experiment Journal Schema

## Implemented Boundary

`enterprise-lab-experiment-journal-event/v1` is the canonical, data-only envelope for durable single-instance
Enterprise Lab experiment evidence. `enterprise-lab-experiment-journal-payload/v1` is its bounded generic payload
schema. PR1 implements the immutable model, canonical codec, and content fingerprint; PR2 supplies the controlled local
append boundary; and PR3 supplies bounded read-only chain verification. Replay and startup reconciliation remain later
campaign slots.

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

## Local Append Boundary

The PR2 local implementation frames each canonical entry as its exact codec bytes followed by one LF byte. It never
adds a BOM, pretty printing, CRLF, or an alternate serialization. A final frame without LF is reported as a truncated
tail even when its bytes happen to form complete JSON. Reads preserve all original bytes and refuse malformed or
non-canonical complete frames; broader corruption and lifecycle classification belongs to the read-only PR3 verifier.

Storage is restricted to an explicit, pre-existing, absolute local data root supplied by trusted application
configuration. The implementation creates only its versioned namespace below that root. It rejects filesystem roots,
relative or missing roots, symbolic links, non-regular targets, and namespace escape. Experiment IDs are validated,
hashed with SHA-256, and represented by a fixed-length `journal-v1-<hash>.jsonl` filename; no path or filename is
accepted from an operator API. Directories use mode 0700 and files mode 0600 where POSIX permissions are available.

One process-local owner may open an experiment journal. Appends require the exact experiment identity, next sequence,
and predecessor fingerprint, and enforce 65,536 bytes per entry, 16 MiB per journal, and 4,096 complete entries. The
write and read loops stop after three consecutive no-progress results. There is no retry sleep, background queue,
unbounded cache, external store, network write, multi-process lock, startup integration, or automatic recovery in PR2.

The default sync policy calls `FileChannel.force(true)` after the complete LF-terminated frame. Optional policies call
`force(false)` or stop after all bytes have been written through the file channel. Receipts distinguish operating-system
write completion from data force and data-plus-metadata force completion and report that no application buffer is
retained. These stages report completed JDK and operating-system calls only. They do not prove disk-controller cache
persistence, power-loss survival on every filesystem, directory-entry persistence, hardware atomicity, remote-filesystem
semantics, or multi-process exclusion.

## Read-only Chain Verification

The PR3 verifier reads only the controlled hashed journal selected by experiment identity and never exposes its path.
It validates regular-file storage, declared and observed byte bounds, LF framing, entry and count bounds, strict decoding,
canonical bytes, current and predecessor fingerprints, contiguous sequence, experiment identity, nondecreasing event
time, lifecycle continuity, the existing lifecycle transition graph, and terminal-state behavior. A valid result carries
the immutable verified events, last sequence, and last fingerprint for the later replay boundary. Invalid results carry
one bounded first-failure classification and no replayable event list, so a malformed middle frame is never skipped.

Structured classifications distinguish unsupported schema, malformed or non-canonical entries, current or predecessor
fingerprint mismatch, missing/reordered/duplicated/otherwise-invalid sequence evidence, identity mismatch, illegal or
post-terminal transitions, timestamp regression, unexpected trailing data, unsafe storage, I/O failure, and exceeded
entry/count/journal bounds. The verifier preserves the original bytes; it does not truncate, repair, quarantine, append,
rename, or replace the source.

A non-LF-terminated final byte range is recoverable-tail evidence only when every complete prior frame forms a valid
chain and the tail begins like a canonical JSON object. Arbitrary trailing bytes, empty complete frames, and any invalid
LF-terminated frame remain invalid. Verification through an owning writer is serialized with append and close;
independent competing verification is reported unavailable. Recovery policy, replay, repair, and quarantine mutation are
not authorized by a verification result and remain later slots.

## Failure Semantics

Append validation occurs before the first byte write. A failure before writing therefore leaves an empty or previously
complete journal unchanged. A failure during a bounded write may leave a non-LF-terminated tail. A failure after the LF
write but before `force` may leave a complete entry without the requested synchronization boundary. A failure after
`force` may leave a complete synchronized entry even though the caller did not receive a success receipt.

Every failed append closes and permanently fails that writer instance, releases process-local ownership, and requires a
fresh verified open. Opening a writer refuses a partial tail and never truncates, repairs, replaces, or skips it. A read
that discovers invalid storage also fails its active writer rather than allowing later appends to extend suspect bytes.
Independent reads are rejected while a writer owns the journal; the owning writer may take a serialized snapshot.

Failure injection proves these call boundaries and bounded-write rejection. It does not simulate an operating-system
kernel crash, sudden power removal, torn storage-sector behavior, or an adversarial second process. Those remain explicit
not-proven boundaries. Fingerprint verification detects integrity changes but still does not authenticate an author,
prove non-repudiation, establish tamper-proof storage, or prove operating-system crash durability.
