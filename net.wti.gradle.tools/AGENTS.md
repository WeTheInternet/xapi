# Legacy Gradle Tools Guide

This build is legacy but still consumed. It uses the customized Gradle 5.1 distribution and publishes artifacts loaded by `net.wti.gradle`, main `buildSrc`, and historical consumers.

- Do not treat the `deprecated` project as safely deletable; main `buildSrc` currently depends on its published artifact.
- Routine `liteBuild.sh` assumes these artifacts are already present and does not rebuild them.
- `fullBuild.sh` and `toolBuild.sh --all` include this build.
- Its wrapper resolves `../.xapi-bootstrap/gradle/gradle-5.1-x-24.zip` through a checked
  relative URL and must be launched with Java 8. `fullBuild.sh` handles that split when
  `XAPI_JAVA8_HOME` or a known Java 8 installation is available.
- Any migration must inventory actual plugin IDs/classes loaded at runtime and coordinate with `../agents/tasks/retire-custom-gradle.md`.
- Do not add new `ProjectView`, `ProjectViewInternal`, `MinimalProjectView`, `TaskSpy`, or `whenSelected` dependencies. They are explicit retirement targets; see `../agents/tasks/eliminate-project-view-and-task-selection.md`.
- Avoid broad modernization under a consumer-specific task.
