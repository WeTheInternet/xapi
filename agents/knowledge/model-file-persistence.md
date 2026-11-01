# Linux model file replacement

`model:jre`'s `ModelServiceJre` serializes a complete UTF-8 model before replacing
its existing file. `AtomicModelFile` writes a unique temporary sibling, forces and
closes it, performs `ATOMIC_MOVE` replacement, then forces the containing directory.
Directory ancestors are also forced before replacement, including on retries after
partially completed directory creation. No copy/delete fallback is allowed.

The tested target is Linux with a provider supporting POSIX attributes, directory
`FileChannel.force(true)` and same-filesystem atomic replacement. Unsupported
providers fail instead of receiving a weaker success guarantee. Existing regular
files retain POSIX owner, group and permission bits; new files are private (0600).
Extended ACLs, xattrs, hard-link identity and other operating systems are not covered.
Symlink destinations are rejected; ancestor resolution retains the existing path
layout and is not a hostile-filesystem sandbox. Keep the store privately owned.

An ordinary write failure before replacement leaves the previous model intact.
`ModelPersistenceException` means replacement may have happened or directory
durability is unconfirmed: callers must not treat it as proof of rejection.
There is no operation receipt or conditional revision in this single-record API.

Names matching `.xapi-write-*.tmp` are reserved, cannot be persisted/loaded as models,
and are excluded from every built-in query. Existing models using this reserved
pattern require an explicit rename/migration before using this provider. Owned
temporary files are removed on ordinary failures. Crash remnants are retained but
query-invisible: automatic removal of another process's staging file would be unsafe
without store ownership. The ordinary unattached provider does not enforce that
ownership; explicit `attachTransactionStore` adds the gate described in
[model-provider-gate.md](model-provider-gate.md).

Generated IDs retain the existing name/count convention without creating visible
empty placeholder models. Canonical parent paths make reservations identical through root aliases; reservations
only coordinate threads in this JVM. A clearly
rejected new ID is cleared on its model so a retry cannot overwrite a reused provisional
ID; uncertain outcomes retain their identity/reservation until resolved. This is not
a cross-process allocation guarantee in unattached mode. Unprotected explicit-key
writes remain last-writer-wins; attached providers reject unconditional changes to
transaction-protected keys.

Loads report `ModelNotFoundException` only for actual absent-file reads. Corruption,
unreadable files and other storage failures remain errors, preserving existing bytes
instead of inviting missing-model reseeding. This service no longer logs or attaches its raw corrupt file buffer. Lower-level
decoder diagnostics still propagate and may contain input; this is not a complete
payload-redaction guarantee.

Focused regressions are `AtomicModelFileTest` and `ModelFilePersistenceTest` under
`model/src/jreTest`. They exercise real temporary files, injected write/force/rename
failures, concurrent readers, process halts around rename, permissions, serialization,
absence versus corruption and query filtering. A process halt is not a physical
power-loss/storage-controller durability test. Atomic multi-record transactions,
single-writer ownership, receipts and consistent composed reads require a separate
explicit capability; successive single-record replacements do not provide them.
