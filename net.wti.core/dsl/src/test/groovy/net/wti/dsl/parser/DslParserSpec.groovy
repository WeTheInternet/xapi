package net.wti.dsl.parser

import net.wti.lang.parser.ast.expr.Expression
import net.wti.lang.parser.ast.expr.JsonContainerExpr
import net.wti.lang.parser.ast.expr.UiAttrExpr
import net.wti.lang.parser.ast.expr.UiContainerExpr
import spock.lang.Specification

// net.wti.core/dsl/src/test/groovy/net/wti/dsl/parser/DslParserSpec.groovy
class DslParserSpec extends Specification {

  def "parser loads complex xapi-dsl (test-dsl.xapi) and exposes element definitions"() {
    given:
    def parser = new DslParser()

    when:
    def model = parser.parseResource("META-INF/xapi/test-dsl.xapi")
    UiContainerExpr root = model.root

    then: "root element is <xapi-dsl> with configured name"
    root.name == DslParser.ROOT_ELEMENT
    root.getAttribute("name")
        .ifAbsentThrow({ new Error("Missing name attribute on xapi-dsl root") })
        .get()
        .getString(false, true) == "test-dsl"

    and: "elements attribute exists and is a json array"
    UiAttrExpr elementsAttr = root.getAttribute("elements")
        .ifAbsentThrow({ new Error("Missing elements attribute on xapi-dsl root") })
        .get()
    def elementsExpr = elementsAttr.expression
    elementsExpr instanceof JsonContainerExpr
    ((JsonContainerExpr) elementsExpr).isArray()

    and: "we have five element-def entries in order"
    def json = (JsonContainerExpr) elementsExpr
    def elementDefs = json.values
        .filterInstanceOf(UiContainerExpr)
        .toList()

    elementDefs.size() == 5
    elementDefs*.name == ["element-def", "element-def", "element-def", "element-def", "element-def"]

    and: "first element-def is root definition with attributes and elements"
    def rootDef = elementDefs[0]
    rootDef.getAttribute("name").get().getString(false, true) == "root"
    rootDef.getAttribute("description").get().getString(false, true) == "Root element of the test DSL."
    rootDef.getAttribute("attributes").present
    rootDef.getAttribute("elements").present

    and: "root-def attributes map contains expected keys"
    def rootAttrsExpr = rootDef.getAttribute("attributes").get().expression as JsonContainerExpr
    !rootAttrsExpr.isArray()
    def rootAttrPairs = rootAttrsExpr.pairs
    rootAttrPairs*.keyString as Set == ["name", "enabled", "version", "tags", "children"] as Set

    and: "second element-def is child definition"
    def childDef = elementDefs[1]
    childDef.getAttribute("name").get().getString(false, true) == "child"
    childDef.getAttribute("description").get().getString(false, true).contains("child element inside root")
    childDef.getAttribute("attributes").present
    childDef.getAttribute("elements").present

    and: "child-def attributes include id, kind, labels, props"
    def childAttrsExpr = childDef.getAttribute("attributes").get().expression as JsonContainerExpr
    !childAttrsExpr.isArray()
    childAttrsExpr.pairs*.keyString as Set == ["id", "kind", "labels", "props"] as Set

    and: "third element-def is config definition"
    def configDef = elementDefs[2]
    configDef.getAttribute("name").get().getString(false, true) == "config"
    configDef.getAttribute("attributes").present
    configDef.getAttribute("elements").present

    and: "fourth element-def is setting definition"
    def settingDef = elementDefs[3]
    settingDef.getAttribute("name").get().getString(false, true) == "setting"
    settingDef.getAttribute("attributes").present

    and: "setting definition has an empty elements map"
    UiAttrExpr settingElemsAttr = settingDef.getAttribute("elements")
        .ifAbsentThrow({ new Error("Missing elements attribute on setting element-def") })
        .get()
    assert settingElemsAttr.expression instanceof JsonContainerExpr
    JsonContainerExpr settingElemsExpr = (JsonContainerExpr) settingElemsAttr.expression
    !settingElemsExpr.isArray()
    settingElemsExpr.pairs.isEmpty()

    and: "fifth element-def is registry definition with roots attribute"
    def registryDef = elementDefs[4]
    registryDef.getAttribute("name").get().getString(false, true) == "registry"
    registryDef.getAttribute("attributes").present
    def registryAttrsExpr = registryDef.getAttribute("attributes").get().expression as JsonContainerExpr
    registryAttrsExpr.pairs*.keyString == ["roots"]
  }

  def "simple-dsl.xapi describes simple-valid.xapi structure"() {
    given:
    DslParser parser = new DslParser()

    when: "we parse the simple-dsl definition"
    DslModel dslModel = parser.parseResource("META-INF/xapi/simple-dsl.xapi")
    UiContainerExpr dslRoot = dslModel.root

    then: "dsl root is <xapi-dsl> named simple-dsl"
    dslRoot.name == DslParser.ROOT_ELEMENT
    dslRoot.getAttribute("name")
        .ifAbsentThrow({ new AssertionError("Missing name attribute on simple-dsl root") })
        .get()
        .getString(false, true) == "simple-dsl"

    and: "elements attribute exists and is a json array of element-defs"
    UiAttrExpr elementsAttr = dslRoot.getAttribute("elements")
        .ifAbsentThrow({ new AssertionError("Missing elements attribute on simple-dsl") })
        .get()
    Expression elementsExpr = elementsAttr.expression
    assert elementsExpr instanceof JsonContainerExpr
    JsonContainerExpr elementsJson = (JsonContainerExpr) elementsExpr
    elementsJson.isArray()

    and: "we have element-defs for root, child and setting"
    List<UiContainerExpr> simpleDefs = elementsJson.values
        .filterInstanceOf(UiContainerExpr)
        .toList()

    simpleDefs.size() == 3
    simpleDefs*.name == ["element-def", "element-def", "element-def"]

    Set<String> simpleNames = simpleDefs.collect {
      it.getAttribute("name").get().getString(false, true)
    } as Set
    simpleNames == ["root", "child", "setting"] as Set

    and: "root definition has expected attributes (name, flag, count, tags, props, settings)"
    UiContainerExpr rootDef = simpleDefs.find {
      it.getAttribute("name").get().getString(false, true) == "root"
    }
    JsonContainerExpr rootAttrsExpr = (JsonContainerExpr) rootDef.getAttribute("attributes").get().expression
    !rootAttrsExpr.isArray()
    Set<String> rootAttrNames = rootAttrsExpr.pairs*.keyString as Set
    rootAttrNames == ["name", "flag", "count", "tags", "props", "settings"] as Set

    and: "child definition has id, tags, props, settings"
    UiContainerExpr childDef = simpleDefs.find {
      it.getAttribute("name").get().getString(false, true) == "child"
    }
    JsonContainerExpr childAttrsExpr = (JsonContainerExpr) childDef.getAttribute("attributes").get().expression
    Set<String> childAttrNames = childAttrsExpr.pairs*.keyString as Set
    childAttrNames == ["id", "tags", "props", "settings"] as Set

    and: "setting definition has key and value"
    UiContainerExpr settingDef = simpleDefs.find {
      it.getAttribute("name").get().getString(false, true) == "setting"
    }
    JsonContainerExpr settingAttrsExpr = (JsonContainerExpr) settingDef.getAttribute("attributes").get().expression
    Set<String> settingAttrNames = settingAttrsExpr.pairs*.keyString as Set
    settingAttrNames == ["key", "value"] as Set

    when: "we parse a valid instance using this DSL"
    UiContainerExpr instanceRoot = parser.parseResource("net/wti/dsl/parser/simple-valid.xapi")

    then: "instance root element matches simple-dsl root definition"
    instanceRoot.name == "root"
    instanceRoot.getAttribute("name").get().getString(false, true) == "example-root"
    instanceRoot.getAttribute("flag").present
    instanceRoot.getAttribute("count").present
    instanceRoot.getAttribute("tags").present
    instanceRoot.getAttribute("props").present
    instanceRoot.getAttribute("settings").present

    and: "instance has a child element matching child definition"
    List<UiContainerExpr> children = instanceRoot.children
        .filterInstanceOf(UiContainerExpr)
        .toList()
    children.size() == 1

    UiContainerExpr child = children.get(0)
    child.name == "child"
    child.getAttribute("id").get().getString(false, true) == "child-1"
    child.getAttribute("tags").present
    child.getAttribute("props").present
    child.getAttribute("settings").present
  }

}
