# XApi Settings Plugin Guide

`xapi-settings` parses `schema.xapi`, builds/indexes the logical graph, decides liveness, creates Gradle projects, and generates tracked project scripts for the main build and consumers.

## Sources and references

- Implementation: `src/main/java/net/wti/gradle/settings/` and `.../plugin/XapiSettingsPlugin.java`.
- Tests: `src/test/groovy/net/wti/gradle/`.
- `src/main/resources/SCHEMA.md`: detailed parser/DSL description, but not guaranteed current.
- `src/main/resources/README.md`: conceptual model/semantics.
- `src/main/resources/REQUIREMENTS.md`: unfinished aspirational Schema V2 checklist, not implemented truth.
- Repository generation rules: `../../agents/knowledge/generated-builds.md`.

## Working rules

- Keep fixes narrow and add focused generated-output or TestKit/Spock coverage.
- Do not infer behavior only from the design docs; compare with parser/index/generator code and tests.
- Preserve null/blank/`UNKNOWN` metadata behavior unless the task explicitly changes it.
- A declared schema version defaults every Gradle project through the settings lifecycle and is also emitted into generated normal/source scripts. Preserve root/implicit/manual/generated coverage and UNKNOWN omission tests.
- Keep the Java 8 toolchain declaration on both normal generated projects and synthetic
  `-sources` siblings; Gradle variant matching otherwise advertises source siblings as the
  Java 17 launcher runtime.
- Logical dependency keys under `build/xindex/coord` must pass through `SchemaPathCodec` before becoming filenames. The codec leaves portable names readable, reversibly escapes unsafe/reserved Windows names, and removes the matching legacy single-segment file during migration. Do not apply the disk encoding to Gradle paths or published coordinates.
- Avoid format churn in tracked generated scripts.
- Run `./gradlew :xapi-gradle-settings-plugin:test` from the modern build.
- Use `:xapi-gradle-settings-plugin:xapiPublish` to publish the main artifact, intentional `pluginMaven`/dummy artifact, and `xapi-settings` marker to `xapiLocal`. Use `publishMainPublicationToXapiLocalRepository` only when deliberately refreshing the main publication alone.
- Consumer-affecting changes should be republished as `net.wti.gradle.modern:xapi-gradle-settings-plugin:0.5.1` and verified downstream when that repository is in scope.
