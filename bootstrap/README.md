# XApi bootstrap seed bundle

The bootstrap seed bundle captures the local binary inputs that cannot currently be
reconstructed from this source tree:

- the customized `gradle-5.1-x-24.zip` distribution used by `net.wti.gradle.tools`
  and `net.wti.gradle`;
- the ignored `repoMvn/` snapshot, including the customized GWT/compiler family.
- the legacy `de.mocra.cy:wti-shared:0.5.1-SNAPSHOT` binary needed only by
  `samples:demo` during a broad build.

It deliberately does not package `repo/`. `fullBuild.sh` and `fullBuild.ps1` rebuild
that repository in dependency order: core, modern Gradle support, legacy Gradle tools,
legacy Gradle plugins, then the main schema-generated build.

## Create the bundle

From the repository root on the machine holding the seed inputs:

```bash
./bootstrap/create-bundle.sh
```

The default custom distribution input is `/opt/gradle/gradle-5.1-x-24.zip`. Supply a
different location as the first argument or with `XAPI_LEGACY_GRADLE_ZIP`. The optional
second argument selects the output ZIP. The default output is:

```text
build/bootstrap/xapi-bootstrap-seed-0.5.1.zip
```

The creator rejects a custom Gradle ZIP whose SHA-256 is not the known x-24 checksum.
It reads `wti-shared` from `/opt/wti/repo` by default; set `XAPI_WTI_SEED_REPO` to a
different Maven repository root when necessary.
The output is a platform-neutral ZIP plus a `.sha256` sidecar.

## Install and verify

Extract the bundle into the root of an XApi checkout. It supplies `repoMvn/` and
`.xapi-bootstrap/`; both paths are ignored by Git. Then verify every installed seed:

```bash
./bootstrap/verify-bundle.sh
```

On Windows PowerShell:

```powershell
& .\bootstrap\verify-bundle.ps1
```

The two legacy wrapper property files use a relative `distributionUrl`, resolved from
their own location to `.xapi-bootstrap/gradle/gradle-5.1-x-24.zip`. No `/opt` path is
required after extraction.

## Build prerequisites and entrypoints

- A Java 17 runtime must launch the stock Gradle 8.11.1 wrappers.
- A Java 8 JDK must launch the customized legacy Gradle stages. `fullBuild.sh` and
  `fullBuild.ps1` discover `XAPI_JAVA8_HOME`, `JAVA8_HOME`, `JDK8_HOME`, or
  `JAVA_HOME_8_X64` (in that order), then accept `JAVA_HOME` when it already points to
  Java 8. The Unix script also recognizes the historical local installation and the
  common Debian OpenJDK 8 path. Set `XAPI_JAVA8_HOME` explicitly on other machines.
- Network access is still required for stock Gradle 8.11.1 and ordinary public Maven or
  toolchain dependencies not represented by the irreplaceable seed. This is a clean-machine
  bootstrap seed, not a complete offline cache.
- On Unix-like hosts, run `./fullBuild.sh --shadow` for a complete clean build.
- On native Windows PowerShell, run `& .\fullBuild.ps1 -Shadow`. It performs the same
  five-stage build order and uses the checked-in `gradlew.bat` launchers.

The default no-shadow mode is the faster daily workflow, but it cannot publish a shadow
publication from an empty build directory because the selected artifact does not exist.
Use `--shadow`/`-Shadow` for clean bootstrap verification. `--fast`/`-f` on Unix or
`-Fast` on PowerShell skips all four prerequisite build families when their publications
are already current.

The customized Gradle archive contains its upstream `LICENSE` and `NOTICE`. Provenance
for the locally customized Gradle/GWT changes is incomplete; keep this bundle private
until licensing and binary provenance have been reviewed for external redistribution.
