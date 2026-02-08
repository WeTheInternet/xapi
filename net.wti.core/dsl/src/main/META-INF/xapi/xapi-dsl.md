# xapi-dsl: DSL Definition DSL (Roadmap)

This document describes the **xapi DSL-definition language** (the “dsl-dsl”):
how we define DSL schemas using `<xapi-dsl/>`, how we describe types, and how we
normalize flexible xapi AST inputs into a predictable, order-preserving DSL object graph.

The goals are:

- **Terse authoring** of schemas (minimal padding / wrappers).
- **Strict types**, with clearly defined coercions and syntactic sugar.
- **Additive semantics**: repeated declarations add to the model (especially collections).
- **Order preservation**: generated output (e.g. Gradle) should preserve user intent order.
- **Good diagnostics**: every normalized DSL node retains references to source AST nodes.

---

## 1. Key concepts and layers

### 1.1 AST layer (input)
The source is parsed into xapi AST nodes (`UiContainerExpr`, `JsonContainerExpr`, etc).
The AST is permissive and may contain:

- json arrays, json objects/maps
- repeated attribute names (effectively “multi-map” semantics)
- annotations on expressions
- mixed value forms (string, name, element, map, array)

### 1.2 Shape / Type layer (schema)
A DSL schema declares:

- elements (what tags exist, attributes, children)
- attribute value shapes (type expressions)
- normalization rules and validation constraints

Types are described using **typed invocations** (method-call-like constructors).

### 1.3 Normalized DSL graph layer (output)
We normalize AST into a predictable object model:

- everything important becomes **lists of entries** (order-preserving)
- ambiguity in surface syntax is flattened
- every produced node retains source AST references for diagnostics

This normalized graph is what generators and validators consume.

---

## 2. Core runtime model types

### 2.1 DslObject vs DslNode (conceptual)
We expect the runtime model to include:

- `DslObject`: a “rich object derived from AST”, generally storing:
    - `sources: List<AstNode>` (often multiple, due to templating / merging)
    - possibly annotations, provenance, and diagnostic helpers
- `DslNode`: a node in a normalized object graph (often immutable),
  typically composed of other `DslNode`s and `DslValue`s.

Note: keep interfaces until fields are needed; builders may be separate types.

### 2.2 Builders, analysis, and multi-file templating
We use a **2-phase** pipeline:

1. **Analyze phase** (AST -> builders)
    - A generated/procedural analyzer visits xapi AST and produces a strongly-typed root builder.
    - The analyzer collects validation errors while visiting and fails at the end (report all errors at once).
    - Builders may contain reference types, template links, and other “DSL conveniences”.

2. **Build phase** (builders -> immutable nodes)
    - `.build()` on a builder resolves templates/references and produces immutable nodes.
    - Immutable nodes should not contain unresolved references or “convenience” forms.
    - The build step performs final flattening / normalization into canonical structures.

Builders:
- Are created by visitors.
- May extend the base read-only interface they build (e.g. `FooBuilder extends Foo`).
- May store “gross union container” internal representations, but must expose a canonical read-only API.
    - In builders, canonical getters may compute lazy filtered views on demand.
    - In immutable nodes, canonical getters may snapshot builder-derived filtered views lazily.

### 2.3 AnalysisContext (parse-time context)
Parsing/analysis uses a `DslAnalysisContext` that is threaded through AST visitors.

It is responsible for:
- collecting errors (do not throw immediately; fail at end)
- providing per-file metadata (file name, display name, etc)
- resolving context-scoped references during analysis (not final resolution)

`DslAnalysisContext` supports parent chaining for template/type lookup:
- look up a key in the current context
- if absent, consult the parent, continuing upward
- stop at the nearest context that can resolve the key (shadowing: closest wins)

This reduces brittle “inherits=true/false” flags: composition is preferred.
If a child wants parent-like behavior, it can explicitly reuse the same template/use-directive.

### 2.4 BuildContext (freeze-time context)
Build/finalization uses a `DslBuildContext`.

It is responsible for:
- collecting build-time errors (unresolved refs, invalid merges, etc)
- resolving template/type references into final concrete nodes
- providing whole-document metadata if needed (root sources, logical name, rootDirectory, etc)

Recommended build API shape:
- `build()` creates a new `DslBuildContext`, runs `build(ctx)`, then throws if ctx has errors.
- `build(ctx)` is used internally for recursive building of child nodes.
  Any unresolved reference encountered during `build(ctx)` should record an error in the context.

### 2.5 Hook point for flattening / specialization (future)
Some DSLs may want custom “flattening mechanics” during build (e.g. special keys, magic defaults).

We may add (later):
- a `DslFlattener` utility (possibly hosted by `DslBuildContext`)
- a `DslBuildEvent` / interceptor mechanism allowing multiple actors to observe/adjust
  inputs/outputs during `build(ctx)`.

This is not required for the first implementation pass; add only when needed.

---

## 3. Type expressions (typeExpr)

Type expressions are authored using **typed invocations** (method-call-like forms).
They describe *what shapes are accepted at a location* and how to normalize them.

### 3.1 Primitive types
Primitive-ish types are strict:

- `string`: string literal; may also accept `NameExpr` as sugar in some contexts
- `bool`, `int`, `float` (as needed)
- `name`: a Java identifier only (no quotes, no dots, no `:`)
- `qualifiedName`: dot-separated identifiers (e.g. `com.foo.bar`)
- `namePair`: either `"a:b"` string literal or `{ a: b }` map form, both sides must be `name`

### 3.2 Element types
Elements are special:

- `element("typeName")`:
  a static-tag element of the named element-type
- `namedElement("typeName")`:
  a **self-keying** element whose *tag name provides the instance name*,
  validated against the element-type `"typeName"`

Elements always refer to element-types by name (string/name token),
not by `typeRef(...)`.

### 3.3 Composition constructors (typed invocations)

#### `many(T1, T2, ...)`
- Runtime DSL accepts a single item **or** an array of items.
- Multiple arguments are an **implicit union**:
  `many(A,B)` means “many of (A ∪ B)”.
- Normalizes to an ordered list of items.

#### `one(T1, T2, ...)`
- Runtime DSL accepts exactly one value.
- Multiple arguments are an **implicit union**:
  `one(A,B)` means “one of (A ∪ B)”.
- When only one type is provided, the generated API can expose that type directly.
- When multiple types are provided, the generated API exposes a tagged union / variant value.

(Note: there is no explicit `union(...)` invocation; unions are expressed by passing multiple types.)

#### `map(V1, V2, ...)`
- **Always modeled as a list of entries** (instruction-list semantics).
- Map keys are always string-ish (string literal or NameExpr sugar).
- Multiple value arguments are an implicit union.
- Map order is preserved as encountered in source.
- Repeated keys are allowed by default (see §4 for specialized map types).

#### `typedMap({ key: typeExpr, ... })`
- **Always modeled as a list of entries** (instruction-list semantics).
- Restricts key set to those declared.
- Each encountered entry validates value shape by its declared key type.
- Repeated keys are allowed; order is preserved.
- Canonical APIs should expose filtered lists per key.

#### `typeRef("aliasName")`
- References a previously defined named type alias.
- Intended for reuse at attribute/element locations.

---

## 4. Repeated keys, order, and map types

### 4.1 Ordered entry model (core rule)
Even when syntax looks like a “map”, we treat it as:

- `List<Entry(keyExpr, valueExpr)>` in encounter order

This supports:
- stable code generation ordering
- repeated keys
- better diagnostics (each entry points to its source)

**Important:** both `typedMap` and `map` are modeled as ordered entry lists.
They are never “last one wins” by default; last-one-wins is reserved for specialized map types.

### 4.2 Map type taxonomy (duplicate-key semantics)
We express duplicate-key behavior via **distinct map types** (not flags).

Baseline map types:
- `typedMap({ ... })`: always instruction-list semantics (repeatable; order-preserving).
    - Keys are restricted to the declared key set.
    - The runtime representation is a single ordered list of typed entries.
- `map(V1, V2, ...)`: always instruction-list semantics (repeatable; order-preserving).
    - Keys are string-ish (string literal or NameExpr sugar).
    - Values are one-of the provided value types (implicit union).

Optional specialized map types (for later):
- `uniqueMap(...)`: keys must be unique (duplicate key is an error).
- `mergeMap(...)`: duplicate keys require mergeable values (or error).
    - Adding elements returns the original value after calling `original.merge(newValue)`.
- `replaceMap(...)`: duplicate keys overwrite earlier values.
    - Return the new value.
    - If `newValue instanceof IsMergeable` then `newValue.merge(original)` is called.
- `firstMap(...)`: keep the first value, ignore later duplicates (return existing).

Note: `typedMap(...)` remains instruction-list semantics even if specialized maps are added.

---

## 5. Pair types (namePair and friends)

### 5.1 `namePair` (primitive)
`namePair` represents a pair of names: `(name, name)`.

Accepted surface syntaxes:
- String form: `"gwt:main"` (must be quoted; `gwt:main` is not valid Java/xapi syntax unquoted)
- Map form: `{ gwt: main }` (strict `name` value; key is string-ish/name sugar)

Normalization:
- both forms normalize into a `DslPair(name left, name right)` node
- sources include the original AST expression(s)

Rationale:
- `name` cannot contain `:`, so the delimiter form is safely “string channel”
- map form reinforces strictness: `{ platform: module }`

---

## 6. Normalization pipeline (high level)

For any schema location with a declared `typeExpr`:

1. **Analyze** into builders (accept “convenience forms” and references).
2. **Validate** shape constraints as early as possible (record errors; don’t throw until end of phase).
3. **Build** immutable nodes:
    - resolve templates and references
    - flatten list-ish/map-ish AST shapes into canonical ordered entry lists
    - snapshot any lazy filtered views (or keep Lazy that snapshots on first access)
4. **Preserve sources**:
    - each normalized node stores one or more source AST nodes
    - merged/templated nodes retain all contributing source ranges

---

## 7. Example: requiresType (sketch)

A common reusable type alias:

- `requiresType = typedMap({ ... })`
- elements use: `requires = typeRef("requiresType")`

For example (illustrative only):

- `internal: many(namePair)`
- `project: many(namePair, name)`
- `external: typeRef("externalType")` // too complex to inline

---

## 8. Implementation plan (ordered, detailed TODO list)

This section is the **actual implementation order** we intend to follow.

### 8.1 Step 1: Build the core `DslType` objects (schema-time model)
Goal: create the minimal type system required to represent DSL definitions and later drive parsing.

Focus types (first pass):
- [x] `DslTypeString` (`string`)
- [x] `DslTypeName` (`name`)
- [x] `DslTypeQualifiedName` (`qualifiedName`)
- [x] `DslTypeBoolean` (`bool`)
- [x] `DslTypeInteger` (`int`)
- [x] `DslTypeNamePair` (`namePair` primitive)

- [x] `DslTypeElement` (`element("...")`)
- [x] `DslTypeNamedElement` (`namedElement("...")`)
- [x] `DslTypeTypedMap` (`typedMap({ key: typeExpr, ... })`)

- [x] `DslTypeOne` (`one(T1, T2, ...)` with implicit union across args)
- [x] `DslTypeMany` (`many(T1, T2, ...)` with implicit union + singleton lifting)

Still needed for this step (implementation, not just stubs):
- [ ] Add missing behavior to `DslTypeTypedMap`:
    - [x] store field schema as ordered unique-key map (LinkedHashMap)
    - [ ] fail fast on duplicate keys during parse/analyze
    - [ ] `getFieldType(String fieldName)` helper
- [ ] Add basic unit tests for type structure:
    - [ ] `DslTypeOne` single vs union (`choices.size()`)
    - [ ] `DslTypeMany` single vs union (`itemChoices.size()`)
    - [ ] typedMap field ordering preserved

Notes:
- `typedMap` *type definition* has unique keys (schema fields), but runtime values may repeat keys (instruction-list).
- `map` runtime values are always instruction-lists; specialized “unique/merge/replace” map types come later.
- We will **not** split `DslTypeOne`/`DslTypeMany` into separate “single vs union” subclasses yet.
  Instead, treat `choices.size() > 1` as “union”; add a visitor later if/when needed.

Implementation notes:
- [ ] Model “implicit union” by storing `List<DslType> choices` inside `DslTypeOne`/`DslTypeMany`.
- [ ] Keep type objects static and reusable; consider interning later (`DslTypePool`) once correctness is proven.
- [ ] No type builders yet unless needed for momentum.
    - We can add `copy()` / builder semantics to types later if it proves valuable.

### 8.2 Step 2: Define the immutable `DslNode` graph types (first pass interfaces + immutable impls)
Goal: define immutable nodes early so we can test the node graph before writing builders.

Minimum nodes (first pass):
- [ ] `DslRootNode` (root of an instance document)
    - [ ] should carry sources and document-level metadata (candidate: `rootDirectory`)
- [ ] `DslElementNode`
    - [ ] tag name
    - [ ] attribute nodes (by canonical API, not by raw AST)
    - [ ] child element nodes
- [ ] `DslTypedMapNode`
    - [ ] single ordered list of entries: `(keyType/name, value, sources...)`
    - [ ] per-key filtered views (lazy snapshot for immutable form)
- [ ] `DslListNode` / list value node (used by `many(...)`)
- [ ] leaf value nodes as needed (`DslStringValue`, `DslNameValue`, `DslPairValue`, etc)

Notes:
- [ ] In immutable nodes, use lazy snapshotting for filtered views where it helps performance and keeps memory predictable.
- [ ] Every node/value should retain **all contributing sources** for diagnostics and template provenance.

### 8.3 Step 3: Spock test for programmatic immutable node construction
Goal: sanity-check the type system + node graph without the parser/generator in the way.

- [ ] Create a Spock spec that:
    - [ ] programmatically constructs a small type graph (`one`, `many`, `typedMap`)
    - [ ] programmatically constructs a small immutable `DslRootNode` with children
    - [ ] asserts:
        - types are consistent (choices present, many lifts, etc)
        - typedMap filtering produces stable ordered results
        - sources are retained (even if mocked/minimal for now)

This test should not require parsing `.xapi` yet.

### 8.4 Step 4: Introduce builders for `DslNode` (and contexts)
Goal: enable analysis-stage “gross union containers”, references, and multi-file aggregation.

- [ ] Define `DslBuildContext`
    - [ ] error sink / collector
    - [ ] template/type resolution helpers
    - [ ] root metadata (rootDirectory, source roots, etc)
- [ ] Define `DslAnalysisContext`
    - [ ] error sink / collector
    - [ ] parent chaining for lookup (closest-wins shadowing)
    - [ ] per-file metadata (logical name, file path, etc)

- [ ] Define builder interfaces:
    - [ ] `DslBuilder<T extends DslNode>`
        - [ ] `T build()` default method allocates `DslBuildContext`, then throws on errors at end
        - [ ] `T build(DslBuildContext ctx)` recursive build implementation point
        - [ ] `DslBuilder<T> copy()` / `toBuilder()` (optional early; required eventually)
    - [ ] `DslRootBuilder extends DslRootNode` (builder extends built interface)
    - [ ] `DslElementBuilder extends DslElementNode`
    - [ ] `DslTypedMapBuilder extends DslTypedMapNode`

Builder internal storage rules (important):
- [ ] typedMap builders store a single `allEntries` list of typed entries:
    - each entry includes:
        - a key identifier (“internal”, “project”, …) and/or a `NamedDslType` representing that key
        - the `DslType` declared for that key
        - the raw/union-friendly `DslValue` representation
        - sources
- [ ] builder convenience getters (like `getInternal()`) filter `allEntries` lazily.
- [ ] immutable nodes snapshot these filtered results lazily on first access.

Type builders:
- [ ] Defer unless it becomes useful naturally during this stage.

### 8.5 Step 5: Tests for builders (instead of manual immutable construction)
Goal: verify builder canonical views, `.build(ctx)` flattening, and error collection.

- [ ] Add tests that:
    - [ ] create builders, append entries in mixed order
    - [ ] verify getters filter correctly
    - [ ] call `.build()` and verify immutables are stable and ordered
    - [ ] verify unresolved refs add errors and build fails at end, not at first error

### 8.6 Step 6: First generated parser / factory (“DslGenerator”)
Goal: generate code that analyzes an instance `.xapi` file into a strongly typed builder graph.

Outputs:
- [ ] `RootElementNameFactory` (or `*Parser`) class generated from a DSL definition file
- [ ] API surface:
    - [ ] `analyze(UiContainerExpr root)` -> `DslAnalysisResult<RootBuilder>`
    - [ ] `analyze(Path file)` or `analyze(String path)` convenience overload
    - [ ] overloads accept optional parent `DslAnalysisContext` to enable multi-file parsing

`DslAnalysisResult` contains:
- [ ] `DslAnalysisContext` used (with errors/metadata)
- [ ] the strongly typed `RootElementBuilder`
- [ ] any additional metadata useful to callers (source file, rootDirectory, etc)

Behavior:
- [ ] analyzer visits AST and produces builder graph
- [ ] records validation errors during traversal
- [ ] fails at end if errors exist (or returns a result that can throw on demand—TBD)

### 8.7 Step 7: “Mountain of tests” (end-to-end)
Goal: validate the full pipeline: generated analyzer -> builders -> build -> immutable nodes.

- [ ] Tests that parse sample `.xapi` instance docs for `simple-dsl` / `test-dsl`
- [ ] Tests that call generated parser, then `.build()`
- [ ] Assertions for:
    - [ ] correct node graph structure
    - [ ] correct ordered-entry behavior for `typedMap` and `map`
    - [ ] correct singleton lifting for `many`
    - [ ] stable source retention for diagnostics
    - [ ] template/multi-file mechanics (once added)

---

## 9. Templating and multi-file parsing (design notes)

This section captures ideas for modeling multi-file parsing generically (not just `xapi-schema`).

### 9.1 Generic model: treat “multi-file” as a first-class DSL feature
If we can express “this attribute/element points at another xapi document” in the DSL spec itself,
then the generator can implement it uniformly across DSLs.

Possible ingredients:
- a built-in element type (inherited by all DSLs) for templates
- a built-in value/type to represent “source reference”
- explicit “include/override” semantics during build

### 9.2 Template model (scoped, parent-chained)
Template lookup:
- If a child defines template `foo`, it overrides any parent `foo`.
- If a child wants to incorporate parent behavior, it does so explicitly via composition.

Proposed directives (DSL-specific, but intended semantics):
- `inheritTemplate = "foo"`: inside a template, explicitly pull in parent `foo` (if present).
- (Maybe later) `inheritOptionalTemplate = "foo"`: if parent `foo` exists, include it; otherwise do nothing.

### 9.3 Source references (parse another file as a child builder graph)
Idea: a built-in “source ref” concept that denotes:
> “A xapi document at relative path X should be parsed and analyzed as element type Y.”

This is the generalization of patterns like:
- `<xapi-schema projects = { virtual : [ core, dev, gwt, jre ] } />`

Proposed approach:
- Introduce a **sourceRef element type** (possibly built-in / inherited by all DSLs) that is a named element.
- The simplest case can collapse to just a name (e.g. `core`, `dev`), using conventions:
    - default directory: `${name}/`
    - default file name: `${name}.xapi` (or `${name}/${name}.xapi` if preferred)
- For non-default layouts, allow attributes on the named element:
    - `<core directory="modules/core" />`
    - `<core filename="core-schema.xapi" />`
    - `<core path="modules/core/core-schema.xapi" />` (if you want a single override)

Path resolution rules (proposal):
- If `directory` or `path` begins with `/`, resolve relative to the DSL’s `rootDirectory`:
    - gradle root project directory if available
    - else a system property / DSL property fallback
- If relative, resolve relative to the current file’s directory (or an explicit “baseDir” in context)

### 9.4 Merge strategy for referenced sources (baseline + optional “override”)
Baseline (recommended for first pass):
- When you see a `sourceRef`, you parse the referenced file, analyze it into a builder.
- That builder becomes a child builder in the current graph.

Optional “override/compose” mode (future add-on):
- “visit referenced file first, then visit the local element”, effectively layering:
    - referenced builder provides defaults
    - local builder provides additions/overrides
- This is how you could make one `.xapi` file reusable by multiple consumers.

This should likely be expressed as an explicit directive:
- `use = "refName"` or `include = "refName"` vs `inheritTemplate = ...`
- or a dedicated type: `sourceElement` that means “merge these two builder graphs”

### 9.5 Where this plugs into the pipeline
- During **analysis**, encountering a `sourceRef` can enqueue additional parses using a child `DslAnalysisContext`.
- During **build**, referenced builders are resolved and flattened into the final immutable node graph.
- Errors (missing file, parse error, type mismatch) are recorded in contexts:
    - parse/analyze failures go to `DslAnalysisContext`
    - unresolved refs at build time go to `DslBuildContext`

---

## 10. Notes / deferred ideas
- A `tuple(...)` type may be useful later for ordered fields, but may be fragile for complex nested items.
- `DslTypePool` / interning can make type identity comparisons cheap; do after correctness.
- Build-time customization hooks (`DslFlattener`, `DslBuildEvent`) are intentionally deferred.