# Schema-Generated Build Rules

## Sources of truth

The main build is derived from:

1. Root `schema.xapi`.
2. Filesystem sources/resources and dependency liveness.
3. Handwritten Gradle fragments, normally under `src/gradle/<key>/`.
4. `XapiSettingsPlugin` and its parser/index implementation.

Generated `*.gradle` files are tracked deliberately so generator/configuration changes produce reviewable diffs. `build/xindex` and generator comparison files under `build/` are local outputs.

## Editing rules

- Do not directly edit generator-owned blocks marked by `GenStart`/`GenEnd`.
- Put intentional customization in supported fragments: `imports`, `plugins`, `repositories`, `buildscript.start`, `buildscript`, `buildscript.end`, `body.start`, `body`, or `body.end` as supported by the generator.
- Normal settings evaluation regenerates live project build scripts.
- If the generator detects a manual change relative to its last output, use the fragment model; do not blindly force regeneration over user work.
- When regeneration is explicitly required, review every generated diff and stage it with the source schema/generator change.

## Known constraints

- A declared schema version is installed as the default for every Gradle project before evaluation and is also emitted into generated normal/source scripts. Null, blank, and `UNKNOWN` versions are omitted.
- Generated normal projects and synthetic `-sources` siblings both advertise the Java 8
  toolchain. Source-only projects still participate in Gradle JVM variant matching even
  though they compile no Java; omitting this makes Java 8 consumers reject them when the
  build itself runs on Java 17.
- Dependency keys below `build/xindex/coord` are logical schema values, not Gradle paths or Maven-coordinate mutations. Unsafe or Windows-reserved filename components are reversibly stored as `~` followed by lowercase UTF-8 hex; matching legacy colon-named files are removed when rewritten. See `../tasks/windows-illegal-generated-paths.md`.
- `schemaLocation` is parsed but does not provide the documented generated-schema behavior. See `../tasks/deprecate-schema-location.md`.
- The settings-plugin reference docs mix implemented semantics with work-in-progress design. See `../tasks/reconcile-settings-schema-docs.md`.
