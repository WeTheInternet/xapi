package net.wti.dsl.type

import net.wti.dsl.test.AbstractDslSpec
import net.wti.lang.parser.JavaParser
import net.wti.lang.parser.ast.expr.Expression
import net.wti.lang.parser.ast.expr.UiContainerExpr
import spock.lang.TempDir
import xapi.fu.itr.MappedIterable

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

///
/// DslTypeSpec:
///
/// This spec is intentionally written in two halves:
///
///  1) Specification-style tests (clarity first):
///     These describe how DslType objects are expected to behave and be consumed.
///     The goal is readable intent over exhaustive coverage.
///
///  2) Coverage-style tests (coverage first):
///     These try to exercise edge cases and invariants with less narrative.
///
/// Notes on resource usage:
///  - Prefer *not* using real `.xapi` files for pure "object behavior" tests.
///  - Only parse real/generated `.xapi` content when validating parser/diagnostics behavior.
///
final class DslTypeSpec extends AbstractDslSpec {

    @TempDir
    File tmpDir

    def "DslTypeTypedMap schema keys must be valid names (Java identifiers) because we generate fields from them"() {
        ///
        /// Establishes the core constraint:
        /// typedMap schema keys must be valid Java identifiers.
        ///
        given: "sourceAst from a real resource file"
        final MappedIterable<Expression> sourceAst = astFromResource("/META-INF/xapi/test-dsl.xapi")

        and: "types to use as schema field values"
        final DslTypeString stringType = new DslTypeString(sourceAst)
        final DslTypeName nameType = new DslTypeName(sourceAst)

        when: "the schema includes a non-name key (contains '-')"
        new DslTypeTypedMap(sourceAst, [
                "not-a-name": stringType,
                "ok": nameType
        ])

        then: "construction fails fast with a clear error message"
        final IllegalArgumentException err = thrown(IllegalArgumentException)
        err.message != null
        err.message.contains("typedMap field name")
    }

    def "DslTypeTypedMap schema preserves declared order for stable output and codegen"() {
        ///
        /// Establishes that typedMap schema ordering is stable.
        ///
        given:
        final MappedIterable<Expression> sourceAst = astFromResource("/META-INF/xapi/simple-dsl.xapi")
        final DslTypeString typeString = new DslTypeString(sourceAst)
        final DslTypeName typeName = new DslTypeName(sourceAst)

        when:
        final DslTypeTypedMap typedMapType = new DslTypeTypedMap(sourceAst, [
                internal: typeName,
                project : typeString,
                external: typeString
        ])

        then:
        typedMapType.getFieldTypes().keySet().toList() == ["internal", "project", "external"]
        typedMapType.getFieldType("internal") == typeName
        typedMapType.getFieldType("project") == typeString
        typedMapType.getFieldType("external") == typeString
        typedMapType.getFieldType("missing") == null
    }

    def "DslTypeOne and DslTypeMany model unions by list size (no special subclasses required)"() {
        ///
        /// Establishes the rule:
        /// - one(T) vs one(T,U) are represented by DslTypeOne with choices.size() 1 vs 2
        /// - many(T) vs many(T,U) are represented by DslTypeMany with itemChoices.size() 1 vs 2
        ///
        given:
        final MappedIterable<Expression> sourceAst = astFromResource("/META-INF/xapi/simple-dsl.xapi")
        final DslTypeString typeString = new DslTypeString(sourceAst)
        final DslTypeName typeName = new DslTypeName(sourceAst)

        when:
        final DslTypeOne oneSingle = new DslTypeOne(sourceAst, Collections.<DslType>singletonList(typeString))
        final DslTypeOne oneUnion = new DslTypeOne(sourceAst, Arrays.<DslType>asList(typeString, typeName))

        final DslTypeMany manySingle = new DslTypeMany(sourceAst, Collections.<DslType>singletonList(typeString))
        final DslTypeMany manyUnion = new DslTypeMany(sourceAst, Arrays.<DslType>asList(typeString, typeName))

        then:
        oneSingle.getChoices().size() == 1
        oneUnion.getChoices().size() == 2
        manySingle.getItemChoices().size() == 1
        manyUnion.getItemChoices().size() == 2
    }

    def "we can parse a generated temp .xapi file and reuse its AST as DslObject sourceAst"() {
        ///
        /// Scaffold for future diagnostic / line-number tests.
        ///
        given:
        final Path filePath = new File(tmpDir, "generated.xapi").toPath()
        final String content =
                "<xapi-dsl\n" +
                "  name = \"generated\"\n" +
                "  rootTag = root\n" +
                "/>\n"
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8))

        when:
        final InputStream inputStream = Files.newInputStream(filePath)
        final UiContainerExpr parsed = JavaParser.parseXapi(inputStream)

        final List<Expression> singleton = Collections.<Expression>singletonList(parsed)
        final MappedIterable<Expression> sourceAst = MappedIterable.mapped(singleton)

        then:
        sourceAst != null
        sourceAst.iterator().hasNext()
        ((UiContainerExpr) sourceAst.iterator().next()).getName() == "xapi-dsl"
    }
}
