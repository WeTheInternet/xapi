# Task: Make no-shadow publication correct from an empty workspace

- Status: decision-needed
- Created: 2026-08-05
- Scope: main generated publication wiring, `liteBuild.sh`, `fullBuild.sh`

## Problem

Daily scripts exclude `shadowJar` for speed. Generated publication logic selects the
shadow component whenever the task exists, so `xapiPublish` still expects that task's
output. A dirty workspace may pass by publishing an old file; an empty workspace fails,
for example at `:dev:uber`, with `InvalidMavenPublicationException` because
`build/libs/uber-0.5.1.jar` does not exist.

`fullBuild.sh --shadow` is the verified complete-bootstrap path. Do not use
`Task.whenSelected(...)` or task-graph inspection to restore the old behavior; those are
explicit retirement targets.

## Decision required

Choose the meaning of a no-shadow publish for projects whose publication is intentionally
an uber/shadow artifact:

1. Skip only those publications while still publishing ordinary projects.
2. Publish the slim Java component under a distinct artifact/variant.
3. Stop requesting publication for those projects in fast mode.

The result must work from empty build directories, never silently publish stale output,
and use ordinary lazy Gradle task/publication wiring.
