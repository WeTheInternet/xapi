# Decision: Optimize local development for fast consumer feedback

- Status: accepted
- Date: 2026-08-05
- Owner confirmation: repository owner interview

## Decision

- `liteBuild.sh` is the normal broad publication path; `fullBuild.sh` is the broader legacy-tool path.
- Broad local scripts compile test classes but intentionally skip running the complete test/check suite. Reusable behavior changes receive focused tests on demand, and consumer builds supply the most useful integration feedback.
- `build` may publish local artifacts. Future CI may separate lifecycle concerns, but local development prioritizes fast refreshed artifacts.
- `xapiPublish` includes every publication targeting `xapiLocal`, including plugin-marker and intentionally dummy publications.
- Mutable `0.5.1` artifacts in local Maven filesystem repositories are expected to refresh in place.
- Generated Gradle scripts remain tracked so generator/configuration changes are visible in review. Agents must not dismiss those diffs solely because they are generated.
- Java 8 compatibility remains required while customized GWT/compiler behavior is still in the dependency chain.

## Consequences

Do not make the everyday scripts exhaustive or remove publication/generated diffs as incidental cleanup. Modernization should preserve fast local iteration while focused tests, future CI, and retirement tasks progressively replace the legacy constraints.

## Related tasks

- `../tasks/xapi-publish-wiring.md`
- `../tasks/establish-ci.md`
- `../tasks/retire-custom-gwt.md`
- `../tasks/clean-bootstrap.md`
