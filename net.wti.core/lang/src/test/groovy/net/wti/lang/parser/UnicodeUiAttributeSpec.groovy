package net.wti.lang.parser

import net.wti.lang.parser.ast.expr.JsonContainerExpr
import net.wti.lang.parser.ast.expr.UiContainerExpr
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class UnicodeUiAttributeSpec extends Specification {
    private static UiContainerExpr parse(String source) {
        JavaParser.parseXapi(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), 'UTF-8')
    }

    def "XApi scalar strings preserve raw Unicode through source round trips"() {
        when:
        def element = parse('<QuestDefinition title = "水, 茶" />')
        def reopened = parse(element.toSource())

        then:
        element.getAttributeRequiredString('title') == '水, 茶'
        reopened.getAttributeRequiredString('title') == '水, 茶'
    }

    def "XApi string arrays preserve raw Unicode and authored commas"() {
        when:
        def element = parse('<QuestDefinition tags = ["food, bag", "water", "水, 茶"] />')
        def reopened = parse(element.toSource())

        then:
        [element, reopened].every { parsed ->
            JsonContainerExpr tags = parsed.getAttributeNotNull('tags').expression as JsonContainerExpr
            tags.array && tags.pairs.collect { it.valueExpr.value } == ['food, bag', 'water', '水, 茶']
        }
    }
}
