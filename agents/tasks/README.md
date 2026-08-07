# One-Off Tasks

This directory is the durable queue for bounded investigations, deferred cleanup, migration concerns, and one-off implementation work.

## Status values

- `backlog`: known work with no active implementation.
- `investigating`: evidence collection is underway.
- `decision-needed`: implementation depends on an owner choice.
- `ready`: sufficiently understood for scoped implementation.
- `blocked`: a concrete external prerequisite prevents progress.
- `done`: completed and distilled into current docs/decisions.
- `superseded`: replaced by another task or decision.

## Completion rule

A task is not done merely because code changed. Complete the knowledge-distillation process in `../knowledge-distillation.md`, update current-reality docs, capture remaining concerns/plans, and either conduct a short owner interview for contentious gaps or make obvious updates while reporting them.

Keep completed task notes until their useful evidence and decisions have been distilled. Archive policy can be chosen when completed notes become noisy.
