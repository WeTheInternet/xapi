# Task: Reconcile settings-schema documentation with implementation

- Status: ready
- Created: 2026-08-05
- Scope: `net.wti.gradle.modern/settings-plugin` schema documentation, implementation cross-references, and executable examples
- Related: `deprecate-schema-location.md`

## Problem

The three settings-schema documents preserve valuable design work, but they serve different purposes without a sufficiently sharp boundary:

- `src/main/resources/README.md` declares intended, implementation-agnostic behavior;
- `src/main/resources/REQUIREMENTS.md` is an entirely unchecked Schema V2 backlog;
- `src/main/resources/SCHEMA.md` claims to describe behavior actually consumed by the parser, then ends with a speculative meta-schema.

Agents and maintainers can therefore mistake a design target for current behavior or assume a parser feature is complete because its syntax is accepted.

## Verified gaps to reconcile

- `applyTemplate`, `templates`, and `shortenPaths` are accepted but are placeholders/no-ops in `XapiSchemaParser`.
- `<preload>` is parsed, but `loadExternals` contains `TODO: actually handle preloads`.
- `schemaLocation` is described as a generated-script destination even though parsing a real schema later replaces it with the input schema path; retire it under the related task.
- README's virtual-project model is stronger than current realization: `XapiSettingsPlugin` includes a non-multiplatform schema project without checking `project.isVirtual()`.
- Schema V2's arbitrary test-source-set model is not current behavior; generated scripts explicitly create and clear `main`/selected and `test` `SourceSet` objects.
- Schema V2 says the reader only interprets canonical numeric liveness. Current `SchemaIndexReader.hasEntries` also treats its computed `explicitDependencies` result as live and recursively evaluates `in/` and `out/` links for liveness value `1`.
- `SchemaIndexReader.IndexResult` currently defines explicit dependencies with `!external.isEmpty() && !project.isEmpty()`, which deserves a focused correctness test before documenting it as a contract.
- `SCHEMA.md` says it was derived from `test-project/schema.xapi`, but that path does not exist in the current repository.
- `README.md` contains a stray patch hunk marker (`@ -260,3 +520,229 @@`) in the Modules section.
- The experimental `xapi-parser` block in `SCHEMA.md` should be clearly separated from the current parser contract and compared with `META-INF/xapi/xapi-schema.xapi` before either is called canonical.

## Proposed work

1. Add a short status banner and audience statement to each document: **current contract**, **target design**, or **experiment**.
2. Build a compact feature matrix linking every schema feature to its parser/model implementation, focused tests, and status (`implemented`, `partial`, `accepted-no-op`, `proposed`, `deprecated`).
3. Correct only verified current-state claims in `SCHEMA.md`; move opinions and future behavior to README/REQUIREMENTS or dedicated design notes.
4. Preserve the valuable V2 ideas, but group unchecked requirements into independently testable milestones rather than implying one wholesale rewrite.
5. Turn representative examples into tests before checking off requirements. Prioritize virtual realization, standalone coordinate selection, liveness/index-reader behavior, platform replacement, test source sets, and source publication.
6. Remove the patch marker and stale source-file reference.
7. Decide whether the speculative meta-schema should become a tested input, a design note, or be superseded by the existing `META-INF/xapi/xapi-schema.xapi` resource.

## Completion criteria

- A new agent can distinguish implemented behavior from target behavior without reading all three documents.
- Every documented current contract has a code or test pointer.
- Accepted-but-inert syntax is visibly labeled and does not silently promise behavior.
- Selected V2 work is represented by focused tasks/tests; unresolved ideas remain explicitly aspirational.
