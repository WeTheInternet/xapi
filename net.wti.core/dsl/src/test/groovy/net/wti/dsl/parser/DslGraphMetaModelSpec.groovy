package net.wti.dsl.parser

import net.wti.dsl.test.AbstractDslSpec
import net.wti.dsl.type.DslType
import net.wti.dsl.type.DslTypeElement
import net.wti.dsl.type.DslTypeListOrMap
import net.wti.dsl.type.DslTypeMany
import net.wti.dsl.type.DslTypeMap
import net.wti.dsl.type.DslTypeString
import net.wti.lang.parser.JavaParser
import net.wti.lang.parser.ast.expr.UiContainerExpr
import net.wti.lang.parser.ast.visitor.ComposableXapiVisitor
import xapi.fu.In2

///
/// DslGraphMetaModelSpec:
///
/// Verifies that a parsed xapi-dsl definition can be converted into a DslGraphModel,
/// and that the resulting model:
///  - includes definitions for elements present in sample instance documents
///  - includes compiled attribute schema types for known attributes
///

/// Created by James X. Nelson (James@WeTheInter.net) on 16/12/2025 @ 01:45
final class DslGraphMetaModelSpec extends AbstractDslSpec {

    ///
    /// Collect all UiContainerExpr nodes (elements) from a parsed xapi AST.
    ///
    private static List<UiContainerExpr> collectElements(final UiContainerExpr root) {
        final List<UiContainerExpr> elements = new ArrayList<>()
        final ComposableXapiVisitor<List<UiContainerExpr>> visitor =
                ComposableXapiVisitor.<List<UiContainerExpr>>whenMissingIgnore(DslGraphMetaModelSpec)
                        .withUiContainerRecurse(In2.in2 { final UiContainerExpr el, final List<UiContainerExpr> acc ->
                            acc.add(el)
                        })
        visitor.visit(root, elements)
        return elements
    }

    // -------------------------------------------------------------------------
    // Reader-first: what the graph meta model provides
    // -------------------------------------------------------------------------

    def "building a graph meta model compiles attribute schema types for element-def entries"() {
        given: "a parsed dsl definition"
        final DslParser parser = new DslParser()
        final DslModel dslModel = parser.parseResource("META-INF/xapi/simple-dsl.xapi")

        when: "we build a graph meta model"
        final DslGraphMetaModelBuilder builder = new DslGraphMetaModelBuilder()
        final DslGraphModel graph = builder.build(dslModel)

        then: "elements are present"
        graph.getRootTag() == "root"
        graph.getElementsByTag() != null
        !graph.getElementsByTag().isEmpty()

        and: "the root element definition contains compiled attribute types"
        final DslGraphModel.ElementType rootEl = graph.getElement("root")
        rootEl != null
        rootEl.getAttributeTypes() != null
        !rootEl.getAttributeTypes().isEmpty()

        and: "representative attribute types are compiled from xapi source"
        final DslType nameType = rootEl.getAttributeType("name")
        nameType instanceof DslTypeString

        final DslType tagsType = rootEl.getAttributeType("tags")
        tagsType instanceof DslTypeMany
        ((DslTypeMany) tagsType).getItemChoices().size() == 1
        ((DslTypeMany) tagsType).getItemChoices().get(0) instanceof DslTypeString

        final DslType propsType = rootEl.getAttributeType("props")
        propsType instanceof DslTypeMap
        ((DslTypeMap) propsType).getKeyType() instanceof DslTypeString
        ((DslTypeMap) propsType).getValueChoices().size() == 1
        ((DslTypeMap) propsType).getValueChoices().get(0) instanceof DslTypeString

        final DslType settingsType = rootEl.getAttributeType("settings")
        settingsType instanceof DslTypeListOrMap
        ((DslTypeListOrMap) settingsType).getListElementType() instanceof DslTypeElement
        ((DslTypeElement) ((DslTypeListOrMap) settingsType).getListElementType()).getElementTypeName() == "setting"
        ((DslTypeListOrMap) settingsType).getMapValueType() instanceof DslTypeElement
        ((DslTypeElement) ((DslTypeListOrMap) settingsType).getMapValueType()).getElementTypeName() == "setting"
    }

    // -------------------------------------------------------------------------
    // Coverage-style: instance documents only use known elements
    // -------------------------------------------------------------------------

    def "simple-dsl meta model covers all elements used in simple-valid.xapi"() {
        given: "a DslModel parsed from simple-dsl.xapi"
        final DslParser parser = new DslParser()
        final DslModel dslModel = parser.parseResource("META-INF/xapi/simple-dsl.xapi")

        and: "a graph meta-model built from that DslModel"
        final DslGraphMetaModelBuilder builder = new DslGraphMetaModelBuilder()
        final DslGraphModel graph = builder.build(dslModel)

        and: "a parsed sample instance implementing this DSL"
        final InputStream simpleStream =
                DslGraphMetaModelSpec.class.getResourceAsStream("/net/wti/dsl/parser/simple-valid.xapi")
        assert simpleStream != null
        final UiContainerExpr instanceRoot = JavaParser.parseXapi(simpleStream)

        when: "we collect all elements from the sample instance"
        final List<UiContainerExpr> elements = collectElements(instanceRoot)

        then: "the meta model has a root tag"
        graph.getRootTag() != null

        and: "every element used in the sample instance is known to the meta model"
        for (final UiContainerExpr el : elements) {
            final String tag = el.getName()
            final DslGraphModel.ElementType elDef = graph.getElement(tag)
            assert elDef != null : "Unknown DSL element <${tag}> in simple-valid.xapi"
            final String elTypeName = elDef.getElTypeName()
            assert elTypeName.endsWith("El")
            assert Character.isUpperCase(elTypeName.charAt(0))
        }
    }

    def "test-dsl meta model covers all elements used in test-valid.xapi"() {
        given: "a DslModel parsed from test-dsl.xapi"
        final DslParser parser = new DslParser()
        final DslModel dslModel = parser.parseResource("META-INF/xapi/test-dsl.xapi")

        and: "a graph meta-model built from that DslModel"
        final DslGraphMetaModelBuilder builder = new DslGraphMetaModelBuilder()
        final DslGraphModel graph = builder.build(dslModel)

        and: "a parsed sample instance implementing this DSL"
        final InputStream testStream =
                DslGraphMetaModelSpec.class.getResourceAsStream("/net/wti/dsl/parser/test-valid.xapi")
        assert testStream != null
        final UiContainerExpr instanceRoot = JavaParser.parseXapi(testStream)

        when: "we collect all elements from the sample instance"
        final List<UiContainerExpr> elements = collectElements(instanceRoot)

        then: "the meta model has a non-empty set of elements and a root tag"
        !graph.getElementsByTag().isEmpty()
        graph.getRootTag() != null

        and: "every element used in the sample instance is known to the meta model"
        for (final UiContainerExpr el : elements) {
            final String tag = el.getName()
            final DslGraphModel.ElementType elDef = graph.getElement(tag)
            assert elDef != null : "Unknown DSL element <${tag}> in test-valid.xapi"
            final String elTypeName = elDef.getElTypeName()
            assert elTypeName.endsWith("El")
            assert Character.isUpperCase(elTypeName.charAt(0))
        }
    }
}