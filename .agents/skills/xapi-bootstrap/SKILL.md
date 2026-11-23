---
name: xapi-bootstrap
description: Verify and reuse the private XApi bootstrap seed, rebuild local Maven publications, and validate an XApi consumer on its supported JDK. Use for missing XApi Gradle settings or artifacts, seed provenance, clean or incremental builds, and headless or low-memory consumer smoke tests.
---

# XApi bootstrap

If the task only needs a runtime smoke and a complete, approved distribution
matches the selected consumer and artifacts, use that distribution with its
supported JDK. For a LifeQuest consumer, follow the
[consumer reference](references/lifequest.md). Bootstrap source when the task
requires rebuilding or the matching distribution is unavailable.

## Bootstrap the selected source

1. Resolve the exact XApi and consumer revisions, host, authorized commands, and
   isolated sibling checkouts. Read their `AGENTS.md`, the XApi
   [bootstrap README](https://github.com/WeTheInternet/xapi/blob/ee0377b665eebe07e6f3fdefaac8a2893aa75b55/bootstrap/README.md)
   and the consumer's settings and verification docs at the selected commits.
   Verify the consumer's actual local Maven resolution. Coordinate `0.5.1`
   alone does not establish which source produced its mutable jars.
2. **Outer seed ZIP asset SHA-256.** Obtain the exact private asset from an
   approved owner or transfer route. Record the filename, producer, source
   revision, archive version, and publisher release metadata or publisher
   sidecar reference tied to that exact asset. Compute the archive digest before
   extraction. Report **matched** or **mismatched** only against a verified
   publisher pin for the same asset. If the pin is missing or belongs to another
   asset, report **provenance unverified**. Where outer verification is required,
   stop before extraction unless it matched. Name the missing publisher pin or
   the same-asset mismatch and the approved source needed next.
3. **Extracted seed manifest.** After the outer gate passes, inspect archive
   paths and extract into a clean, isolated XApi checkout. Expect only the
   ignored `.xapi-bootstrap/` and `repoMvn/` trees. Reject unsafe paths or
   unexpected entries and avoid overwriting an existing seed. Run
   `./bootstrap/verify-bundle.sh` (PowerShell `bootstrap/verify-bundle.ps1` on
   Windows). Record the `.xapi-bootstrap/MANIFEST.sha256` reference and outcome.
   This checks the extracted files against the seed's internal manifest.
4. **Inner custom Gradle ZIP SHA-256.** Before building, verify the extracted
   `gradle-5.1-x-24.zip` against the distribution pin in
   `bootstrap/create-bundle.sh` at the selected XApi source revision. Record
   its exact filename, that source ref and the outcome. This verifies the
   custom distribution. Keep all three outcomes separate, including when
   reporting someone else's existing verification. Internal checks establish
   neither an outer digest nor a publisher match. Keep the seed private.

## Choose the smallest build

| Situation | Action |
| --- | --- |
| Existing seed has a matching outer pin and passes both installed checks | Reuse it. Record the three verification outcomes and selected XApi source revision. |
| Clean or stale `repo/` publications | Rebuild from the pinned source. A complete clean publication uses `./fullBuild.sh --shadow` with Java 17 for the Gradle 8.11.1 stages and Java 8 selected via `XAPI_JAVA8_HOME` for the legacy stages. The ZIP supplies inputs and omits `repo/`. |
| Routine source rebuild with current legacy publications | Use `./liteBuild.sh` for core, modern, and main publications. Use `--fast` only if the skipped prerequisites are already current. |
| Seed inputs actually changed or an approved archive must be recreated | On the authorized producer, verify the custom Gradle, compiler Maven seed and private sample input. Run `bootstrap/create-bundle.sh` with the reviewed input/output locations, retain the new outer digest and manifest evidence, and establish a new pin before use. If the creator rejects a new custom Gradle digest, stop for review of that source pin. |

Use the checked-in wrapper of the owning build or consumer. Broad XApi scripts
skip tests by default. Run and report focused tests or a headless smoke
separately. For a LifeQuest consumer, read the
[consumer reference](references/lifequest.md) for its repository, runtime,
headless smoke and low-memory options.

The seed leaves ordinary Maven and stock Gradle downloads to the normal
dependency path. When offline, use an approved complete cache if it already
exists for the selected build. On missing seed, JDK, ordinary dependency or
authorization, report the failed stage, exact missing input, who can supply it
and what remains untested. Use [wti-ai-workflow](https://github.com/WeTheInternet/wti-ai)
for capability and owner routing if this surface cannot execute locally.
