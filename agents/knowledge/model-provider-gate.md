# Explicit ModelServiceJre transaction ownership

`ModelServiceJre.attachTransactionStore(store)` explicitly attaches an already
owned/recovered `FileModelTransactionStore` before the provider's first filesystem
operation. Creating/registering model types is not filesystem IO. A fresh provider
binds directly to the store directory; it does not guess an ambient directory.
An already configured root must match canonically. Failed attachment after prior IO
does not change the provider's original root or identity.

The caller owns the store lifetime. Applications using global `X_Model` must attach
that actual provider; attaching only a separate instance leaves global callers
unguarded. A same-root `setRootDir` remains idempotent for embedded seed composition.
Another root or store cannot replace the attachment, even after close. Closing the
owner leaves pending provider calls failing closed; restart the application rather
than trying to reactivate its global provider against another ownership lifetime.
Unattached providers retain their ordinary single-record mode.

## Gate and callbacks

Actual asynchronous load/query runnable bodies acquire the engine gate. Enumeration,
file reads and deserialization finish inside it; successful/error callbacks run
after it is released. The provider snapshots attached-mode query and key identities
before dispatch. Deferred queries also capture their inputs in unattached legacy
mode, so changing the original namespace/ancestor/cursor cannot redirect later IO.
Original attached full key/ancestor chains are validated and copied without
letting the wire codec normalize an inconsistent child's namespace into its parent's
owner. Complete IDs/kinds, namespace agreement, depth/cycles and every custom key type
are retained; query namespace must match its captured ancestor. A load of a missing protected file is a storage error, not the
ordinary missing-model result that would invite reseeding.

`ModelServiceWithQueryValidation` is an optional thin capability in the existing
model owner. Its `In1<ModelQuery<M>>` validator receives the exact captured query
used for enumeration, immediately before it under the same read scope. This strict
capability rejects an unattached provider; ordinary manifest queries remain supported
without delegating through the strict overload. The caller's
mutable original is never supplied in attached mode. Validators must not change the
target, invoke asynchronous IO or wait for a
callback. Engine commit, legacy-write, recover and close attempts reentered from a
scope are rejected; consumer callbacks outside the scope can initiate new IO.
The provider does not catch a consumer callback exception and then report a second
storage failure.

Legacy persist/delete checks and their actual filesystem mutations share the
engine's `legacyWriteScope`. A protected key rejects unconditional mutation,
including after restart. Unprotected legacy writes are still single-record writes
without a transaction receipt. All typed, manifest and untyped query paths exclude
reserved transaction metadata and temporary files. Guarded enumeration propagates
storage/symlink errors rather than converting them into empty results.
The reserved metadata directory is only `.xapi-transactions` immediately under the
store root; the same basename remains a valid model file in a type bucket.

Before enumerating an owned query, protected path bindings are reconciled under the
same gate. A missing protected record, bucket or namespace is a storage failure.
Typed queries check their exact ancestor bucket; untyped queries check the existing
flat namespace/type shape, not arbitrary descendants. The entire selected bucket
is validated before cursor/page filtering, so an earlier missing record does not
become invisible on later pages. A missing record in another scope is not an error
for this query. The initial implementation scans all protected binding metadata:
O(total protected records) metadata work per query plus in-scope binding/presence
checks, without loading all model bodies or introducing an index. Unreadable or
corrupt metadata whose scope cannot be determined fails closed; no compaction or
query-index performance guarantee is claimed.

Untyped pagination uses a versioned opaque `(type, record)` cursor for the first
unreturned record. Iteration skips earlier types, applies the record boundary only
within the cursor's type, and includes every later type without that filename filter.
This avoids both dropping later kinds and repeating a full earlier page. Typed
queries retain their existing filename cursors. Malformed/version-unknown untyped
cursors and historical bare filenames fail explicitly; callers must deliberately
restart without a cursor instead of silently restarting or guessing the lost type.
Pages are ordered continuations, not a snapshot spanning separate requests.
Attached legacy persist rejects serialized images exceeding the engine's 4 MiB
record bound before any write; query decoding checks the file size before allocating
its input buffer. Detached legacy mode keeps its existing size behavior. A clear
oversize rejection releases a provisional generated ID and preserves previous bytes.

`ModelFileQueries` owns synchronous file enumeration; `ModelServiceJre` owns provider
composition, serialization and scheduling. The engine owns transaction decisions,
recovery and shared scopes. No monitor is transferred across asynchronous callbacks.

## Remaining application integration

Attached providers deserialize key-only references as key-bearing stubs exactly as
encoded. They do not schedule hidden reference loads after the outer snapshot is
returned. Child contexts inherit that policy; inline model/list values remain inline.
Unattached/default providers retain legacy deferred hydration. Composed hydrated
views belong in explicit caller-owned reads, not an implicit unbounded graph load.

This capability does not automatically activate servers or create an HTTP bundle
endpoint. Application startup must own/attach the global provider before in-process
seeding, pass query validation through the capability, and close the store at
shutdown. External seed programs that ignore the gate cannot run concurrently with
the owner. Business command authorization, atomic completion/history bundles,
offline outboxes and migration remain separate work.

`ModelServiceTransactionGateTest` uses real provider serialization and temporary
stores, controlled asynchronous runnable dispatch, transaction/read/write barriers,
parent/child process ownership and crash/recovery. These tests are not a claim of
device, HTTP authorization, household-store migration or physical power-loss proof.
