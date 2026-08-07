# Task: Apply schema version to every Gradle project

- Status: done
- Created: 2026-08-05
- Completed: 2026-08-05
- Scope: modern settings plugin, focused regression tests, local `0.5.1` publication

## Outcome

When `schema.xapi` declares a real version, `XapiSettingsPlugin` installs it as the version default before every Gradle project evaluates. This covers the root project, implicit aggregator descriptors, manually included projects, generated module projects, and synthetic source projects.

Generated normal/source scripts continue to contain explicit version assignments so their publication metadata and checked-in output remain self-contained. Null, blank, and `UNKNOWN` schema versions install no default and emit no assignment.

An explicit `version = ...` later in a handwritten build script can still override the settings-level default.

## Verification

- Three focused version regressions passed.
- Complete `:xapi-gradle-settings-plugin:test`: 20 tests, zero failures/errors/skips.
- `net.wti.gradle.modern:xapi-gradle-settings-plugin:0.5.1` republished to `repo/`.
- Published JAR SHA-256 changed from `c35d32ee91f5889e3c8c6efbea5455163dc610e049034e19af313012b5713677` to `f7539925d51a8c5d6b5f85ee6e2d7c6b2bc5a2509d9f748dfa2b5124eceace80`.
- Main schema regeneration updated 105 tracked generated Gradle scripts; the audited diff is exactly one `version = '0.5.1'` addition per file and no other generated change.
