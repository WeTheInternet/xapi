# Build and Bootstrap Reality

## Supported everyday path

`liteBuild.sh` is the normal broad development command. With no arguments it runs this task set in `net.wti.core`, `net.wti.gradle.modern`, and the main build:

```text
build xapiPublish testClasses -x test -x check -x javadoc
```

The main invocation also disables composite/changing behavior, enables parallel/build cache, and normally excludes `shadowJar`.

Use `liteBuild.sh --fast` or `-f` to skip explicit core and modern prerequisite builds when their local `0.5.1` artifacts are already current.

## Portable clean bootstrap seed

Create the platform-neutral seed archive with:

```text
./bootstrap/create-bundle.sh
```

The ignored output is `build/bootstrap/xapi-bootstrap-seed-0.5.1.zip`. Extract it into
the root of a clean checkout, then run `bootstrap/verify-bundle.sh` or the PowerShell
equivalent. It installs ignored `.xapi-bootstrap/` and `repoMvn/` seeds. `repo/` is
deliberately omitted and rebuilt in dependency order.

The seed currently packages the customized Gradle ZIP, customized GWT/compiler Maven
artifacts, and the private `wti-shared` snapshot used by `samples:demo`. The legacy
wrappers use a relative distribution URL, and the modern migration bridge extracts its
compile-only legacy Gradle API JARs from the same verified ZIP. No `/opt` path is required
after extraction.

Java 17 launches Gradle 8.11.1. Java 8 launches the two customized legacy Gradle builds;
set `XAPI_JAVA8_HOME` when it is not auto-detected. Network access remains necessary for
stock Gradle and public dependencies. This is not an offline cache, and incomplete binary
provenance means the bundle should remain private.

For a complete build from empty build directories, use `fullBuild.sh --shadow` or
`fullBuild.ps1 -Shadow`. Default no-shadow mode is faster but cannot publish a project
whose selected publication artifact is an absent shadow JAR; see
`../tasks/no-shadow-publication-mode.md`.

See:

- `../tasks/clean-bootstrap.md`
- `../tasks/retire-custom-gradle.md`
- `../tasks/retire-custom-gwt.md`

## Script roles

- `liteBuild.sh`: normal fast publish/build path.
- `fullBuild.sh`: core, modern, legacy tools under Java 8, legacy Gradle plugins under
  Java 8, then the composite/changing main build under Java 17. Use `--shadow` for a
  complete clean publication.
- `fullBuild.ps1`: native Windows equivalent; `-Shadow` is the clean publication mode and
  `-Fast` skips the four prerequisite build families.
- `toolBuild.sh`: builds core and modern; `--all` adds both legacy Gradle families.
- `quickBuild.sh`: main build only, excluding tests/check/Javadoc; it currently has older positional-argument handling.

Do not describe any broad script as exhaustive verification: defaults skip actual test execution.
