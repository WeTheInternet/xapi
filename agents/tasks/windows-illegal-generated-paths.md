# Task: Remove Windows-illegal colon-containing generated paths

- Status: done
- Created: 2026-08-05
- Completed: 2026-08-05
- Scope: settings-plugin coordinate-index dependency filenames

## Desired outcome

Generate portable filesystem names without changing logical Gradle paths or published coordinates.

## Findings

- The active Windows-illegal output was below `build/xindex/coord/<group>/<name>/<version>`: external dependency coordinates were leaf filenames such as `junit:junit:4.13`, and project dependency keys were children such as `project/:ui:producer`.
- The `build/xindex/path` graph does not put colons in a filename. Its PPM helpers split logical colons into directory levels, while generated Gradle project paths and Maven coordinates remain logical strings.
- The pre-fix main index contained 367 colon-named dependency files: 214 external leaves plus project dependency leaves.
- `QualifiedModule.mangleProjectPath` still has a theoretical underscore collision (`:a:b` versus `:a_b`). It was not changed because it does not produce the Windows-illegal names and replacing that index topology is a separate compatibility decision.

## Implemented contract

- `SchemaPathCodec` leaves Windows-portable names readable.
- Unsafe, reserved-device, trailing-dot/space, or escape-prefixed names become `~` plus lowercase UTF-8 hex.
- Decoding is reversible, and the reserved prefix prevents collisions with literal portable names.
- Project, internal, and external dependency-key writer branches all use the codec. When an encoded key is written, the matching legacy single-segment file is removed to prevent duplicate cached entries on Unix hosts.
- Logical Gradle paths, dependency declarations, generated script contents, and published coordinates are unchanged.

## Verification

- Focused codec spec covers reversibility, forbidden Windows characters, reserved devices, Unicode, malformed input, and delimiter/prefix collision cases.
- Existing nested project/external-dependency integration coverage now asserts that every generated coordinate-index component is portable and that encoded names decode to `:ui:producer` and `junit:junit:4.13`.
- Full `:xapi-gradle-settings-plugin:test`: 32 tests, zero failures/errors/skips.
- Root `./gradlew help`: successful after publication; main index audit found 367 encoded dependency files, zero colon-named files, and zero Windows-illegal components.
- Published `net.wti.gradle.modern:xapi-gradle-settings-plugin:0.5.1` to `repo/`.
- No native Windows build was available in this session. The waiting Windows consumer/bootstrap run remains the platform-level integration verification.

## Sequence

Repair `xapiPublish` next. A portable clean-machine bootstrap artifact remains separate work in `clean-bootstrap.md`.
