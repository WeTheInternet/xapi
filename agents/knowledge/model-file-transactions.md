# Opt-in Linux model transaction engine

`FileModelTransactionStore` is an explicitly constructed JRE storage engine in the
existing `model` owner. It is not registered as a `ModelService` provider and does
not change server startup, HTTP endpoints, seeding, or Android synchronization.
The existing single-record file layout and serialized model bytes are retained.
No third-party database dependency or generated build topology is added.

## Scope and ownership

One engine exclusively owns a store-root lock for its lifetime. All its reads,
bounded multi-record snapshots, receipt lookups and commits share one gate.
An in-process normalized-root reservation rejects a duplicate instance before it
opens another descriptor for the lock inode. This matters on Linux: closing that
second descriptor after a Java overlap rejection can release the first process's
OS lock. The regression probes another process after the rejected duplicate open.
The adapter supplies a complete canonical model-key serialization and its derived
relative record path. The engine validates paths and durably binds canonical
identity to physical path in both directions; it does not parse model keys,
deserialize models, authenticate users, or authorize business operations.

An adapter must validate every decoder, key derivation and authorization before
calling commit. A string actor scope is a receipt namespace, never authority.
Existing files start at revision zero; must-not-exist is a distinct precondition.
Every committed transaction assigns the next monotonic store revision to all its
children. Revisions are not value hashes, so A → B → A does not revive an old
precondition. Stable operation IDs are scoped to an authenticated actor by the
adapter. The engine computes a fingerprint of the exact length-delimited request,
including protocol, source manifest, actor scope, operation ID, ordered children,
full identities, expected revisions and immutable model images.

Technical pilot caps are 64 child replacements and 4 MiB of encoded request data.
These are configurable-in-code storage bounds, not reward policy or an assertion
about a server transport limit. Deletion/tombstones, query indexes, compaction and
receipt expiry are not implemented. Receipts retain the original request images;
operators must account for that storage cost before activating the engine.

## Commit and recovery protocol

1. Under the gate, resolve an existing receipt before evaluating current record
   revisions. An identical retry returns the original receipt; a reused ID with a
   different payload conflicts. Validate every child, duplicate, path alias,
   ancestor/file collision and revision before publishing any model mutation.
2. Serialize a complete redo decision containing immutable child images, their
   conditions and identities, the next store revision and receipt. A checksum
   covers the full journal header/revision and request. No Java object decoding is
   used. The journal lives under reserved `.xapi-transactions/pending`.
3. Publish that file using `AtomicModelFile`: unique sibling, complete write,
   file force/close, atomic replacement and directory force. Failure before
   publication has no child effects. Once replacement is attempted, ambiguity is
   reported as `RecoveryRequired`; publication followed by failed directory force
   is never called a clean rejection.
4. Materialize every child, its checksummed path/identity revision bindings, the
   checksummed global sequence, and the original receipt using the same atomic
   file helper. Readers remain gated throughout.
5. Remove the pending decision and force its directory before allowing a later
   transaction. A cleanup failure closes the gate until recovery. This ordering
   prevents an old journal from replaying over a newer committed transaction.

Startup acquires ownership and rolls forward any pending decision before serving
reads. A corrupt decision fails closed and remains available for diagnosis.
Recovery may be repeated after interruption. Post-commit IO failures and unexpected
Errors leave reads closed; recovery, not a new operation ID, resolves the result.
Abandoned unpublished temporary siblings are never promoted into transactions.
Receipts are retained indefinitely in this first engine; no cleanup/GC protocol
for receipts is claimed.

Model bytes are opaque to the engine. Storage absence, unreadable files, corrupt
revision metadata and missing protected files are distinct outcomes. Existing
permissions are preserved by the single-record helper. Symbolic links and reserved
internal paths are rejected. The store is intended for a trusted Linux filesystem
with one cooperating process; OS actors that bypass its ownership lock and mutate
files directly are outside this contract.

## Required integration before claiming application atomicity

- Acquire ownership before recovery and seeding. Applications that launch an
  unguarded seed subprocess must resolve that ownership gap rather than taking a
  lock only afterward. Do not mix guarded and unguarded providers for one root.
- Explicitly attach every participating `ModelServiceJre` instance before its first
  filesystem operation. The opt-in provider now gates actual asynchronous read and
  query bodies and rejects unconditional writes/deletes to protected keys. See
  [model-provider-gate.md](model-provider-gate.md). `isProtected` alone does not
  enforce rules against a bypassing provider.
- Use `ModelServiceWithQueryValidation` for application directory validation and
  actual query enumeration to share one scope. The provider filters reserved files;
  HTTP callers still need their authorization/path validation in that scope. A
  bounded snapshot differs from separate requests made across a commit.
- Add the narrow authenticated transport and structured conflict/recovery results.
  Authorize all children and receipt disclosure; no payload actor field confers
  parent or ledger privileges. Preserve source manifests for old receipt lookup.
- Integrate completion/history as one conditional bundle before describing that
  application's two-write partial-completion defect as fixed.

The engine tests exercise real temporary files, faults and process termination.
Provider integration tests additionally exercise real serialization, protected
legacy-write rejection after restart, scoped validation and asynchronous gate
barriers. They do not establish HTTP authorization, application startup, device
behavior or physical power-loss/controller guarantees. I21 and all outbox/migration
cases remain application work, not passing claims.
