# Repository Map

## Build families

| Family | Current role | Routine entry point | Important constraint |
| --- | --- | --- | --- |
| `net.wti.core` | Routine bootstrap primitives: annotations, functional utilities, parser/language, generation, DSL | First stage of `liteBuild.sh` | Java 8 target; locally publishes `net.wti.core:*:0.5.1` |
| `net.wti.gradle.modern` | Routine Gradle 8.11.1 support, settings/schema parser, migration bridge, font/atlas plugins | Second stage of `liteBuild.sh` | Java 8 toolchain; settings plugin drives the main build |
| Main root build | Schema-generated XApi libraries | Final stage of `liteBuild.sh` | Root settings loads modern plugins from ignored `repo/`; generated project scripts are tracked |
| `net.wti.gradle.tools` | Legacy but still consumed Gradle plugins/tools | `fullBuild.sh` or `toolBuild.sh --all` | Uses customized Gradle 5.1 distribution; contains explicitly deprecated publication plugin still consumed by main `buildSrc` |
| `net.wti.gradle` | Legacy but still consumed loader/plugin/API family | `fullBuild.sh` or `toolBuild.sh --all` | Uses customized Gradle 5.1 distribution; main `buildSrc` consumes its published artifacts |

## Main schema build

The root `schema.xapi` describes logical projects, platforms, modules, dependencies, publication, and version `0.5.1`. The current index contains roughly 51 project entries and the repository tracks roughly 106 settings-plugin-generated Gradle scripts. Counts can change as liveness or source layout changes; they are orientation figures, not invariants.

Major domains include `base`, `collect`, `core`, `dev`, `gwt`, `gwtc`, `inject`, `io`, `jre`, `log`, `model`, `process`, `samples`, `server`, `ui`, and `util`.

Do not infer maintenance status merely from schema membership or generated-script presence. Establish status while investigating concrete code, consumers, and runnable tests, then update this map when the conclusion is clear.

## Language and structured DSL layer

The `net.wti.core/lang` parser is an extended JavaParser/JavaCC grammar, not a separate
XML configuration reader. Its generated `ASTParser` contains both Java entry points and
XApi's `RootUiContainer` entry point. Many `*.xapi` files use an XML-shaped container
surface whose attribute values are Java-derived AST expressions: names, literals, calls,
lambdas, JSON-like arrays/maps, nested tags, templates, and related nodes remain
structurally distinct. `JavaParser.parseXapi(...)` returns a `UiContainerExpr` tree;
domain visitors such as the modern `XapiSchemaParser` or core `DslParser` then assign
semantics. The root `README.md` explains this format and links the grammar, facade, AST,
schema, and DSL examples.

## Build-time dependency chain

The main `settings.gradle` resolves the modern schema parser and settings plugin from `repo/`. The main `buildSrc` also resolves artifacts from all of these families:

- `net.wti.gradle.modern`
- `net.wti.gradle`
- `net.wti.gradle.tools`
- `net.wti.core`

This is why the legacy build sources are not safely deletable even though routine `liteBuild.sh` does not rebuild them.
