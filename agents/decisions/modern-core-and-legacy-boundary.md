# Decision: Separate the active modern core from fork-dependent legacy XApi

- Status: accepted direction
- Date: 2026-08-05
- Owner confirmation: repository owner direction

## Decision

The active XApi component closure used by Kukunochi and `/opt/wti-ui` should build with stock supported Gradle and modern non-fork toolchains. Fork-dependent code should move behind a `net.wti.legacy` boundary.

Desktop and Android are the current platform priorities. Maintaining working GWT artifacts during the split is not required; GWT model/injection support may be suspended until magic-method behavior can be replaced using Java compiler APIs.

## Migration policy

- Determine the boundary from real consumer dependencies, then move the remaining code; names such as “core” are starting hypotheses, not proof.
- Preserve non-GWT model functionality in the active side where its dependency closure permits.
- Package the hacked Gradle/GWT inputs reproducibly for legacy builds in the near term, while continuing to plan their eventual removal.

## Related work

- `../tasks/split-net-wti-legacy.md`
- `../tasks/clean-bootstrap.md`
- `../tasks/retire-custom-gwt.md`
- `../tasks/eliminate-project-view-and-task-selection.md`
