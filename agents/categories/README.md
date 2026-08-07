# Work Categories

Use this table to load only the relevant branch of guidance.

| Category | Likely paths | Required guidance |
| --- | --- | --- |
| Bootstrap and publication | root scripts, `gradle/`, build roots | `../knowledge/build-bootstrap.md`, `../knowledge/testing-and-publication.md` |
| Core primitives and parser | `net.wti.core/` | `../../net.wti.core/AGENTS.md` |
| Modern Gradle/settings | `net.wti.gradle.modern/` | `../../net.wti.gradle.modern/AGENTS.md` |
| Settings/schema generator | modern `settings-plugin/`, root `schema.xapi`, generated scripts | settings-plugin `AGENTS.md`, `../knowledge/generated-builds.md` |
| Legacy Gradle archaeology | `net.wti.gradle.tools/`, `net.wti.gradle/` | both matching nested guides and bootstrap knowledge |
| Schema-generated libraries | main domain directories | `../knowledge/generated-builds.md`; investigate domain status before broad edits |
| GWT/client compatibility | `gwt/`, `gwtc/`, GWT platform sources | bootstrap/GWT retirement task notes; assume Java 8/fork constraints |
| Consumer integration | another repository consumes XApi | `../knowledge/integrations.md` and an explicit handoff if sessions are independent |

Add a dedicated category document only after repeated work reveals stable, non-obvious instructions that do not belong in a path-local `AGENTS.md`.
