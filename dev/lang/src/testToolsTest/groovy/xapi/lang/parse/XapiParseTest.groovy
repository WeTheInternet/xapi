package xapi.lang.parse

import net.wti.lang.parser.JavaParser
import net.wti.lang.parser.ast.expr.JsonContainerExpr
import net.wti.lang.parser.ast.expr.UiContainerExpr
import spock.lang.Specification

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/20/18 @ 3:45 AM.
 */
class XapiParseTest extends Specification implements ParseTestMixin {

    def "Parser can handle simple xapi manifests"() {

        when:
        String src = '''
<xapi sources = [
  "$rootDir/src/main/java",
  "$rootDir/src/main/gen"
]
resources = [
  "$rootDir/src/main/resources"
]
outputs = [
]
/>
'''
        UiContainerExpr ast = JavaParser.parseUiContainer("XapiParseTest", src)
        then:
        ast.childrenNodes.size() == 3
        ast.getAttributes().size() == 3
        (getAttr(ast, 'sources') as JsonContainerExpr).size() == 2
        (getAttr(ast, 'resources') as JsonContainerExpr).size() == 1
        (getAttr(ast, 'outputs') as JsonContainerExpr).size() == 0

    }

    def "Parser can handle arrays with trailing commas"() {

        given:
        String src = '''
<xapi sources = [
  "value",
]
resources = [ "value" , ]
outputs = ["value",]
/>
'''
        when:
        parse(src)

        then:
        ast.childrenNodes.size() == 3
        ast.getAttributes().size() == 3
        getAttr(ast, 'sources', JsonContainerExpr).size() == 1
        getAttr(ast, 'resources', JsonContainerExpr).size() == 1
        getAttr(ast, 'outputs', JsonContainerExpr).size() == 1

    }

}
