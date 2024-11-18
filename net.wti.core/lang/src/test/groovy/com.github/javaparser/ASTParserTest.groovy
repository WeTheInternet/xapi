package com.github.javaparser

import com.github.javaparser.ast.expr.JsonContainerExpr

class ASTParserTest extends spock.lang.Specification {
    def "extra trailing comma works for json container with multiple pairs"() {
        given:
        JsonContainerExpr expr = JavaParser.parseJsonContainer("""{
            one: 2,
            three: 4,
        }""")
        JsonContainerExpr compressed = JavaParser.parseJsonContainer(expr.toSourceCompressed())
        expect:
        expr.toSource() == """{
  one : 2,
  three : 4
}"""
        expr.toSourceCompressed() == """{one:2,three:4}"""
        expr.toSource() == compressed.toSource()
    }
    def "extra trailing comma works for json container with single pairs"() {
        given:
        JsonContainerExpr expr = JavaParser.parseJsonContainer("""{
            one: 2,
        }""")
        JsonContainerExpr compressed = JavaParser.parseJsonContainer(expr.toSourceCompressed())
        expect:
        expr.toSource() == """{ one : 2 }"""
        expr.toSourceCompressed() == """{one:2}"""
        expr.toSource() == compressed.toSource()
    }
}