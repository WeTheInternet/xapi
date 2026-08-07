# Task: Repair `xapiPublish` and `publishRequired` wiring

- Status: done
- Sequence: after Windows-safe generated/index paths
- Created: 2026-08-05
- Completed: 2026-08-05
- Scope: `gradle/xapi-modern.gradle`, bootstrap build task graphs, focused publication verification
- Exclusions: generated main-project publication logic; remote release design

## Desired outcome

Make `xapiPublish` reliably publish all intended publications to `xapiLocal`, and make root `publishRequired` aggregate useful leaf publication tasks even when `build` is not also requested.

## Current evidence

`tasks.withType(PublishToMavenRepository).configureEach` adds dependencies only after matching publication tasks are realized. Requesting `xapiPublish` alone does not realize them, so the lifecycle task is empty. `publishRequired` correctly finds child `xapiPublish` tasks, but those leaves are empty.

Dry-run evidence:

- focused `:xapi-gradle-settings-plugin:xapiPublish`: only itself;
- modern root `xapiPublish`: one empty lifecycle task per subproject;
- modern root `publishRequired`: those empty tasks plus the root aggregator;
- focused `build`: publication tasks appear because `assemble.finalizedBy("publish")` independently realizes them.

Default `liteBuild.sh` therefore publishes through `build`, not through its simultaneous `xapiPublish` request.

## Decisions

- Keep the accepted behavior that `build` may publish; eventual CI can revisit it.
- `xapiPublish` means **all publications targeting `xapiLocal`**, including plugin markers and intentionally dummy publications.
- Preserve `publishRequired` as a build-boundary/composite prerequisite aggregator. In the current coarse model, that means all leaf `xapiPublish` tasks exposed by the included build; dependency-derived artifact minimization would be a separate future optimization.

## `publishRequired` research

`publishRequired` is an XApi lifecycle-task name, not a Gradle term. It was introduced in commit `8e32d8cb` (`Multi-variant Composite Build Publishing Success Action!`, 2019-01-26).

The legacy `XapiPublishPlugin` establishes a two-sided composite-build handshake:

1. In the top-level build, each selected `xapiPublish` depends on `:publishRequired` in every included build.
2. Inside an included build, the root `publishRequired` task depends on that build's project-level `xapiPublish` tasks when the aggregator is selected.

The associated tests describe and assert publishing all producer and consumer tasks. No implementation computes the minimal publications from the consumer's dependency graph. “Required” therefore means “the publication surface the included build exposes as a prerequisite,” not “only coordinates directly referenced by this consumer.”

The legacy handshake uses nonstandard `Task.whenSelected(...)` calls (and Gradle internal APIs). There is no implementation of that `Task` method in this repository, making this code another fingerprint of the customized Gradle fork. Treat it as intent/history, not an implementation to copy into Gradle 8 code.

Later bootstrap implementations copied only the included-build side of the convention:

- `net.wti.core` and `net.wti.gradle.modern` register an empty root task, while `gradle/xapi-modern.gradle` adds every subproject `xapiPublish` to it.
- `net.wti.gradle.tools/root-tools.gradle` does the same eagerly.
- The main build's root `build.gradle` aggregates every generated project `xapiPublish` it can find.

Current scripts usually build/publish prerequisite build families explicitly, so they do not rely on the old top-level composite handshake. `liteBuild.sh` also disables composites for the main invocation; `fullBuild.sh` enables main-build inclusion of core/modern only after explicitly building them.

## Suggested reading

- `net.wti.gradle.tools/deprecated/src/main/java/net/wti/gradle/publish/plugin/XapiPublishPlugin.java`: original two-sided composite handshake and all-publications realization.
- `net.wti.gradle.tools/deprecated/src/test/groovy/net/wti/gradle/publish/plugin/XapiPublishTest.groovy`: intended composite/non-composite outcomes.
- `gradle/xapi-modern.gradle`: current core/modern leaf publication and root aggregation wiring; contains the lazy-realization defect.
- `net.wti.core/build.gradle` and `net.wti.gradle.modern/root-modern.gradle`: empty root aggregator declarations.
- `net.wti.gradle.tools/root-tools.gradle`: older eager implementation that still realizes publish tasks correctly.
- root `build.gradle`: main generated-build aggregator.
- Historical introduction: `git show 8e32d8cb`.
- `agents/tasks/retire-custom-gradle.md`: the broader task for eliminating APIs such as `Task.whenSelected(...)`.

## Implementation

`gradle/xapi-modern.gradle` now wires every leaf `xapiPublish` to Maven Publish's repository lifecycle task by string task name:

```groovy
xapiPublish.dependsOn("publishAllPublicationsToXapiLocalRepository")
```

The string dependency defers task lookup until Maven Publish has configured repositories/publications, avoiding the lazy-realization loop. The existing `configureEach` and `assemble.finalizedBy("publish")` behavior is preserved.

## Verification results

- `net.wti.core :xapi-fu:xapiPublish --dry-run` reaches `publishMainPublicationToXapiLocalRepository` and `publishAllPublicationsToXapiLocalRepository`.
- `net.wti.gradle.modern :xapi-gradle-settings-plugin:xapiPublish --dry-run` reaches main, `pluginMaven`, and `xapiSettingsPluginMarkerMaven` publication tasks, all for `xapiLocal`.
- Root `publishRequired --dry-run` in both core and modern reaches every leaf `xapiPublish` and useful repository publication task.
- Real `:xapi-gradle-settings-plugin:xapiPublish --rerun-tasks` succeeded with 19 executed tasks and refreshed the modern main artifact, intentional `net.wti.unused` plugin artifact, and `xapi-settings` marker POM.
- A focused liteBuild-equivalent graph (`build xapiPublish testClasses -x test -x check -x javadoc --dry-run`) contains each publication task once and no remote publication task.
- `git diff --check` passed.

## Latest status

The lifecycle defect is repaired and verified. Keep the coarse `publishRequired` meaning unless a separate dependency-minimal publication design is approved.
