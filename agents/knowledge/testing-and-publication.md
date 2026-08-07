# Testing and Publication

## Development philosophy

Broad scripts favor fast consumer-driven iteration:

- compile production and test classes;
- skip `test` and `check` by default;
- run focused tests explicitly when changing reusable behavior;
- verify consequential integration behavior in the consuming repository when it is in scope.

Report exact commands and distinguish compilation, unit/Spock tests, publication, and downstream verification.

## Local publication

Development artifacts use mutable version `0.5.1` in local filesystem Maven repositories. Local filesystem artifacts are expected to refresh in place rather than behave like downloaded remote-cache artifacts.

The normal `liteBuild.sh` default publishes because `build` reaches `assemble`, and bootstrap projects finalize `assemble` with `publish`. Standalone leaf `xapiPublish` also depends on Maven Publish's repository lifecycle task, `publishAllPublicationsToXapiLocalRepository`. Root `publishRequired` aggregates every leaf `xapiPublish` in `net.wti.core` or `net.wti.gradle.modern`; it is a coarse build-boundary prerequisite surface, not a dependency-minimal selection.

For all publications of one modern project, including Gradle plugin markers and intentional dummy publications, use:

```text
:xapi-gradle-settings-plugin:xapiPublish
```

For only one publication, invoke its concrete task, for example:

```text
:xapi-gradle-settings-plugin:publishMainPublicationToXapiLocalRepository
```

Generated main-build project scripts have their own explicit `xapiPublish -> publishXapiPublicationToXapiLocalRepository` dependency and do not use the same empty bootstrap lifecycle wiring.

Main projects with a `shadowJar` task publish the shadow component. Excluding that task
works only when its old output already exists; from an empty build directory publication
fails because the selected artifact is missing. Use `fullBuild.sh --shadow` for clean
bootstrap. Choosing correct semantics for daily no-shadow publication remains tracked in
`../tasks/no-shadow-publication-mode.md`.

## Focused settings-plugin verification

From `net.wti.gradle.modern`:

```text
./gradlew :xapi-gradle-settings-plugin:test
```

Add `--rerun-tasks` when proving a same-source test rerun, and use explicit test filters during iteration. Publication must follow successful focused verification when the updated local artifact is required by a consumer.
