# Task: Establish useful CI without slowing local iteration

- Status: backlog
- Created: 2026-08-05
- Scope: repeatable build/test matrix after bootstrap dependencies are understood

## Desired outcome

Keep local `liteBuild.sh` optimized for fast consumer-driven work while CI performs deliberate focused tests, generated-diff checks, publication validation, and selected consumer smoke tests.

## Prerequisites

Clean-bootstrap inventory, custom toolchain strategy, and an explicit test/publication matrix. CI must not silently depend on one machine's ignored `repo/` or custom distributions.
