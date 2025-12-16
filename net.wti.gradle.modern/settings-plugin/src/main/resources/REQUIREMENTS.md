# Xapi Settings / Schema V2 – Requirements

This file is a **requirements checklist** for a SchemaV2 / xapi‑settings V2 implementation.
It is meant to guide design, tests, and implementation work. Each requirement is written
as a checkbox; we will check them off as we implement and test them.

Where possible, requirements are backed by **concrete `schema.xapi` examples** and an
English description of the expected behavior.

---

## 0. Global Goals

### 0.1 High‑level design goals

- [ ] **Schema is the single source of truth.**  
  `schema.xapi` fully describes:
  - which logical projects exist,
  - which platforms and modules they contain,
  - how those modules depend on each other (internal, project, external),
  - which nodes are candidates to become Gradle modules.

- [ ] **V2 lives alongside V1 until complete.**  
  Build a **SchemaV2 / analyzer V2** infrastructure that can coexist with the current system.  
  Only once V2 is clearly superior (documented, tested, and exercised against real schemas)
  do we remove legacy V1 behavior.

- [ ] **Index is canonical; readers are clients.**  
  - The root build (or a designated “index writer” build) fully computes the index once.  
  - All other builds (including included builds) are **read‑only clients** of that index.  
  - No Gradle project should need to “re‑parse the world” after the index is written.

- [ ] **Performance: many small projects are OK.**  
  - Exploding platform:module combinations into separate Gradle projects is acceptable and
    even desirable (parallelism, simple publishing, IDE responsiveness).  
  - Do *not* introduce “resolution profiles” that fold many modules back into fewer projects;
    previous experiments of that style were too slow.

- [ ] **Examples‑first specification.**  
  - REQUIREMENTS.md should grow into a collection of **examples plus expectations**.  
  - For each pattern of `schema.xapi`, we specify the implied Gradle structure and behavior.  
  - These examples later become executable tests.

---

## 1. Index and Liveness Requirements

The `build/xindex` directory is the canonical representation of the build graph and its
liveness state. It already exists; V2 must formalize and rely on it.

- [ ] **Index structure is documented.**  
  - At a high level in this REQUIREMENTS.md.  
  - In a dedicated `INDEX.md` / similar that explains:
    - `coord/` (group:name:version, etc.),
    - `path/` (xapi project paths),
    - node directories, `live` files, `in/` and `out/` edges.

- [ ] **Liveness is computed *into* the index, not re‑derived ad‑hoc.**  
  Once indexing is “finished”, the index on disk must contain, for every
  `[project, platform, module]` node:
  - a `live` marker file with a numeric value (e.g. `0`, `1`, `2`), and
  - edge directories (`in/` and `out/`) describing dependency relationships.

- [ ] **Liveness semantics are explicit and minimal.**  
  Define three conceptual liveness states:
  - `0` – dead (not live),
  - `2` – base‑live (has sources and/or explicit `requires`),
  - `1` – propagated‑live (reachable from a base‑live node via deps).  

  A node is **live** iff its `live` value is `> 0`.

- [ ] **Base‑live conditions** (what sets `live >= 2`) are clearly defined:  
  A `[project, platform, module]` node is base‑live if *any* of the following hold:
  - It has sources or resources in the corresponding filesystem location, **including**:
    - regular code under `src/<platMod>/...`,
    - Gradle build fragments under `src/gradle/<key>/...` (e.g. `buildscript.start`),
    - or other explicit code roots we define.
  - It declares **explicit `requires` dependencies** (internal/project/external) in `schema.xapi`
    for that `[platform,module]`.

- [ ] **Propagation rules** (what sets `live = 1`) are well‑defined:  
  - Starting from all base‑live nodes, we follow `out/` edges (internal + project deps).  
  - Any node reachable from a base‑live node becomes `live = max(live, 1)`.  
  - Propagation does **not** promote nodes that are reachable only from dead nodes.

- [ ] **SchemaIndexReader uses only the index for liveness.**  
  - `hasEntries(buildCoords, projectName, platform, module)` is equivalent to:
    “The index says this node’s `live` value is > 0.”  
  - Any more nuanced queries (e.g., “has explicit dependencies?”, “has external deps?”)
    are separate methods, not mixed into liveness.

- [ ] **Settings plugin reads, never recomputes liveness.**  
  `XapiSettingsPlugin` must:
  - Treat `SchemaIndexReader` as the authoritative liveness oracle.  
  - Never attempt to recompute liveness from the AST or Gradle state once the index is ready.

---

## 2. Standalone (Single‑Platform) Projects

Terminology here: “standalone” = “single‑platform / single‑module” in terms of realized Gradle
surface. Implementation details may still use a full `[platform,module]` internal model.

### 2.1 Requirements

- [ ] **A standalone project has exactly one active `[platform,module]` at realization time.**  
  - That pair may not be `main:main` (e.g., `jre:main` only).  
  - It remains addressable internally as `(platform, module)`, but Gradle sees a
    single module.

- [ ] **Standalone projects can be “platform filtered”.**  
  - It must be possible to run the build with `xapi.platform=somePlatform`, such that:
    - Only that platform and its *parents* (via `replace`) are realized for each project.
    - All other platforms are pruned from realization (though they may still appear in schema).

- [ ] **Coordinate retention for standalone.**  
  - Even when only `jre:main` is realized, the node must retain its full
    `(platform=jre, module=main)` coordinates in the index.  
  - This is necessary so that:
    - injection/platform selection logic works consistently,
    - other nodes can refer to it precisely if they choose to.

- [ ] **Plain project dependencies to standalone projects are coordinate‑less from the consumer.**  
  - For a standalone target project `:producer2` with a single `[plat,mod]`:
    - `requires = { project : "producer2" }` (or `":producer2"`) **must** mean:
      “depend on the single realized `[platform,module]` of that project.”  
  - If coordinates are supplied explicitly, they **must** be correct:
    - `requires = { project : { ":producer2" : "jre:main" } }` must resolve to the same node.

- [ ] **Project‑deps must be aware of the target project’s coordinate space.**  
  - `project` type dependencies must use the **target** project’s schema to interpret:
    - whether it is standalone,
    - which `(platform,module)` combinations it supports,
    - and how to map a plain `project: ":path"` or `project: "path"` into a concrete node.

- [ ] **For standalone targets, do *not* auto‑guess platforms/modules.**  
  - When the target is known to be “single‑platform / single‑module”:
    - A plain `project : "producer2"` must always resolve to exactly that node.
    - Code must **not** try to “use the same `[plat,mod]` as the consumer” for such a target.

### 2.2 Example: standalone `jre:main` target

```xapi
<xapi-schema
    platforms = [
        <main />,
        <jre replace = "main" />
    ]
    modules = [ main ]
    projects = [
        <producer2
            inherit    = false
            platforms  = [ jre ]      // only jre
            modules    = [ main ]     // only main
        /producer2>,
        <consumer
            platforms = [ main ]
            modules   = [
                <main
                    requires = {
                        // plain project dependency; must resolve to producer2:jre:main
                        project : "producer2"
                    }
                /main>
            ]
        /consumer>
    ]
/xapi-schema>
```

**Expected behavior:**

- Index has:
  - `producer2` with node `(jre, main)` marked live (due to sources or requires),
  - `consumer` with `(main, main)` live.
- `consumer:main` → dependency on `producer2`’s single realized `(jre,main)` node.
- No other `[plat,mod]` permutations of `producer2` are realized.

---

## 3. Test Modules and Test Sourcesets

Gradle’s built‑in `main` / `test` sourcesets and test configurations are important, but
SchemaV2 **must not** hard‑wire a single “test module” concept. Instead, *any*
`project:platform:module` may have one or more associated test source sets, and test
dependencies are a property of the **consumer** (non‑transitive by default).

#### 3.1 Requirements

- [ ] **Every node can have zero or more “test‑type” source sets.**  
  - For any live `[project,platform,module]` node, SchemaV2 must be able to attach one or
    more test source sets (e.g. `test`, `integrationTest`, `uiTest`, etc.).  
  - These source sets map naturally onto Gradle test configurations
    (`testImplementation`, `integrationTestImplementation`, etc.).

- [ ] **`test` is *not* a special module or platform; it is a *source set kind*.**  
  - Modules like `testTools` are ordinary modules, identical in status to `main`, `api`, etc.  
  - A module or platform *named* `test` is strongly discouraged and may be disallowed or
    reserved in SchemaV2 (final decision TBD; keep this as a design note, not a hard rule
    yet).

- [ ] **Test dependencies are declared via `requires`, and are non‑transitive by default.**  
  - A “test dependency” means:  
    _“for a given consumer `[project,platform,module]` and a given test source set name,
    add this dependency to the appropriate test configuration in the consumer.”_  
  - These dependencies are **non‑transitive**: a module being used as a test dependency
    does not itself cause its own test dependencies to propagate.

- [ ] **Transitive test behavior is modeled via regular modules (e.g. `testTools`).**  
  - To share reusable test utilities across projects:
    - publish them as standard modules (e.g. `testTools`), with normal dependencies,  
    - have consumers depend on them using a test‑scoped requires annotation.  
  - There is no “magical” transitive test graph; transitivity is built from ordinary
    published modules plus explicit test‑scoped consumption.

- [ ] **Introduce an explicit test annotation for `requires`.**  
  - Replace or deprecate the current `@transitive("test")` pattern in favor of something
    clearer, e.g.:
    - `@test` → attach this dependency to the default `test` sourceset of the consumer,  
    - `@test("integrationTest")` → attach it to `integrationTest` (or similar).  
  - The *producer* remains a normal module; the **consumer** decides which test source set
    uses it.

  Example sketch (SchemaV2 design target):

  ```xapi
  <xapi-schema
      platforms = [ main ]
      modules   = [ main, testTools ]
      projects  = [
          <libUnderTest
              modules = [
                  <testTools
                      published = true
                      requires  = { external : "org.junit.jupiter:junit-jupiter-api:5.10.0" }
                  /testTools>
              ]
          /libUnderTest>,
          <consumer
              modules = [
                  <main
                      requires = {
                          @test("integrationTest")
                          project : { "libUnderTest" : "main:testTools" }
                      }
                  /main>
              ]
          /consumer>
      ]
  /xapi-schema>
  ```

  Expected:
  - `libUnderTest-testTools` is a normal publishable module.  
  - `consumer-main` has an `integrationTestImplementation` (or equivalent) dependency on
    that module; `libUnderTest`’s own test deps do *not* propagate transitively.

---

## 4. Multiplatform Projects

Multiplatform projects are those where many `[platform,module]` combinations are potentially
realized as separate Gradle projects. This is especially important for:

- injection platforms (`main`, `jre.replace main`, `android.replace jre`, `gwt.replace main`),
- complex “matrix” libraries.

### 4.1 Requirements

- [ ] **Exploded per `[platform,module]` projects are the default for multiplatform.**  
  - Each live combination `(project, platform, module)` is realized as its own Gradle project
    (modulo main/main name‑reduction heuristics).
  - This is already how things work; SchemaV2 should treat this as the *standard* strategy.

- [ ] **Name‑reduction rules are preserved and documented.**  
  - `main` alone means `main:main`.  
  - `gwt` alone means `gwt:main`, `jre` means `jre:main`.  
  - `api` means `main:api`, `spi` means `main:spi` (assuming `api` and `spi` are registered modules).  
  - Resulting Gradle project names should be documented (e.g. `:service-mainApi`,
    `:service-jreMain`, etc.) with concrete examples.

- [ ] **Live–but–non‑contributing “middle” nodes are allowed initially.**  
  - Some nodes may be live (due to graph structure) but:
    - have no own sources, and
    - have no own external deps.  
  - For SchemaV2 initial rollout, it is acceptable to **keep** those nodes as
    separate Gradle projects, even though an ideal future version might “compress
    them out” and depend directly on their final producers.

- [ ] **Future compression is a separate concern.**  
  - Requirements should acknowledge a possible *later* feature:
    - compress non‑contributing internal nodes during Gradle wiring,  
    - but V2’s first goal is correctness and clarity, not compression.

---

## 5. Plain Project Dependencies and Platforms

There is a tension between:

- “Choose the same `platform:module` in the producer as the consumer,” and
- “Use simple `project: ":path"` for standalone or main‑platform cases.”

SchemaV2 should steer toward *predictable* behavior.

- [ ] **Plain project deps default to main/main or a single standalone node.**  
  - If the target project has:
    - exactly one realized node `(plat,mod)` → use that node, OR
    - multiple nodes, but one canonical `(main,main)` → use `main:main` by default.

- [ ] **If the consumer wants a non‑default platform/module, it must say so.**  
  - The syntax:
    - `project : { ":otherProj" : "plat:mod" }` is the canonical way to specify
      “I want a particular `(platform,module)` from that project.”

- [ ] **Implicit “same plat:mod as consumer” should be reconsidered.**  
  - The current behavior (“if exact plat:mod exists in both, use it”) is powerful
    but subtle and may be error‑prone.  
  - SchemaV2 design should either:
    - drop this behavior entirely, or
    - make it explicit and opt‑in (not default).

---

## 6. Virtual Projects

Virtual projects are used for structuring, grouping, and inheritance, not for direct
artifact production.

- [ ] **Virtual projects do not become Gradle projects.**  
  - Their purpose is:
    - to define shared `platforms`, `modules`, and `requires`,
    - to host nested project hierarchies,
    - to shape Gradle project names (via nesting).

- [ ] **Nesting and naming rules are documented.**  
  - E.g., a hierarchy like `ui` (virtual) → `gdx` (virtual) → `editor` (multimodule) → `timeEditor` (module)  
    might produce something like `:ui-gdx-editor-timeEditor`.  
  - Current behavior is messy; SchemaV2 should:
    - document the intended behavior, and
    - provide clear examples.

---

## 7. Examples as Requirements (to be expanded)

This section is intentionally incomplete; it will grow as we encode more real‑world
schemas as examples. Each example should follow this pattern:

- full `schema.xapi` snippet,
- list of expected Gradle projects and names,
- description of expected dependencies and liveness.

### 7.1 Simple single‑platform / single‑module schema

- [ ] **Example: simplest standalone app.**

```xapi
<xapi-schema
    platforms = [ main ]
    modules   = [ main ]
    projects  = [
        <app />
    ]
/xapi-schema>
```

**Expectations:**

- Exactly one Gradle project `:app`.
- One live node `(app, main, main)` in index.
- No extra subprojects are created.

### 7.2 Standalone jre:main schema with consumer

- [ ] **Example: standalone `jre:main` producer consumed by `main:main` consumer.**

(See §2.2 – already specified; tests should be derived directly from that example.)

---

Further examples and checklists will be added as we refine SchemaV2 and as we
audit existing real‑world `.xapi` schemas.

## 8. Templates Instead of Inheritance

The existing V1 model relies heavily on **inheritance** (`inherit=true/false` flags and
implicit parent state). This makes behavior too stateful and hard to reason about.
SchemaV2 should replace most inheritance with **templating**.

### 8.1 Requirements

- [ ] **Introduce explicit templates as reusable schema fragments.**  
  - A template represents a reusable chunk of AST: platforms, modules, requires, etc.  
  - Templates may be defined:
    - inline within a `schema.xapi` file, or
    - by referencing external `.xapi` files.

- [ ] **Projects apply templates explicitly via `applyTemplate`.**  
  - Each schema file (or project element) can declare:

    ```xapi
    <xapi-schema
        templates = [
            <client-template
                // e.g. main | gwt | jre | android | gdx
            /client-template>,
            <server-template
                // e.g. main | vertx | netty | tomcat
            /server-template>,
        ]
        projects = [
            <app
                applyTemplate = [ "client-template", "gdx-template" ]
                // local overrides...
            /app>
        ]
    /xapi-schema>
    ```

  - Applying a template means: *take a fixed, immutable AST fragment and compose it into
    the project’s definition*, with deterministic merge semantics (to be specified).

- [ ] **AST is treated as immutable; visitors build compiled graphs.**  
  - Parsing `schema.xapi` should be **one pass** that:
    - parses into a persistent, immutable AST representation,
    - does as little “decision‑making” as possible.  
  - A separate compilation step (or set of visitors) should:
    - apply templates,
    - resolve platforms/modules,
    - build the object graph used for indexing (SchemaMap/SchemaProject/etc.).

- [ ] **Minimize stateful traversal in the parser.**  
  - SchemaV2’s parser should:
    - avoid mutating global state while walking the AST,
    - instead produce an intermediate representation that is easy to reason about and
      replayable (e.g. for tests or alternate back‑ends).

- [ ] **Templating replaces most uses of `inherit`.**  
  - The `inherit` flag in V1 is a major source of hidden behavior: children “just” pick up
    whatever their parents have configured.  
  - In V2, the preferred pattern is:
    - define reusable templates (client, server, common, etc.),
    - apply them explicitly where needed,
    - make inheritance strictly limited or deprecated.

### 8.2 Example: client/server templates

- [ ] **Example: client/server templates with explicit application.**

  ```xapi
  <xapi-schema
      templates = [
          <client-template
              platforms = [
                  main,
                  <gwt     replace = "main" />,
                  <jre     replace = "main" />,
                  <android replace = "jre"  />,
                  <gdx     replace = "jre"  />,
              ]
              modules = [ api, main ]
          /client-template>,
          <server-template
              platforms = [
                  main,
                  <vertx  replace = "main" />,
                  <netty  replace = "main" />,
                  <tomcat replace = "main" />
              ]
              modules = [ api, main ]
          /server-template>,
      ]
      projects = [
          <frontend
              applyTemplate = [ "client-template", "gdx-template" ]
          /frontend>,
          <backend
              applyTemplate = [ "server-template" ]
          /backend>
      ]
  /xapi-schema>
  ```

**Expected behavior:**

- `frontend` and `backend` receive well‑defined platform/module sets from templates.  
- Any additional local configuration is applied *after* templates, with deterministic
  override/merge rules.  
- No implicit parent “inheritance” is required to make this work.