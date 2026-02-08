# Xapi Settings: Schema Model and Semantics

This document defines the intended behavior of the `schema.xapi` language as used by `xapi-settings`. It is **intentionally implementation‑agnostic**: it describes *what* should happen, not *how* the current code does it.

The goal is to treat `schema.xapi` as the single source of truth for:

- What projects exist in a build,
- Which platforms and modules each project has,
- How those modules depend on each other (internally, across projects, and externally),
- Which of those modules are *live* (i.e. should actually be realized as Gradle modules / variants).

Later implementation work and refactoring should aim to make the runtime behavior match this document.

---

## 1. Concepts

### 1.1 Schema

A `schema.xapi` file describes a *schema root* — a logical root project whose children (real or virtual) can also have their own `schema.xapi` files.

Top level structure:

```xapi
<xapi-schema
    name     = "root-name"       // optional, default = directory name
    group    = "com.example"     // default group for publishing, etc.
    version  = "1.0.0"           // default version
    platforms = [
        main
    ]
    modules   = [
        main
    ]
    projects  = [
        app
    ]
/xapi-schema>
```

All child `projects` inherit from this root schema unless they explicitly opt out.

### 1.2 Projects

A **Schema Project** represents a logical project (often, but not always, a Gradle project). It may be:

- **Standalone / single‑platform** (the typical case),
- **Multiplatform** (an aggregator whose child Gradle projects represent platform:module combinations),
- **Virtual** (a purely logical container or grouping, not intended to have code of its own).

Example:

```xapi
<xapi-schema
    platforms = [ main ]
    modules   = [ main ]
    projects = [
        consumer,
        <producer
            multiplatform = true
        /producer>,
        <ui
            virtual = true
            multiplatform = true
            projects = [
                <widgets
                    platforms = [ main ]
                    modules   = [ main ]
                /widgets>
            ]
        /ui>,
    ]
/xapi-schema>
```

#### 1.2.1 Project attributes

- `name`  
  The logical name of the project. The Gradle path normally corresponds to `:<subpath>:<name>`.

- `multiplatform` (default: depends on parent; root defaults to `true` only if explicitly stated)  
  - `true`: this project is an **aggregator**; each `[platform,module]` pair may become its own Gradle subproject (`<projectName>-<platMod>`).
  - `false`: this project is **single‑platform**; only its default `[platform,module]` pair becomes a Gradle project.

- `virtual` (default: `false`)  
  - `true`: this project is a logical grouping / namespace; concrete buildable modules usually belong to its descendants, not the virtual node itself.
  - `false`: this project can have real modules and code.

- `inherit` (default: `true`)  
  Controls inheritance of platforms/modules from the parent schema project:
  - `true`: this project inherits the parent’s platforms/modules (unless overridden).
  - `false`: this project only has platforms/modules explicitly declared in its own `platforms`/`modules` attributes (and in its own `schema.xapi`, if any).

- `platforms = [...]`  
  Per-project restriction or augmentation of the set of platforms (see §2).

- `modules = [...]`  
  Per-project restriction or augmentation of the set of modules (see §3).

- Nested `projects = [...]`  
  Defines child schema projects; these form a tree rooted at the `<xapi-schema>`’s top project.

### 1.3 Platform

A **Platform** is a logical target environment for building archives (e.g. `main`, `jre`, `gwt`, `dev`, `prod`, etc.). Conceptually:

- Every project has **at least one platform** (default: `main`).
- Platforms may *replace* other platforms, effectively layering new behavior and dependencies on top of a base platform (e.g. `dev replaces main`).

Typical declarations:

```xapi
<xapi-schema
    platforms = [
        main,
        <dev  replace = "main" />,
        <prod replace = "main" />
    ]
    modules = [ main ]
    projects = [ app ]
/xapi-schema>
```

#### 1.3.1 Platform attributes

- `name`  
  The platform name (e.g. `main`, `dev`, `prod`, `gwt`).

- `replace` / `replaces`  
  A single platform name that this platform *replaces*. Conceptually:
  - The new platform gets all archives (modules) and behavior of the parent platform,
  - Plus any additional configuration specific to the child platform.
  - For dependency resolution, the child platform can see and build upon its parent’s `[platform,module]` graph.

- `published` (default: `true` for `main`, `false` otherwise, but configurable)  
  Indicates whether artifacts built for this platform should, by default, be considered publishable.

- `needSource` / `publishSource`  
  Drive whether this platform consumes/publishes source artifacts; not elaborated here.

- `test` (default: `false`)  
  Indicates whether this platform is specifically for test code (affects how test tasks are wired).

- `requires = { ... }` at **platform scope**  
  A platform-wide *require block* attaches dependencies to *all modules* on this platform, unless further restricted. See §4.

### 1.4 Module (Archive)

A **Module** (also “archive”) is a logical build artifact within a platform:

- Typical modules: `main`, `api`, `spi`, `test`, `sample`, etc.
- For a given project and platform, each module is a separate archive (and typically becomes a separate Gradle “variant” or even a separate subproject).

Examples:

```xapi
<xapi-schema
    platforms = [ main ]
    modules = [
        api, spi,
        <main include = [ api, spi ] /main>,
    ]
    projects = [ lib ]
/xapi-schema>
```

or:

```xapi
<xapi-schema
    platforms = [ main ]
    modules   = main
    projects  = [ app ]
/xapi-schema>
```

or, platform-scoped modules:

```xapi
<xapi-schema
    platforms = [
        <main
            modules = [
                <sample
                    includes = main
                    published = true
                    requires = { external : "junit:junit:4.13.2" }
                /sample>,
            ]
        /main>,
        dev
    ]
    modules = [ main ]
    projects = [ app ]
/xapi-schema>
```

#### 1.4.1 Module attributes

- `name`  
  Module name (e.g. `main`, `api`, `spi`, `test`, `sample`).

- `includes` / `include`  
  A list of other module names within the *same project/platform* that this module conceptually includes:
  - This translates into internal dependencies (e.g. `main` depends on `api` and `spi`).
  - Includes are *transitive* — “what main includes” is part of its canonical, “what main provides” view.

- `published` (default: `true` for `main`, `false` for others unless configured)  
  Whether this module is intended to have a published artifact by default.

- `test` (default: `false`)  
  Whether this module is a test module (affects test tasks and wiring).

- `force`  
  Additional control over module realization; not elaborated here.

- `requires = { ... }` at **module scope**  
  Module-level dependencies: internal, project, external, and platform-structured dependencies (see §4).

- `forPlatform` (internal)  
  Used when modules are declared inside a platform’s `modules = [...]` block; conceptually, such a module applies only to that platform.

---

## 2. Platforms: Replacement and Inheritance

Platform replacement is a key feature:

```xapi
<xapi-schema
    platforms = [
        <main />,
        <impl   replace = "main" />,
        <jre    replace = "impl" />,
        <sample replace = "jre" />,
    ]
    modules = [
        api, spi,
        <main include = [ api, spi ] /main>,
    ]
    projects = [ game ]
/xapi-schema>
```

Semantics:

1. Each platform defines a *layer* of configuration on top of its `replace` chain. For example:
   - `impl` sees everything from `main`, plus its own `requires` and `modules` customizations.
   - `jre` layers on top of `impl`, etc.

2. For any project:
   - The set of platforms is the union of:
     - Platforms declared at the root schema,
     - Platforms declared on the project (if `inherit=true`),
     - Or only those declared locally (if `inherit=false`).

3. When a platform B replaces platform A:
   - B’s modules conceptually “wrap” A’s modules:
     - Default behavior is that B has the same module set as A, unless overridden.
     - B can add additional modules or configure modules differently (e.g. extra dependencies).

4. For dependency resolution:
   - Child platforms are allowed to inherit internal edges from the replaced platform:
     - If `impl` replaces `main`, then `impl:main` can see `main:main`’s internal deps as a base.
   - Additional `requires` on child platforms stack on top of the parent.

---

## 3. Modules: Inheritance and Aggregation

Modules exist at several scopes:

1. **Global module definitions** at the schema root:
@ -260,3 +520,229 @@
   These define the *default* module set for all inheriting projects/platforms.

2. **Project-specific modules** (optional):

```xapi
<xapi-schema
    platforms = [ main ]
    modules   = [ main, test ]
    projects = [
        <producer2
            inherit   = false
            platforms = [ main ]
            modules   = [ main ]
        /producer2>
    ]
/xapi-schema>
```

   - `inherit = false` means it does *not* inherit the root module set.
   - `modules = [ main ]` gives it exactly a `main` module.

3. **Platform-specific modules** inside a platform element:

```xapi
<xapi-schema
    platforms = [
        <main
            modules = [
                <sample
                    includes = main
                    published = true
                    requires = { external : "junit:junit:4.13.2" }
                /sample>,
            ]
        /main>,
        dev
    ]
    modules = [ main ]
    projects = [ app ]
/xapi-schema>
```

   - `sample` exists only on the `main` platform for this project.
   - Its dependencies do not automatically apply to other platforms unless explicitly routed via `platform: { ... }` dependencies.

### 3.1 Includes

`include` / `includes` on a module mean:

- At the schema level:
  - “Module X includes module Y” = “for the same `[project, platform]`, X has an internal dependency on Y”.
- Includes are conceptually transitive:
  - If `main includes [api, spi]` and `spi includes [testTools]`, then `main` effectively depends on `api`, `spi`, and `testTools`.

The intended behavior is:

- Includes form part of the canonical dependency view that determines:
  - Publication (e.g. what should be transitively available from `main`),
  - Realization (which modules are worth building because they are required by a published module).

---

## 4. Requires: Dependency Semantics

The `requires = { ... }` block is used at several levels:

- On the **schema root** itself (project-wide requirements),
- On a **project** element (per-project defaults),
- On a **platform** element (per-platform defaults),
- On a **module** element (per-module specifics).

The semantics are additive and compositional.

### 4.1 Keys in `requires` blocks

A `requires` block is a JSON-like map:

```xapi
<xapi-schema
    platforms = [ main ]
    modules   = [ main ]
    projects  = [
        <app
            modules = [
                <main
                    requires = {
                        internal : [ "api", "spi", "main:test" ],
                        project  : [
                            ":core-lib",
                            "util-lib",
                        ],
                        external : [
                            "org.slf4j:slf4j-api:2.0.0",
                        ],
                        platform : {
                            main : {
                                external : [
                                    "junit:junit:4.13.2",
                                ]
                            }
                        }
                    }
                /main>
            ]
        /app>
    ]
/xapi-schema>
```

Supported keys:

- `internal`  
  Internal dependencies within the same schema project, usually referencing module names (optionally with platform like `plat:mod`).

- `project`  
  Dependencies on other schema projects, by name or Gradle path (e.g. `"producer2"`, `":producer2"`), plus optional coordinates specifying target platform/module.

- `external`  
  Dependencies on external artifacts, usually in `group:artifact:version` form (with optional classifier).

- `platform`  
  A platform-specific map:

```xapi
platform : {
    main : {
        external : [ "g:a:v" ],
        project  : { "otherProj" : "main:api" },
        internal : [ "impl", "main:api" ],
    },
    jre : {
        external : [ "g:jre-artifact:1.0" ]
    }
}
```

This allows per-platform overrides and fine-grained control.

### 4.2 Application order and scoping

Conceptually, the resulting dependencies for a `[project, platform, module]` node are the union of:

1. Project-level `requires` that apply to all platforms/modules, unless specifically constrained by `platform:{...}` or `module:{...}`.
2. Platform-level `requires` that apply to all modules on that platform, unless constrained by `module:{...}`.
3. Module-level `requires` defined at the global schema root.
4. Module-level `requires` defined under a platform’s `modules` block (only for that platform).

The intended rules:

- Where `platform : { ... }` is present, its entries apply **only** to the named platforms.
- Where `module : { ... }` is used (future extension), its entries would apply only to specific modules.
- Annotations on expressions (like `@transitive`, `@closure(...)`) configure how these dependencies behave, but do not change **where** they apply.

---

## 5. Liveness and Realization

Even though the schema describes the full universe of possible graph nodes, not all of them should become real Gradle modules.

A `[project, platform, module]` node should be considered **live** (and hence materialized) if:

1. It has source code or resources on disk associated with that `[plat, mod]` (e.g. `src/<platMod>/java`, `src/<platMod>/resources`, or legacy `src/main/java` for single-platform cases), **or**
2. It declares at least one explicit dependency (`internal`, `project`, `external`) in `schema.xapi`, **or**
3. It is reachable from some other live node via dependency edges (forward reachability in the graph built from `requires` and `includes`).

Additionally:

- Nodes that have only implicit, inherited structure and never appear in any source or dependency graph should remain **dead** and not be realized.
- Being referenced **only by dead nodes** does not make a node live.

This implies a two-phase approach:

1. Build the complete graph (all nodes + edges).
2. Compute liveness (sources / own deps as seeds, then propagate through edges).
3. Use liveness to decide which Gradle modules (and which Gradle projects) to actually create.

The filesystem index (`build/xindex`) is the canonical storage for this graph and its liveness marks; the in-memory `SchemaIndexReader` is a *client* that reads and caches this information.

---

## 6. Index and Reader: Conceptual Contract

The **index** is a whole‑build artifact that:

- Lists *all* `[project, platform, module]` nodes,
- Records all edges between them (internal, project, external),
- Records liveness marks for each node based on the rules above,
- Optionally records metadata like “multiplatform”, “virtual”, and “published”.

The **SchemaIndexReader** should:

- Load the index from disk *once*, and then answer queries from an in-memory cache:
  - `hasEntries(buildCoords, projectName, platform, module)`: “is this node live / does it participate in the graph?”
  - `isMultiPlatform`, `isPublished`, `isVirtual` etc.
- Prefer the on-disk liveness state over ad-hoc in-memory heuristics.
- Be usable by *any* settings or included build, even when only the root build computed the index.

The `settings` plugin (`XapiSettingsPlugin`) then becomes a client that:

- Uses `SchemaIndexReader` to decide which schema projects / modules should become Gradle projects,
- Wires up generated `build.gradle` files and dependencies based solely on the indexed graph and liveness.

---

## 7. Desired Behavior Summary

Putting it all together:

1. **Schema as truth**  
   `schema.xapi` describes the logical world: projects, platforms, modules, and their dependencies.

2. **Full graph, lazy realization**  
   The full `[project,plat,mod]` graph and all edges are established before any Gradle projects are created.

3. **Liveness from sources and edges**  
   A node is live if it has:
   - Code for that plat/mod, or
   - Its own explicit dependencies, or
   - Is needed transitively by some other live node.

4. **Pruning**  
   Nodes with no sources, no own deps, and reachable only from dead nodes remain dead and are not realized.

5. **Index first, clients later**  
   The root build writes a complete index once. All other consumers (including included builds) read this index as clients, without recomputing the world.

6. **Reader as cache**  
   `SchemaIndexReader` reads from disk and holds an in-memory cache, but never invents liveness on its own; it only interprets what the index provides.

7. **User expectations**  
   - Declaring dependencies in `schema.xapi` is enough to describe how all projects and modules relate.
   - If a developer adds either:
     - sources under an appropriate `src/<platMod>/...` path, or
     - explicit dependencies in `requires` for a given module,
     then that module will be realized and wired into the build automatically.

This is the target semantics that future implementation changes should strive to match.