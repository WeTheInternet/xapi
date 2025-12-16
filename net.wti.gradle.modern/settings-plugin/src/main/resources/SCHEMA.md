# Xapi Gradle Schema (`schema.xapi`) Specification

This document describes the structure of `schema.xapi` (and related `.xapi` schema files) as they are *actually* consumed by `XapiSchemaParser` and friends.

It is derived from:

- The root `schema.xapi` files in the repo (`schema.xapi`, `test-project/schema.xapi`, `ui/ui.xapi`, `core/core.xapi`, `inject/inject.xapi`)
- The visitors used in `XapiSchemaParser`

---

## 1. High-level shape

Every schema file is expected to have a single root element:

- Root element: `xapi-schema`

Conceptually:

- There is a **project-level** schema (`xapi-schema`) that:
    - Describes child projects (`projects`)
    - Describes platforms (`platforms`)
    - Describes modules (`modules`)
    - Describes dependencies (`require` / `requires`)
    - Describes external repos & artifacts

`XapiSchemaParser` does **two** main passes:

1. Parse the root `<xapi-schema ...>` as a “schema file” (`parseSchemaElement`)
2. Per-project / child schema, parse an `<xapi-schema ...>` or project-level element as a “project schema” (`parseProjectElement`), using the same attribute rules.

---

## 2. Attribute naming: `require` vs `requires`

### Where `require` / `requires` are accepted

The parser currently accepts both singular and plural, in several places:

- On the root `xapi-schema` element:
    - Attributes: `require`, `requires` → `addRequires(...)`
- On `modules` elements:
    - Attributes: `require`, `requires` → per-module dependency map
- On `platforms` elements:
    - Attributes: `require`, `requires` → platform-level module dependencies

### Semantics

- `require` and `requires` are treated identically:
    - Both are passed into the same code path (`addRequires(...)` or `insertModuleRequires(...)`)
    - Both can use the same JSON structures (see **§5 Requires / Dependencies**)

### Should everything be `require`?

From the current behavior & ergonomics:

- In code, “requires” matches the data-structure semantics better:
    - `requires = { internal: [...], project: [...], external: [...] }`
- The parser already supports both; changing everything to `require` only would:
    - Break backwards compatibility for existing `.xapi` files
    - Remove a minor-but-useful convenience (`requires` reads more naturally for sets/maps)

**Recommended convention:**

- **Use `requires` as the *preferred* spelling** in new schema files
- Treat `require` as a convenience alias (still fully supported)
- Document both explicitly (as in this spec), but nudge examples toward `requires`

This gives clarity without needing to break anything.

---

## 3. Root element: `<xapi-schema ...>`

### 3.1. Required structure

- File must parse to a root UI container named `xapi-schema`
- If the first container is **not** named `xapi-schema`, parsing fails

Example (informal, using ````` instead of backticks):

```
<xapi-schema
name = "rootProject"
group = "net.wti"
version = "1.0.0"
multiplatform = true
platforms = [...]
modules   = [...]
projects  = [...]
/xapi-schema>
```

### 3.2. Attributes on `<xapi-schema>`

All of the following are handled by `readProjectAttributes(...)`:

- `multiplatform` (boolean string)
    - Marks current project as explicitly multi-platform
    - `metadata.setExplicitMultiplatform(true)` if `"true"`
    - Also affects defaults for child `projects` if they inherit settings
- `virtual` (ignored here; validated as “simple value” only)
- `parentPath`
    - Ignored at this level; used on child `projects` for path derivation
- `shortenPaths`
    - Placeholder; reserved to control whether multi-platform gets extra parent project segments
- `applyTemplate`
    - Placeholder for future “copy from named template” behavior
- `templates`
    - Placeholder; for defining named templates without applying them
- `repos` / `repositories`
    - Value forwarded to `addRepositories(...)`
- `platforms`
    - Value forwarded to `addPlatforms(...)`
- `modules`
    - Value forwarded to `addModules(...)`
- `projects`
    - Value forwarded to `addProjects(...)`
- `external`
    - Value forwarded to `addExternal(...)`
- `defaultRepoUrl`
    - Value forwarded to `addDefaultRepoUrl(...)` → `metadata.setDefaultUrl(...)`
- `schemaLocation`
    - Value forwarded to `addSchemaLocation(...)` → `metadata.setSchemaLocation(...)`
- `name`
    - Sets project logical name → `metadata.setName(...)`
- `group`
    - Sets group id → `metadata.setGroup(...)`
- `version`
    - Sets version → `metadata.setVersion(...)`
- `require` / `requires`
    - Project-level dependency declarations
    - Passed to `addRequires(metadata, PlatformModule.UNKNOWN, expr)`
    - Used primarily for project-level dependency indexing and liveness graph
- `inherit` (boolean)
    - Controls whether schema inherits things from parent metadata
- `description`
    - Freeform description string → `metadata.setDescription(...)`
- `path`
    - Disk path / relative file system path → `metadata.setDiskPath(...)`

Anything else is rejected with:

- `"Attributes named X are not (yet) supported"`

---

## 4. Structural components

The main “schema components” are:

- Repositories (`repos` / `repositories`)
- Platforms (`platforms`)
- Modules (`modules`)
- Projects (`projects`)
- External / preload artifacts (`external`)
- Default repository URL (`defaultRepoUrl`)
- Generated schema location (`schemaLocation`)
- Dependencies (`require` / `requires`)

Each is described by a single attribute whose value can be:

- A JSON-like container (`[...]` array or `{...}` map)
- A UI element (`<something ... />`)
- A name / string (`foo`, `"foo"`)

The parser uses `ComposableXapiVisitor` to flexibly handle multiple shapes.

---

## 4.1. Repositories (`repos` / `repositories`)

Attribute: `repos` or `repositories`

Handler: `addRepositories(DefaultSchemaMetadata, Expression)`

Supported forms:

1. **Array of UI elements**

   ```
   repositories = [
   <maven url="https://repo1.maven.org/maven2" name="mavenCentral" />,
   <maven url="https://my.company/repo" name="corpRepo" />
   ]
   ```

    - For each UI container (`<name .../>`):
        - `meta.addRepositories(el)`
    - Later, `loadRepositories(...)` will interpret each `<repoElement>`:
        - If `name="literal"` → treat `value` attr as literal Gradle snippet
        - Otherwise expect `<name url="..."/>` only

2. **Array of JSON-compatible “method calls” or names**

    - Method call:

      ```
      repositories = [
      mavenCentral(),
      jcenter()
      ]
      ```

        - Each `mavenCentral()` or `jcenter()` becomes:
            - `<literal value=mavenCentral() />`
            - Stored via `meta.addRepositories(expr)`

    - String / name:

      ```
      repositories = [
      "https://repo1.maven.org/maven2",
      "https://my.company/repo"
      ]
      ```

        - Each plain string / name becomes:
            - `<maven url="URL" name="host-name" />`
            - Via `nameOrString(...)` transformer

3. **Non-array JSON map**

    - Currently *not* supported; parser throws:
    - `"{Json: containers} are not supported, instead use <literal value='maven {...}' />"`

**Runtime loading:**

- `loadRepositories(map, project, metadata)` iterates `metadata.getRepositories()`
- For each repo:
    - If `literal`:
        - `value` attribute is arbitrary text to hand to Gradle
    - Else:
        - Require exactly one attribute `url="..."` on that element

---

## 4.2. Platforms (`platforms`)

Attribute: `platforms`

Handlers:

- Parsing: `addPlatforms(DefaultSchemaMetadata, Expression)`
- Loading into project: `loadPlatforms(SchemaProject, DefaultSchemaMetadata, parser, explicitPlatform)`

### 4.2.1. Accepted shapes for `platforms = ...`

1. **Array of UI elements**:

   ```
   platforms = [
     <main />,
     <jre replace = "main" />,
     <gwt replace = "main" published = true />
   ]
   ```

    - Each `<platformName ... />` is added:
        - `meta.addPlatform(el)`

2. **JSON map from name to replace / settings**

   ```
   platforms = {
   main: "",
   gwt: "main",
   jre: "main"
   }
   ```

    - For each `name: replaceName` pair:
        - Construct `<name replace="replaceName" />` (if value non-empty)
        - `meta.addPlatform(expr)`

3. **Single UI container / name / literal**

    - Single element:

      ```
      platforms = <main />
      ```

    - Or name / string:

      ```
      platforms = "main"
      platforms = main
      ```

    - Each case yields `<main />` added as a platform.

If the expression doesn’t match these shapes, an `InvalidSettingsException` is thrown.

### 4.2.2. Per-platform attributes (when loading into `SchemaProject`)

When `loadPlatforms(...)` runs, it processes each `UiContainerExpr platform`:

- `name` = `platform.getName()`
- Skip non-default names for single-platform projects
- Attributes:

    - `published` (boolean)
        - Defaults:
            - `true` for `main` platform
            - `false` for others
    - `needSource` (boolean)
        - Default `true` for `"gwt"`
    - `publishSource` (boolean)
        - Default `true` for `"gwt"`
    - `test` (boolean)
        - Default `false`
    - `replace` / `replaces`
        - Accepted as names (or list of names)
        - Currently code asserts you only replace at most **one** other platform
        - Stored as `SchemaPlatform.replace`
    - `requires` / `require`
        - Platform-level dependencies
        - Applied via `insertModuleRequires(platformName, ...)` for *all* modules in that project
    - `modules`
        - Platform-specific module configuration (see **§4.3.3**)

Each parsed platform is turned into a `SchemaPlatform` (usually a `DefaultSchemaPlatform`) and inserted via `insertPlatform(...)`.

Later, after loading:

- If `explicitPlatform` is set:
    - `project.trimPlatforms(explicitPlatform)`
- `publishChildren` / `publishChildrenSource` propagate publishing to replaced platforms

---

## 4.3. Modules (`modules`)

Attribute: `modules`

Handlers:

- Parsing: `addModules(DefaultSchemaMetadata, Expression, In1<UiContainerExpr> massager)`
- Loading: `loadModules(SchemaProject, DefaultSchemaMetadata, parser)`

### 4.3.1. Accepted shapes

1. **Array of UI containers and / or names / strings**

   ```
   modules = [
     <main requires = [ api, spi ] />,
     <sample requires = "main" published = true />,
     <testTools requires = main published = true />,
     <test requires = ["sample", testTools] />
   ]
   ```

    - For each `UiContainerExpr el`:
        - `meta.addModule(el.getName(), el)`
    - For each name / string `x`:
        - Synthesized `<x />` and added as module

2. **JSON map from module name to `requires` value**

   ```
   modules = {
   main: "",
   api: "",
   spi: "main",
   test: "main"
   }
   ```

    - Each entry `name: requireExpr`:
        - Build `<name requires=requireExpr />`
        - Apply massager hook
        - `meta.addModule(name, expr)`

3. **Single UI container**

   ```
   modules = <main requires = [api, spi] />
   ```

4. **Single name / string**

   ```
   modules = "main"
   modules = main
   ```

    - Synthesized `<main />` module.

Non-matching shapes raise `InvalidSettingsException`.

### 4.3.2. Per-module attributes when loading

In `loadModules(...)`, each module element is turned into a `SchemaModule`:

- `name` = `module.getName()`
- Skip modules not matching default module name in single-platform projects
- Attributes:

    - `published` (boolean)
        - Default: `true` for module `main`, false otherwise (but this can change if platform-specific overrides publish)
    - `test` (boolean, default `false`)
    - `force` (boolean, default `false`)
    - `requires` / `require`
        - Module-level dependencies for this module across platforms (see **§5**)
    - `include` / `includes`
        - “Includes” list; implies strong internal dependencies (see **§4.3.4**)
        - Names resolved per platform, yield internal deps
    - `forPlatform`
        - Used when modules come from `platform.modules` override:
            - That module config applies only to that single platform
            - e.g. j2cl-only module layout

Other attributes are preserved for later processing (e.g. `forPlatform`).

### 4.3.3. Platform-specific `modules` block

On a `<platform>` element, you may specify:

```
<platform
name = "j2cl"
modules = [
<jszip requires = main published = true />,
<externs />
]
/>
```

Mechanism:

- `loadPlatforms(...)` sees a `modules` attribute on a platform
- Calls `extractModuleForPlatform(platformName, metadata, modulesAttr, platformPublished)`
    - Internally calls `addModules(metadata, expr, massager)` with:
        - A massager that:
            - Forces `published = true` if the platform is published and module didn’t declare `published`
            - Adds `forPlatform="platformName"` to each module
- Later, `loadModules(...)` uses `forPlatform` to apply module-specific requires / includes into the right platform.

### 4.3.4. `include` / `includes` (module includes)

- On each module `<moduleName ...>`:

    - Attributes:
        - `include = expr`
        - `includes = expr`
    - The provided expr is interpreted as a list of *module names* to be “included”:
        - Similar to always-required internal dependencies
    - Parsing:
        - Collects names from the expr with `extractNamesAndTypes(...)`
        - Adds them to `SchemaModule.include` set
        - For each included module name, synthesizes an internal `requires = { internal: [ "includedName" ] }` on the *same platform*

- Implementation detail:
    - `insertModuleIncludes(...)`:
        - Clones `includes` attribute into a synthetic `requires` JSON structure
        - Calls `insertModuleRequires(...)` to hook it into the dependency maps

---

## 4.4. Projects (`projects`)

Attribute: `projects`

Handler: `addProjects(DefaultSchemaMetadata, Expression, DefaultSchemaMetadata ancestor)`

Purpose:

- Describe **child projects** below this schema
- Each child project is a `UiContainerExpr` with attributes like:
    - `name`
    - `path`
    - `multiplatform`
    - `virtual`
    - Possibly its own `platforms`, `modules`, etc. (via another `xapi-schema`)

### 4.4.1. State toggles inside `projects`

`addProjects` maintains two booleans:

- `multiplatform[0]`
- `virtual[0]`

These default to:

- `multiplatform[0] = meta.isMultiPlatform()` (or explicit multi-platform)
- `virtual[0] = false`

Within the `projects` JSON map, keys can set modes that apply to subsequent entries:

- `"multiplatform"` / `"multi"`:
    - Children default to multiplatform
- `"standalone"` / `"single"` / `"singleplatform"`:
    - Children default to single-platform
- `"virtual"`:
    - Children are **virtual**:
        - They exist only as logical bubble; no physical disk project necessarily

### 4.4.2. Accepted shapes

1. **Array of UI containers or names**

   ```
   projects = [
   <consumer />,
   <producer1 />,
   "producer2"
   ]
   ```

    - Each element is interpreted as a child project
        - If an element is a name / string, a synthetic `<name />` is created

2. **JSON map with special keys**

   Example:

   ```
   projects = {
   // mode keys
   multiplatform : [
   <consumer />,
   <ui multiplatform=false />
   ],
   virtual : [
   "tools",
   "templates"
   ],
   single : [
   <docs />,
   "samples"
   ]
   }
   ```

    - For each `JsonPair`:
        - If `key` is one of the mode words, adjust `multiplatform[0]` / `virtual[0]` and recurse into its value
        - Else error

3. **Direct `UiContainer` or simple name from a JSON array position**

    - If key is an integer (array index), treat value as project entry:
        - Accept `UiContainer`
        - Or a name / string, converted to `<name />`

### 4.4.3. Per-project derived attributes

When actually adding a project (`addProject` closure):

- Ensure the following derived attributes are present if `inherit` is true (default):

    - `virtual`
        - Defaults from current `virtual[0]` if not set
    - `multiplatform`
        - If `explicitMultiplatform` on parent is true → force child's `multiplatform = true`
        - Else default from `multiplatform[0]`

- `parentPath`
    - Computed from `meta.getRoot().getPath()` and `meta.getPath()`
    - If non-empty, set as `parentPath` attribute

The resulting `UiContainerExpr` is added to `metadata.projects` (`ListLike<UiContainerExpr>`).

---

## 4.5. External dependencies (`external`)

Attribute: `external`

Handler: `addExternal(DefaultSchemaMetadata, Expression)`

Supported form:

- `external = [ <preload ... />, <otherElements...> ]`

Behavior:

- Parser expects a JSON array
- Each `UiContainerExpr` is `meta.addExternal(el)`

Known element:

- `<preload ... />`
    - Special semantics:
        - Ahead-of-time resolution of external dependencies into local repo
        - `loadExternals` currently just recognizes and skips it
    - Typical structure (informal):

      ```
      external = [
      <preload
      name        = "gwt"
      url         = "https://wti.net/repo"  // optional if defaultRepoUrl set
      version     = "2.8.0"                 // optional; can be per-artifact
      platforms   = [ "gwt" ]              // optional; limit by platform
      modules     = null                   // optional; limit by module
      inherited   = true                   // optional, default true
      artifacts   = {
      "com.google.gwt" : [
      "gwt-user",
      "gwt-dev",
      "gwt-codeserver"
      ]
      }
      /preload>
      ]
      ```

Other element names are permitted; `loadExternals` just switches on name and currently only treats `preload` specially.

---

## 4.6. Default repository URL (`defaultRepoUrl`)

Attribute: `defaultRepoUrl`

Handler: `addDefaultRepoUrl(DefaultSchemaMetadata, Expression)`

Accepted forms:

- Name / string:

  ```
  defaultRepoUrl = "https://wti.net/repo"
  ```

- Method call (no args):

  ```
  defaultRepoUrl = companyRepo()
  ```

Validation:

- If it’s a method call expression, it must take **no arguments** or parser throws.

Stored as:

- `metadata.setDefaultUrl(String)` (name, string or method call text)

---

## 4.7. Generated schema location (`schemaLocation`)

Attribute: `schemaLocation`

Handler: `addSchemaLocation(DefaultSchemaMetadata, Expression)`

Accepted forms:

- Name / string only:

  ```
  schemaLocation = "build/schema.gradle"
  ```

Stored via:

- `metadata.setSchemaLocation(String)`

---

## 5. Requires / dependencies (`require` / `requires`)

There are **three** layers where dependencies are declared:

1. Project-level (`xapi-schema` attribute: `require` / `requires`)
2. Module-level (`modules`’ module elements: `require` / `requires`)
3. Platform-level (`platform` elements: `require` / `requires`)

Internally everything ends up building:

- `metadata.getDepsProject()`
- `metadata.getDepsInternal()`
- `metadata.getDepsExternal()`

Key types:

- `DependencyType`:
    - `project`, `internal`, `external`, `unknown`
- `PlatformModule`:
    - Combined `platform:module` coordinate
    - Defaults to `"main:main"` or context-derived

### 5.1. `addRequires` – high-level `requires` parsing

`addRequires(DefaultSchemaMetadata meta, PlatformModule platMod, Expression expr)`:

- Expects the expression to be JSON-like
- Visits:

    - `JsonContainerExpr` / `JsonArrayExpr`
    - `JsonPairExpr` pairs

- For each pair:

    - If key is integer (array index):
        - Equivalent to `project` dependencies:
            - `requires = [ "a", "b" ]` is same as `requires = { project: ["a", "b"] }`
            - Implementation: `meta.addDependency(DependencyType.project, platMod, pair)`
    - Else key is string:
        - Attempts to parse key as `DependencyType` prefix:
            - `"project"` → `DependencyType.project`
            - `"internal"` → `DependencyType.internal`
            - `"external"` → `DependencyType.external`
            - `"project_gwtMain"` style:
                - Key starts with a type name, suffix is parsed as `PlatformModule` coordinate (e.g., `_gwtMain`)
        - `meta.addDependency(t, platMod.edit(null, coord), pair)` stores expression for later extraction.

### 5.2. Shapes for `requires`

The key shapes used everywhere are:

1. **Simple arrays per type**

   ```
   requires = {
   project  : [ "subProjectA", "subProjectB" ],
   internal : [ "api", "impl" ],
   external : [ "group:artifact:version", "org.junit:junit:4.13" ]
   }
   ```

2. **Map form for type + platform/module**

   ```
   requires = {
   external : {
   "com.google.gwt:gwt-dev:2.8.0" : "main:main",
   "com.google.gwt:gwt-user:2.8.0" : "gwt:api"
   }
   }
   ```

3. **Platform-specific dependencies**

    - On a `module.requires`:

      ```
      requires = {
      platform: {
      jre8: {
      external: [ "tld.ext:name-api:1.0", "tld.ext:name-spi:1.0" ],
      project : [ "projA", "projB" ],
      internal: [ "api", "impl" ]
      },
      jre11: {
      @Version("2.0")
      @Group("tld.ext")
      external: [ "name-api", "name-spi" ]
      }
      }
      }
      ```

    - `insertModuleRequires` handles:
        - `platform: { jre8: {...}, jre11: {...} }`
        - `module: { api: {...}, main: {...} }` (if used)

4. **Implicit arrays for simple “just internal modules”**

   ```
   requires = [ "api", "spi" ]
   ```

    - Treated as `internal` dependencies in `insertModuleRequires`:
        - Key is integer → `internal` bucket.

### 5.3. `insertModuleRequires` – modules & platforms

`insertModuleRequires(String platformName, DefaultSchemaMetadata metadata, UiAttrExpr attr, In1<In1<SchemaModule>> moduleSource)`:

- For each affected module (via `moduleSource`):
    - Build a `PlatformModule(platName, moduleName)`
    - Visit the `requires` attribute expression:
        - `jsonPair` with key:

            - `"project"` / `"internal"` / `"external"` / `"unknown"`:
                - Put into `metadata.getDepsProject()/Internal/External().get(platformModule).add(valueExpr)`
            - `"platform"`:
                - Value must be `{}` map
                - For each nested `platformName: {...}`:
                    - Temporarily set `platValue = thatPlatform`
                    - Re-run visitor on inner `{...}`
            - `"module"`:
                - Value must be `{}` map
                - For each nested `moduleName: {...}`:
                    - Temporarily set `modValue = thatModule`
                    - Re-run visitor on inner `{...}`
            - integer index:
                - Shorthand for `internal` dependency; uses `metadata.getDepsInternal()`

This allows very compact, composable definitions like:

```
<main
  requires = {
    project : [ "subProject" ],
    internal: [ "api", "spi" ]
  }
/>
```

or more elaborated platform-specific cases.

### 5.4. Extracting runtime `SchemaDependency` objects

`extractDependencies(DependencyMap, Expression expr, ...)` takes each stored `Expression` and computes fully qualified `SchemaDependency` entries:

- Interprets:
    - `@Group`, `@Version`, `@Transitive`, `@Category`, etc. annotations on expressions
    - Splits external strings `g:n:v(:c?)`
    - Handles `platformModule` encoding (`"gwt:api"` / `"main:main"` / etc.)

Resulting `SchemaDependency` objects are:

- Bound to an `IndexNode` per project/module/coordinate
- Used for:
    - Liveness graph
    - Module-to-module dependency inclusion
    - External dependency markers

---

## 6. Liveness & indexing (brief)

While not the main topic here, note:

- During `loadDependencies(...)`, each `SchemaDependency` is also mapped to an `IndexNode`:
    - `consumerNode` for the dependent module
    - `projectDepNode` / `internalDepNode` / external liveness for dependency
- Liveness reasons:
    - `is_dependency` / `has_dependencies`
- For project dependencies:
    - The dependency chain is also realized via `consumerNode.include(projectDepNode)`

This is where the `requires` structure defines which modules are “live” and consumed when building the XIndex.

---

## 7. Opinionated conventions

Given the above:

- **Prefer `requires` syntax** for anything that’s a collection or JSON-like map
- Keep `require` as permissive synonym
- Use these patterns consistently:

    - For `platforms`:

      ```
      platforms = [
        <main />,
        <jre replace="main" />,
        <gwt replace="main" published=true />
      ]
      ```

    - For `modules`:

      ```
      modules = [
      <api />,
        <main requires = [ api ] published = true />,
        <test requires = { internal: [ "main" ] } test = true />
      ]
      ```

    - For `projects`:

      ```
      projects = {
      multiplatform : [
      <consumer />,
      <producer1 />
      ],
      single : [
      <docs />,
      "samples"
      ]
      }
      ```

    - For `external`:

      ```
      external = [
      <preload
      name      = "gwt"
      version   = "2.8.0"
      platforms = [ "gwt" ]
      artifacts = {
      "com.google.gwt" : [ "gwt-user", "gwt-dev", "gwt-codeserver" ]
      }
      /preload>
      ]
      ```

---

## 8. Experimental `.xapi` schema for describing schemas (`xapi-parser`)

Below is a speculative `.xapi` format for a “schema-of-schema” — an `xapi-parser` that describes the valid structure of `schema.xapi` files themselves.

It uses the same `xapi` style, but defines meta-elements like `named-elements`, `config-map`, etc.

```
<xapi-parser
name = "schema-xapi"
version = "0.1"

elements = [
<xapi-schema
description = "Root of a Gradle schema.xapi file."

      attributes = {
        name            : <string   required = false />,
        group           : <string   required = false />,
        version         : <string   required = false />,
        multiplatform   : <boolean  required = false />,
        virtual         : <boolean  required = false />,
        parentPath      : <string   required = false />,
        shortenPaths    : <boolean  required = false />,
        applyTemplate   : <string   required = false />,
        templates       : <config-map
                             keyType   = "string"
                             valueType = "xapi-schema"
                             description = "Named, reusable template blocks."
                           />,
        repos           : <repositories />,
        repositories    : <repositories />,
        platforms       : <platforms />,
        modules         : <modules />,
        projects        : <projects />,
        external        : <externals />,
        defaultRepoUrl  : <stringOrMethod />,
        schemaLocation  : <string   required = false />,
        require         : <requires />,
        requires        : <requires />,
        inherit         : <boolean  required = false />,
        description     : <string   required = false />,
        path            : <string   required = false />
      }

      elements = {
        // nested xapi-schema for child projects
        xapi-schema : <xapi-schema />
      }
    /xapi-schema>,

    <repositories
      description = "Repository declarations: ui elements, method calls, or urls."
      value = {
        list : [
          <repositoryElement />,
          <methodCall />,
          <string />
        ]
      }
    /repositories>,

    <repositoryElement
      description = "Structured repository element like <maven url='...' name='...'>."
      attributes = {
        name : <string required = true />,
        url  : <string required = true />
      }
/repositoryElement>,

    <platforms
      description = "Platform list or map."
      oneOf = [
        <json-array  element = <platformElementOrName /> />,
        <json-object entry   = <platformMapEntry />   />,
        <platformElement />,
        <string />
      ]
    /platforms>,

    <platformElementOrName
      oneOf = [
        <platformElement />,
        <string />
      ]
    /platformElementOrName>,

    <platformElement
      description = "A single platform, e.g. <gwt replace='main' published=true />."
      attributes = {
        published      : <boolean />,
        needSource     : <boolean />,
        publishSource  : <boolean />,
        test           : <boolean />,
        replace        : <stringOrList />,
        replaces       : <stringOrList />,
        requires       : <requires />,
        require        : <requires />,
        modules        : <modules />
      }
/platformElement>,

    <modules
      description = "Module list, map, or single name."
      oneOf = [
        <json-array  element = <moduleElementOrName /> />,
        <json-object entry   = <moduleMapEntry />       />,
        <moduleElement />,
        <string />
      ]
    /modules>,

    <moduleElementOrName
      oneOf = [
        <moduleElement />,
        <string />
      ]
    /moduleElementOrName>,

    <moduleElement
      description = "A single module, e.g. <main requires=[api,spi]/>."
      attributes = {
        published   : <boolean />,
        test        : <boolean />,
        force       : <boolean />,
        requires    : <requires />,
        require     : <requires />,
        include     : <stringOrList />,
        includes    : <stringOrList />,
        forPlatform : <string />
      }
/moduleElement>,

    <moduleMapEntry
      description = "Map entry: moduleName : requiresExpr."
      key   = <string />,
      value = <requiresOrEmpty />
    /moduleMapEntry>,

    <projects
      description = "Child projects; supports mode keys multiplatform/single/virtual."
      value = {
        oneOf = [
          <json-array element = <projectElementOrName /> />,
          <json-object
            entry = <projectModeEntry />
          />
        ]
      }
    /projects>,

    <projectElementOrName
      oneOf = [
        <projectElement />,
        <string />
      ]
    /projectElementOrName>,

    <projectElement
      description = "Child project container."
      attributes = {
        name         : <string required = false />,
        multiplatform: <boolean />,
        virtual      : <boolean />,
        parentPath   : <string />
      }
      elements = {
        xapi-schema : <xapi-schema />
      }
/projectElement>,

    <projectModeEntry
      description = "Map key in projects: multiplatform, single, virtual."
      keyOneOf = [ "multiplatform", "multi", "single", "standalone", "singleplatform", "virtual" ]
      value    = <json-array element = <projectElementOrName /> />
    /projectModeEntry>,

    <externals
      description = "External preload declarations and other external specs."
      value = <json-array element = <externalElement /> />
    /externals>,

    <externalElement
      description = "External declarations; currently only <preload> is special."
      oneOf = [
        <preload />,
        <ui-container name='*' />
      ]
    /externalElement>,

    <preload
      description = "Preload external artifacts into local repo."
      attributes = {
        name       : <string required = true />,
        url        : <string required = false />,
        version    : <string required = false />,
        platforms  : <stringOrList required = false />,
        modules    : <stringOrList required = false />,
        inherited  : <boolean required = false />
      }
      elements = {
        artifacts : <config-map
          keyType   = "string"   // groupId
          valueType = "stringOrList"  // artifactIds
        />
      }
/preload>,

    <requires
      description = "Dependency map used by projects, modules, and platforms."
      oneOf = [
        // Simple internal list: [ 'api', 'spi' ]
        <json-array  element = <stringOrNamedElement /> />,
        // Typed map: { project: [...], internal:[...], external:[...] }
        <json-object entry = <requiresEntry /> />
      ]
    /requires>,

    <requiresEntry
      description = "One key in a requires map."
      keyOneOf = [
        "project", "internal", "external", "unknown",
        "platform", "module"
      ]
      value = <json-arrayOrObject />
    /requiresEntry>,

    <stringOrNamedElement
      oneOf = [
        <string />,
        <ui-container name='*' />
      ]
    /stringOrNamedElement>,

    <stringOrList
      oneOf = [
        <string />,
        <json-array element = <string /> />
      ]
    /stringOrList>,

    <stringOrMethod
      oneOf = [
        <string />,
        <methodCall />
      ]
    /stringOrMethod>
]
/xapi-parser>
```

This hypothetical `xapi-parser` could, in principle:

- Validate `schema.xapi` structures
- Enforce attribute shapes and allowed keys
- Generate a strongly-typed object graph from a schema-of-schema definition

If you like this shape, it can be iterated into a real meta-schema engine that:

- Reads such `xapi-parser` definitions
- Emits:
    - Visitor configurations
    - Object models
    - Validators
    - Possibly even Gradle DSL stubs
