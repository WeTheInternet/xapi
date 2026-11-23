# LifeQuest and the Quest seed launcher

## Consumer build

Read the LifeQuest README and `settings.gradle` in the selected checkout to
confirm the wrapper JDK and XApi artifact source. The Java 17 build commonly
resolves the XApi settings plugin and selected artifacts from sibling
`../xapi/repo`. The Quest seed distribution belongs to `wti-ui`, whose
[launcher guide](https://github.com/WeTheInternet/wti-ui/blob/7b5f552f4f19dbe6a58ae2fc708baa150a126ad0/model/src/jreQuest/README.md)
documents the separate build. Its inspected settings also allow `xapiRepo`;
verify the actual resolved repository before assuming the sibling default.

If a complete authorized distribution matches the selected consumer and source
artifact identities, use its `bin/lifequest-quest-seed` with sibling `lib/` on
the intended runtime. Otherwise build it in the pinned `wti-ui` checkout with
Java 17, using its own checked-in wrapper:

```sh
./gradlew :model-jreQuest:installDist --no-daemon --max-workers=1 --no-parallel \
  '-Dorg.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=384m'
```

This is a low-memory starting point for the direct consumer wrapper. The broad
XApi scripts supply their default task list only when they receive no
pass-through arguments. Resource flags alone suppress that default. For a
bounded XApi stage, use its owning Gradle wrapper with explicit tasks and
resource limits. If a broad run needs overrides, preserve its intended tasks
explicitly and check option handling on all selected stages. Verify locally
available JDKs and the selected build's toolchain requirements.
Java 8 remains the launcher module's compilation target. Its installed launcher
selects the supported module options for the Java actually used.

## Headless smoke

Use an isolated synthetic seed root, a fresh model directory, and a synthetic
namespace. Follow the installed launcher's supported `--seed-root`,
`--model-dir`, `--namespace` and optional `--manifest-out` arguments. Run it
headless without a display or a production data directory. The
[distribution test](https://github.com/WeTheInternet/wti-ui/blob/7b5f552f4f19dbe6a58ae2fc708baa150a126ad0/model/src/jreQuestTest/groovy/net/wti/quest/model/jre/QuestSeedDistributionSpec.groovy)
shows the scoped headless JVM settings and two-run proof. Verify the expected
first-run insert count, zero inserts on the identical rerun, stable stored
bytes and successful exit. Record the exact launcher, runtime, fixture and
result as evidence of that specific consumer smoke.
