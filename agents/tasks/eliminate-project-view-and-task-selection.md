# Task: Eliminate ProjectView and custom task-selection APIs

- Status: backlog
- Created: 2026-08-05
- Owner directive: delete these abstraction families; do not preserve them through renaming or another compatibility facade
- Scope: `net.wti.gradle.tools`, `net.wti.gradle`, `net.wti.gradle.modern`, dependent build support, and representative consumers

## Goal

Remove the `ProjectView` / `ProjectViewInternal` / `MinimalProjectView` abstraction chain and every dependency on custom `Task.whenSelected(...)` behavior so XApi can build on stock supported Gradle without the customized legacy distribution.

This is retirement work, not an invitation to modernize the universal view abstraction. Replace each use with the narrow capability it actually needs.

## Current evidence

- 87 Java/Groovy files across the three Gradle build families mention the ProjectView family.
- Six files call `whenSelected(...)`, including publication, manifest, metadata, test isolation, and Gradle-service code.
- Modern `ProjectViewInternal` still casts to `GradleInternal`, reads internal `BuildState`, uses internal instantiation/decorator types, and contains a reflective cross-version fallback.
- `MinimalProjectView` describes itself as a universal grab bag and crosses settings/project phases.
- The legacy composite publication handshake depends directly on `Task.whenSelected(...)`; that method is not implemented in this repository or provided by standard Gradle.

## Replacement rule

Do not create `NewProjectView`, a renamed service locator, or another object that exposes every Gradle phase. Split call sites by need, for example:

- immutable build/project identity and directory values;
- property lookup independent of Gradle internals;
- settings-only descriptor access;
- project-only task/publication configuration;
- ordinary Gradle lazy task dependencies and documented lifecycle callbacks;
- explicit model/index inputs that can be tested without pretending to be a Gradle project.

Prefer public Gradle APIs. Any remaining internal API must be isolated, justified, version-tested, and have an explicit removal task.

## Suggested phases

1. Inventory every nonstandard/custom Gradle API and classify ProjectView consumers by the narrow capability used.
2. Remove `whenSelected(...)` call sites using normal task registration/dependencies and focused task-graph tests.
3. Cut the legacy schema/require/publish graph away from `ProjectView`; retire unused plugins and published artifacts as consumers permit.
4. Replace the modern settings-plugin view layer with explicit settings/model inputs, preserving focused schema/index/generation behavior.
5. Remove the ProjectView interfaces/implementations, cross-version reflection, and custom Gradle distribution from bootstrap.

## Completion criteria

- Repository searches find no `Task.whenSelected`, `ProjectView`, `ProjectViewInternal`, or `MinimalProjectView` production dependency.
- XApi builds and publishes on an unmodified supported Gradle distribution.
- Main `buildSrc` and representative consumers no longer resolve the retired legacy artifacts.
- Focused publication, settings/index generation, and consumer tests cover the replacement seams.

This is a broad cross-build migration. Before implementation, present a staged plan, dependency inventory, risks, and exact first-phase files.
