package net.wti.dsl.parser

import net.wti.dsl.test.AbstractDslSpec
import net.wti.lang.parser.ast.expr.Expression
import net.wti.lang.parser.ast.expr.JsonContainerExpr
import net.wti.lang.parser.ast.expr.UiAttrExpr
import net.wti.lang.parser.ast.expr.UiContainerExpr
import spock.lang.Shared

///
/// DslParserSpec:
///
/// Reader-first tests for how `DslParser` behaves when given xapi-dsl definition sources.
/// Lower-level assertions about detailed schema structure belong later in this file.
///
final class DslParserSpec extends AbstractDslSpec {

    @Shared
    private static final String SIMPLE_DSL_RESOURCE = "META-INF/xapi/simple-dsl.xapi"

    @Shared
    private static final String TEST_DSL_RESOURCE = "META-INF/xapi/test-dsl.xapi"

    def "parseResource loads an xapi-dsl definition and returns a DslModel rooted at <xapi-dsl>"() {
        given:
        final DslParser parser = new DslParser()

        when:
        final DslModel model = parser.parseResource(SIMPLE_DSL_RESOURCE)
        final UiContainerExpr root = model.getRoot()

        then:
        root != null
        root.getName() == DslParser.ROOT_ELEMENT
        model.getName() == "simple-dsl"
        model.getPackageName() == "net.wti.simple"
    }

    def "parseResource fails with a clear error when the resource does not exist"() {
        given:
        final DslParser parser = new DslParser()
        final String missing = "META-INF/xapi/does-not-exist.xapi"

        when:
        parser.parseResource(missing)

        then:
        final IllegalArgumentException err = thrown(IllegalArgumentException)
        err.message != null
        err.message.contains("DSL resource not found on classpath")
        err.message.contains(missing)
    }

    def "parseStream rejects sources whose root element is not <xapi-dsl>"() {
        given:
        final DslParser parser = new DslParser()
        final String sourceName = "inline://not-a-dsl"
        final String content = "<not-xapi-dsl />"
        final InputStream inputStream = new ByteArrayInputStream(content.getBytes("UTF-8"))

        when:
        parser.parseStream(sourceName, inputStream)

        then:
        final IllegalArgumentException err = thrown(IllegalArgumentException)
        err.message != null
        err.message.contains("root element must be <" + DslParser.ROOT_ELEMENT + ">")
        err.message.contains(sourceName)
    }

    // --------------------------------------------------------------------------
    // Coverage-style assertions (structure inspection)
    // --------------------------------------------------------------------------

    def "parsed xapi-dsl includes an elements= json array of <element-def> nodes"() {
        given:
        final DslParser parser = new DslParser()

        when:
        final DslModel model = parser.parseResource(TEST_DSL_RESOURCE)
        final UiContainerExpr root = model.getRoot()

        then:
        root.getName() == DslParser.ROOT_ELEMENT

        and:
        final UiAttrExpr elementsAttr = root.getAttribute("elements")
                .ifAbsentThrow({ new AssertionError("Missing elements attribute on xapi-dsl root: " + TEST_DSL_RESOURCE) })
                .get()

        and:
        final Expression elementsExpr = elementsAttr.getExpression()
        elementsExpr instanceof JsonContainerExpr

        and:
        final JsonContainerExpr json = (JsonContainerExpr) elementsExpr
        json.isArray()

        and:
        final List<UiContainerExpr> elementDefs = json.getValues()
                .filterInstanceOf(UiContainerExpr)
                .toList()

        elementDefs.size() > 0
        elementDefs.every { final UiContainerExpr defn -> defn.getName() == "element-def" }
    }

    def "element-def entries include a name= attribute and an attributes= json object"() {
        given:
        final DslParser parser = new DslParser()

        when:
        final DslModel model = parser.parseResource(SIMPLE_DSL_RESOURCE)
        final UiContainerExpr root = model.getRoot()

        then:
        final UiAttrExpr elementsAttr = root.getAttribute("elements")
                .ifAbsentThrow({ new AssertionError("Missing elements attribute on xapi-dsl root: " + SIMPLE_DSL_RESOURCE) })
                .get()

        and:
        final JsonContainerExpr elementsJson = (JsonContainerExpr) elementsAttr.getExpression()
        elementsJson.isArray()

        and:
        final List<UiContainerExpr> defs = elementsJson.getValues()
                .filterInstanceOf(UiContainerExpr)
                .toList()

        defs.size() == 3

        and:
        for (final UiContainerExpr defn : defs) {
            defn.getName() == "element-def"

            final UiAttrExpr nameAttr = defn.getAttribute("name")
                    .ifAbsentThrow({ new AssertionError("element-def missing name= attribute: " + defn.toSource()) })
                    .get()
            final String name = nameAttr.getString(false, true)
            name != null
            !name.isEmpty()

            final UiAttrExpr attrsAttr = defn.getAttribute("attributes")
                    .ifAbsentThrow({ new AssertionError("element-def " + name + " missing attributes= map") })
                    .get()
            final Expression attrsExpr = attrsAttr.getExpression()
            assert attrsExpr instanceof JsonContainerExpr
            assert !((JsonContainerExpr) attrsExpr).isArray()
        }
    }
}
