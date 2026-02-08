package net.wti.dsl.parser

import net.wti.dsl.test.AbstractDslSpec
import net.wti.dsl.type.DslTypeBoolean
import net.wti.dsl.type.DslTypeInteger
import net.wti.dsl.type.DslTypeMany
import net.wti.dsl.type.DslTypeMap
import net.wti.dsl.type.DslTypeName
import net.wti.dsl.type.DslTypeNamePair
import net.wti.dsl.type.DslTypeOne
import net.wti.dsl.type.DslTypeQualifiedName
import net.wti.dsl.type.DslTypeRef
import net.wti.dsl.type.DslTypeString
import net.wti.dsl.type.DslTypeTypedMap
import net.wti.lang.parser.JavaParser
import net.wti.lang.parser.ast.expr.Expression
import net.wti.lang.parser.ast.expr.MethodCallExpr
import net.wti.lang.parser.ast.expr.UiContainerExpr
import spock.lang.Shared
import xapi.fu.itr.MappedIterable

///
/// DslTypeExprParserSpec:
///
/// Exercises parsing of type expressions from real xapi-dsl resource files,
/// so tests stay aligned with the DSL authoring surface syntax.
///
final class DslTypeExprParserSpec extends AbstractDslSpec {

    @Shared
    private static final String TEST_DSL = "/META-INF.xapi/test-dsl.xapi"

    @Shared
    private static final String SIMPLE_DSL = "/META-INF.xapi/simple-dsl.xapi"

    def "parse primitive type tags"() {
        given:
        final UiContainerExpr stringExpr = new UiContainerExpr("string")
        final UiContainerExpr nameExpr = new UiContainerExpr("name")
        final UiContainerExpr qualifiedNameExpr = new UiContainerExpr("qualifiedName")
        final UiContainerExpr boolExpr = new UiContainerExpr("bool")
        final UiContainerExpr intExpr = new UiContainerExpr("int")
        final UiContainerExpr namePairExpr = new UiContainerExpr("namePair")

        expect:
        DslTypeExprParser.parse(stringExpr) instanceof DslTypeString
        DslTypeExprParser.parse(nameExpr) instanceof DslTypeName
        DslTypeExprParser.parse(qualifiedNameExpr) instanceof DslTypeQualifiedName
        DslTypeExprParser.parse(boolExpr) instanceof DslTypeBoolean
        DslTypeExprParser.parse(intExpr) instanceof DslTypeInteger
        DslTypeExprParser.parse(namePairExpr) instanceof DslTypeNamePair
    }

    def "parse map and typeRef from real xapi-dsl test resources"() {
        ///
        /// This test uses the same META-INF resources as other DSL specs,
        /// and asserts we can locate and parse representative typeExpr forms.
        ///
        given: "an xapi-dsl document parsed from resources"
        final MappedIterable<Expression> sourceAst = astFromResource(TEST_DSL)
        final Expression rootExpr = sourceAst.iterator().next()
        assert rootExpr instanceof UiContainerExpr

        final UiContainerExpr root = (UiContainerExpr) rootExpr

        and: "a helper to parse a subtree back into a UiContainerExpr"
        final String sourceText = root.toSource()
        final UiContainerExpr reparsed = JavaParser.parseXapi(new ByteArrayInputStream(sourceText.getBytes("UTF-8")))

        when: "we scan for any <typeRef .../> usage"
        final List<UiContainerExpr> allTags = collectAllTags(reparsed)
        final UiContainerExpr typeRefExpr = allTags.find { UiContainerExpr it -> it.getName() == "typeRef" }

        then:
        typeRefExpr == null || DslTypeExprParser.parse(typeRefExpr) instanceof DslTypeRef
    }

    ///
    /// Collects all UiContainerExpr nodes in a tree.
    ///
    private static List<UiContainerExpr> collectAllTags(final UiContainerExpr root) {
        final List<UiContainerExpr> out = new ArrayList<>()
        out.add(root)
        final List<Expression> kids = root.getBody() == null ? Collections.<Expression>emptyList() : root.getBody().getChildren()
        for (final Expression child : kids) {
            if (child instanceof UiContainerExpr) {
                out.addAll(collectAllTags((UiContainerExpr) child))
            }
        }
        return out
    }

    def "parse one/many/map from call syntax"() {
        given:
        final UiContainerExpr stringExpr = new UiContainerExpr("string")
        final UiContainerExpr nameExpr = new UiContainerExpr("name")

        when:
        final MethodCallExpr oneCall =
                new MethodCallExpr(null, "one", Arrays.<Expression>asList(stringExpr, nameExpr))
        final MethodCallExpr manyCall =
                new MethodCallExpr(null, "many", Collections.<Expression>singletonList(stringExpr))
        final MethodCallExpr mapCall =
                new MethodCallExpr(null, "map", Arrays.<Expression>asList(stringExpr, nameExpr))

        then:
        final DslTypeOne one = (DslTypeOne) DslTypeExprParser.parse(oneCall)
        one.getChoices()*.class == [DslTypeString, DslTypeName]

        and:
        final DslTypeMany many = (DslTypeMany) DslTypeExprParser.parse(manyCall)
        many.getItemChoices()*.class == [DslTypeString]

        and:
        final DslTypeMap map = (DslTypeMap) DslTypeExprParser.parse(mapCall)
        map.getValueChoices()*.class == [DslTypeString, DslTypeName]
    }
}
