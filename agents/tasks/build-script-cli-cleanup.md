# Task: Make build-script option handling explicit and safe

- Status: backlog
- Created: 2026-08-05
- Scope: `liteBuild.sh`, `fullBuild.sh`, `toolBuild.sh`, `quickBuild.sh`

## Current evidence

`--fast`/`-f` now implements the formerly ineffective skip-tool behavior in `liteBuild.sh`, with old aliases retained. Remaining concerns include string-based argument accumulation, an unused `all_args` value, `quickBuild.sh` using unquoted `$@`, and options whose effects differ between tool and main invocations.

## Desired outcome

Use Bash arrays for task/options, document pass-through semantics, preserve convenient aliases, and add a cheap argument-parsing smoke test without slowing the build.

## Verification

- `bash -n` all scripts.
- ShellCheck with intentional exceptions documented.
- Dry-run/fake-wrapper tests for default, `--fast`, `--main`, `--shadow`, and pass-through arguments.
