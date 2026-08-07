# Task: Reconcile schema index dependency categories and decoding

- Status: ready
- Created: 2026-08-05
- Priority: after `xapiPublish`; combine with index work when practical
- Scope: modern settings-plugin `SchemaIndexReader` and coordinate-index format semantics

## Finding

The Windows filename audit found unfinished reader/writer category symmetry that was not required to unblock portable filenames:

- `SchemaIndexerImpl` writes project dependencies under a singular `project/` directory, while `SchemaIndexReader.readIndex(...)` switches on `projects`.
- External dependencies are written as files directly under the coordinate version directory, so the reader currently places them in its `unknown` list rather than its `external` list.
- `IndexResult` presently uses these private file lists only to derive coarse liveness/explicit-dependency state; there are no public typed accessors, and no current caller parses dependency identity from `File.getName()`.
- Encoded dependency filenames must be decoded through `SchemaPathCodec` if a future typed reader exposes logical keys. Disk names must never leak into Gradle dependency declarations or Maven coordinates.

## Desired outcome

Define the intended on-disk category topology, migrate writer and reader together, preserve existing liveness behavior, and add direct round-trip tests for project/internal/external/unknown entries. Decide explicitly whether compatibility with pre-migration local indexes is required or whether ignored `build/xindex` is always regenerated.

Do not fold this into the `xapiPublish` lifecycle repair unless concrete task wiring depends on typed index reads.
