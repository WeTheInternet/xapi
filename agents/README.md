# Agent Knowledge Router

This directory holds repository knowledge that is useful to agents but should not all be loaded for every task.

## Tool-facing layout

- Root and nested `AGENTS.md` files are the durable Codex instruction surface. Keep them concise; Codex composes the files from repository root down to the working directory.
- Root `CLAUDE.md` imports `AGENTS.md`, so Claude Code receives the same shared entry point without duplicating it.
- `agents/` is routed knowledge and work tracking, not an automatically loaded instruction tree. Agents should open only the files selected below.
- Reserve `.agents/skills/` for repeatable, triggerable workflows with a `SKILL.md`; one-off investigations remain in `agents/tasks/`.

## Read by task

| Work | Read first |
| --- | --- |
| Any build/bootstrap/script change | `knowledge/build-bootstrap.md`, `knowledge/testing-and-publication.md` |
| Root `schema.xapi`, generated `*.gradle`, or `build/xindex` | `knowledge/generated-builds.md` |
| `net.wti.core` | `knowledge/repository-map.md`, `../net.wti.core/AGENTS.md` |
| Modern Gradle/settings/schema work | `knowledge/repository-map.md`, `../net.wti.gradle.modern/AGENTS.md` |
| `XapiSettingsPlugin` | `../net.wti.gradle.modern/settings-plugin/AGENTS.md` and its in-module reference docs |
| Legacy Gradle tooling | `knowledge/build-bootstrap.md` and the matching nested `AGENTS.md` |
| Cross-repository consumer work | `knowledge/integrations.md` |
| A named deferred investigation | The matching file under `tasks/` |

`categories/README.md` provides a second routing view by agent/work category.

## Content boundaries

- `knowledge/`: verified current facts, including explicit uncertainty.
- `categories/`: task-oriented routes and category guidance.
- `tasks/`: bounded one-off work, concerns, investigations, and deferred migrations.
- `decisions/`: owner-confirmed choices that future agents should not repeatedly reopen without new evidence.
- `knowledge-distillation.md`: required wrap-up process for substantial sessions.

Do not use this directory as an uncurated scratchpad. Temporary cross-window status belongs in `/tmp/codex-handoffs/`; repeatable executable workflows belong in `.agents/skills/` if added later.
