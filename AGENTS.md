# XApi Agent Guide

## Start here

XApi is a large, partially migrated Java repository. Code being present does not imply that it is current, independently buildable, or safe to modernize. Before editing:

1. Read `agents/README.md` and follow its task/path routing.
2. Read the nearest nested `AGENTS.md` for every build family in scope.
3. Check `agents/tasks/` for an existing note about the requested work.
4. Preserve unrelated staged, unstaged, and untracked work. Do not stage, unstage, commit, or otherwise update Git metadata unless explicitly asked.
5. Warn the user before broad refactors, cross-build migrations, large generated diffs, or changes whose risk/file count grows materially beyond the accepted scope.

## Build topology

The supported lifecycle classification is:

- Routine bootstrap: `net.wti.core` -> `net.wti.gradle.modern` -> the main schema-generated build.
- Legacy but still consumed: `net.wti.gradle.tools` and `net.wti.gradle`. The main `buildSrc` loads their locally published artifacts even though `liteBuild.sh` does not rebuild them.
- Transitional: `net.wti.gradle.modern/migration`.
- Deprecated implementation: `net.wti.gradle.tools/deprecated`; it is still a main `buildSrc` dependency, so do not delete it casually.

Clean bootstrap uses the private seed archive created by `bootstrap/create-bundle.sh`.
It supplies ignored customized Gradle/GWT and sample inputs while `fullBuild.sh --shadow`
rebuilds `repo/` in dependency order. Java 17 and Java 8 are both required, plus network
access for ordinary public dependencies. Native Windows execution is not yet verified.
See `bootstrap/README.md`, `agents/knowledge/build-bootstrap.md`, and the related task
notes before changing bootstrap behavior.

The accepted migration direction is to keep the real Kukunochi and `/opt/wti-ui` dependency closure on stock supported toolchains and isolate fork-dependent code under `net.wti.legacy`. Desktop and Android take priority; preserving working GWT modules during that split is not required. See `agents/decisions/modern-core-and-legacy-boundary.md`.

## Primary workflows

- `./liteBuild.sh`: routine fast bootstrap/publication. Builds core, modern Gradle support, then the main build. Its default compiles test classes but skips `test`, `check`, and Javadoc.
- `./liteBuild.sh --fast` or `./liteBuild.sh -f`: skip the core and modern prerequisite rebuilds and run only the main build. Use only when the needed `0.5.1` artifacts are already current in `repo/`.
- `./fullBuild.sh --shadow`: complete clean-bootstrap path. It rebuilds both legacy Gradle
  families under Java 8 and then builds/publishes the main tree with required shadow JARs.
- `./fullBuild.sh`: faster no-shadow broad path for an already-built workspace; an empty
  workspace cannot publish shadow-component projects in this mode.
- `./fullBuild.ps1 -Shadow`: native Windows equivalent; `-Fast` skips prerequisite build
  families when their local publications are already current.
- `./quickBuild.sh`: main-build-only compilation with tests/check/Javadoc excluded; it does not request the explicit publication lifecycle.
- Focused Gradle commands should be run from the build that owns the target project, not assumed to work from the repository root.

In `net.wti.core` and `net.wti.gradle.modern`, each leaf `xapiPublish` reaches Maven Publish's `publishAllPublicationsToXapiLocalRepository`; root `publishRequired` aggregates every leaf `xapiPublish` in that build family. This is intentionally coarse and includes plugin markers/dummy publications targeting `xapiLocal`.

Local Maven filesystem artifacts are intentionally mutable at version `0.5.1` and are resolved directly from local filesystem repositories. Do not assume remote-cache semantics. See `agents/knowledge/testing-and-publication.md`.

## Generated main build

The root `schema.xapi`, source layout, and handwritten fragments under paths such as `src/gradle/<key>/` are generator inputs. The XApi settings plugin produces and updates tracked `*.gradle` project scripts; generated changes are intentionally reviewed and committed.

- Do not hand-edit content between `// GenStart ...XapiSettingsPlugin` and `// GenEnd ...`.
- Put custom build logic in supported fragment files such as `plugins`, `body.start`, `body.end`, or `buildscript`.
- Expect normal Gradle invocations to regenerate scripts and `build/xindex`.
- Review generated diffs for unintended topology, dependency, coordinate, or path changes.
- Coordinate-index dependency keys use the reversible `SchemaPathCodec` disk encoding when a logical name is not Windows-portable. Do not decode or reuse those disk names as Gradle paths or Maven coordinates.
- A real root schema version becomes the default for every Gradle project and is emitted into generated normal/source scripts; null, blank, and `UNKNOWN` versions are omitted.

See `agents/knowledge/generated-builds.md` before changing `schema.xapi`, generator logic, fragments, or generated scripts.

## Verification philosophy

The broad scripts optimize for fast consumer-driven development, not exhaustive CI: they compile tests but generally do not run them. For code changes, select focused tests from the owning build and report exactly what ran. Consumer verification is often the most meaningful integration test, but it does not replace focused regression coverage for reusable build tooling.

## Knowledge and task maintenance

- Update verified knowledge when code work makes an existing statement obviously stale.
- Discuss non-obvious deviations, new policy, new architectural claims, or speculative additions before making them normative.
- Offer guidance improvements when recurring confusion or mistakes are discovered, even if documentation was not explicitly requested.
- One-off investigations and deferred work live in `agents/tasks/`; repeatable workflows belong in `.agents/skills/` if introduced later.
- Complete substantial work with the knowledge-distillation process in `agents/knowledge-distillation.md`.

## Cross-window handoffs

When independent agent windows must exchange work, use the two-file outbox protocol under `/tmp/codex-handoffs/<exchange-id>/`. Each session writes only its own outbox and treats peer outboxes as read-only. Outboxes must remain self-contained with scope, context, decisions, files, verification, questions, and a latest peer message.

## External consumers

- `/opt/wti` is the original XApi consumer and remains an important compatibility boundary.
- Kukunochi consumes selected XApi artifacts and the settings plugin from `repo/`; keep work originating there narrowly tied to demonstrated consumption.
- `/opt/wti-ui` consumes the modern settings generator and locally published artifacts.
- `/opt/collide` is a low-priority historical consumer that may no longer compile but could be revived.

Do not edit another repository unless the user explicitly places it in scope.
