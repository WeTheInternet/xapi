# Task: Retire the customized legacy Gradle distribution

- Status: backlog
- Created: 2026-08-05
- Scope: `net.wti.gradle.tools`, `net.wti.gradle`, their consumers, and `/opt/gradle/gradle-5.1-x-24.zip`

## Desired outcome

Move remaining required behavior to supported Gradle/tooling and remove the custom Gradle distribution from bootstrap.

## Constraints

The legacy families are still consumed by main `buildSrc`; directory names such as `deprecated` do not make their artifacts removable. Inventory plugin IDs, classes, and runtime behavior actually loaded by the main build and external consumers before migrating or deleting anything.

Confirmed coupling includes `Task.whenSelected(...)` and the broad `ProjectView` abstraction family. These must be deleted rather than ported or renamed. The phased inventory and removal criteria live in `eliminate-project-view-and-task-selection.md`.

## Completion evidence

A clean bootstrap and representative consumers succeed without the custom distribution or its published artifacts.
