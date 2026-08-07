# Decision: Retire custom Gradle abstraction layers

- Status: accepted
- Date: 2026-08-05
- Owner confirmation: repository owner direction

## Decision

`Task.whenSelected(...)` and the `ProjectView` / `ProjectViewInternal` / `MinimalProjectView` families must be removed so XApi can sever its dependency on ancient customized Gradle behavior.

Do not preserve these concepts through a renamed universal facade. Replace call sites with narrow data, services, and standard Gradle lifecycle APIs, then delete the obsolete layers.

## Related work

- `../tasks/eliminate-project-view-and-task-selection.md`
- `../tasks/retire-custom-gradle.md`
