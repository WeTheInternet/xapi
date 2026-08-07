# Legacy Gradle Plugin Build Guide

This build is legacy but still consumed. It uses the customized Gradle 5.1 distribution and publishes loader/plugin/API artifacts required by the main `buildSrc`.

- Routine `liteBuild.sh` does not rebuild it; `fullBuild.sh` and `toolBuild.sh --all` do.
- Its wrapper resolves `../.xapi-bootstrap/gradle/gradle-5.1-x-24.zip` through a checked
  relative URL and must be launched with Java 8. `fullBuild.sh` handles that split when
  `XAPI_JAVA8_HOME` or a known Java 8 installation is available.
- It depends on `net.wti.gradle.tools` artifacts and applies legacy XApi schema/publish plugins.
- Do not remove or rewrite it solely because modern equivalents exist; first prove the main build and consumers no longer load the relevant artifacts/classes.
- Do not expand the ProjectView family or custom `Task.whenSelected` usage; both are explicit deletion targets in `../agents/tasks/eliminate-project-view-and-task-selection.md`.
- Plan retirement through `../agents/tasks/retire-custom-gradle.md` and clean-bootstrap verification.
