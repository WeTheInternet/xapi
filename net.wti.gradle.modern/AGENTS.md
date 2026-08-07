# Modern Gradle Build Guide

This is the routine Gradle 8.11.1 build for modern XApi build support. It publishes `net.wti.gradle.modern:*:0.5.1` and is the second stage of `liteBuild.sh`.

- Use this directory's wrapper and Java 8 toolchain.
- Projects include core/test support, schema parser, settings plugin, font/atlas plugins, and a temporary migration bridge.
- The migration bridge compiles against five legacy Gradle libraries extracted from the
  verified `.xapi-bootstrap/gradle/gradle-5.1-x-24.zip`; do not restore machine-absolute
  `/opt/gradle` dependencies.
- Prefer focused project tests, then use the leaf `xapiPublish` lifecycle to publish every publication targeting `xapiLocal`; concrete publication tasks remain useful when intentionally publishing only one publication.
- The modern `MinimalProjectView` / `ProjectViewInternal` layer is also an explicit retirement target. Do not deepen it; new work should pass narrow settings/model capabilities. See `../agents/tasks/eliminate-project-view-and-task-selection.md`.
- Do not fold legacy `net.wti.gradle` or `net.wti.gradle.tools` cleanup into modern-plugin work without a migration task: the main build still consumes their artifacts.
- Read `settings-plugin/AGENTS.md` before changing schema parsing, indexing, liveness, project generation, or generated build scripts.
