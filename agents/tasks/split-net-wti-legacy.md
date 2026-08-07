# Task: Isolate fork-dependent code in `net.wti.legacy`

- Status: planned
- Created: 2026-08-05
- Scope: main schema topology, active component closure, GWT-specific modules, legacy Gradle/GWT dependencies, consumer verification

## Goal

Move code that still requires the customized GWT or Gradle forks behind a `net.wti.legacy` build/module boundary. Keep the actively used core—such as XApi language, model core, collections, utilities, and the actual transitive closure consumed by Kukunochi and `/opt/wti-ui`—buildable with stock supported Gradle and modern non-fork toolchains.

## Accepted tradeoff

Continuous GWT support is not a migration requirement. `xapi-inject` and GWT-facing `xapi-model` modules are expected to fail without the customized compiler and may remain legacy-only or unsupported until magic-method injection is reimplemented with Java compiler APIs. Desktop and Android are the active platform priorities.

## Required investigation

1. Derive the exact active artifact/module closure from Kukunochi and `/opt/wti-ui`; do not rely on directory-name guesses.
2. Classify mixed components such as model into stock-toolchain core and fork-dependent GWT portions.
3. Inventory main `buildSrc`, schema, generated-script, and publication edges crossing the proposed boundary.
4. Design coordinates and compatibility handling before moving directories or generated projects.
5. Define independent modern and legacy build/publication commands; the portable bootstrap bundle may supply the legacy toolchains.

## Completion criteria

- Active Kukunochi and `/opt/wti-ui` XApi dependencies build and publish on stock Gradle without hacked GWT/compiler jars.
- Fork-dependent modules live behind the explicit legacy boundary and cannot leak onto the active build classpath accidentally.
- Desktop and Android consumer verification passes.
- Suspended GWT artifacts are documented rather than silently appearing supported.

This is a broad topology migration and requires a reviewed staged plan before implementation.
