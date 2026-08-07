# Task: Define and restore a clean-machine bootstrap

- Status: done (native Windows smoke tracked separately)
- Created: 2026-08-05
- Scope: ignored local repositories, bootstrap ordering, required artifacts and distributions

## Desired outcome

A versioned bootstrap artifact that can be unpacked on a clean container, Linux host, or Windows machine and then build XApi locally without relying on undocumented contents of one developer machine.

Near-term reproducibility is more important than purity: the artifact may bundle the customized Gradle distribution, hacked GWT/compiler jars, and seeded local Maven inputs until the legacy boundary is retired.

## Implemented result

- `bootstrap/create-bundle.sh` creates one platform-neutral seed ZIP plus SHA-256 sidecar.
- The seed contains the customized Gradle distribution, the customized `repoMvn` GWT
  family, and the private `wti-shared` snapshot needed by `samples:demo`. It omits
  rebuildable `repo/` outputs.
- Both legacy wrappers resolve the customized distribution through a relative path below
  `.xapi-bootstrap/`; the modern migration bridge extracts its five compile-only Gradle
  libraries from that verified ZIP instead of `/opt/gradle`.
- `fullBuild.sh` and `fullBuild.ps1` isolate Java 8 to the two legacy Gradle stages while
  the Gradle 8.11.1 stages run on Java 17.
- Bash and PowerShell manifest verifiers validate every extracted seed file.

Current artifact:

```text
build/bootstrap/xapi-bootstrap-seed-0.5.1.zip
SHA-256 722a902f087b9ff8e3d5eca8d8c00a4a61be07eb975ae840628ba7def90f571e
```

## Verification

- Final archive checksum, ZIP integrity, embedded 97-file manifest, and Windows-illegal
  archive components passed.
- A source-only Linux fixture with no `.git`, `repo/`, Gradle caches, build outputs, or
  preinstalled repository seeds extracted the bundle and completed
  `./fullBuild.sh --shadow`. All four prerequisite build families published, followed by
  a successful 1,607-task main build/publication graph.
- The clean build exposed and verified three source fixes: synthetic source projects now
  advertise Java 8, `core:reflect` uses Java 17 with `--release 9` for its multi-release
  source set, and `dev:javac-main` generates its runtime injection bindings.
- Native PowerShell execution has not been tested because this environment has no Windows
  runner. That remaining platform smoke is tracked in `native-windows-bootstrap-smoke.md`.

The archive is a clean-machine seed, not an offline dependency cache or a redistributable
release. Java 17, Java 8, network access for public dependencies, and a source checkout
are still required. Binary provenance is incomplete, so keep it private.
