package net.wti.dsl.graph;

import net.wti.dsl.type.DslTypeName
import net.wti.dsl.type.DslTypeString
import net.wti.dsl.type.DslTypeTypedMap
import net.wti.dsl.value.DslValueString
import net.wti.lang.parser.ast.expr.Expression
import spock.lang.Specification
import xapi.fu.itr.MappedIterable

///
/// DslNodeTypedMapSpec:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 00:22
final class DslNodeTypedMapSpec extends Specification {

    private static MappedIterable<Expression> syntheticSourceAst() {
        return MappedIterable.mapped(Collections.<Expression>emptyList())
    }

    def "DslNodeTypedMap preserves encounter order and allows repeated keys"() {
        given:
        def src = syntheticSourceAst()
        def stringType = new DslTypeString(src)
        def nameType = new DslTypeName(src)

        def schema = new DslTypeTypedMap(src, [
                internal: nameType,
                project : stringType,
        ])

        and:
        DslValueString v1 = new DslValueString(src, stringType, "a")
        DslValueString v2 = new DslValueString(src, stringType, "b")
        DslValueString v3 = new DslValueString(src, stringType, "c")
        def e1 = new DslNodeTypedMap.Entry("project", v1)
        def e2 = new DslNodeTypedMap.Entry("project", v2)
        def e3 = new DslNodeTypedMap.Entry("project", v3)
        when:
        def node = new DslNodeTypedMapImmutable(src, schema, [e1,e2,e3])

        then:
        node.getEntries()*.key == ["project", "project", "project"]
        node.getValues("project")*.dslValue == ["a", "b", "c"]
        node.getDeclaredType("project").is(stringType)
        node.getValues("missing").isEmpty()
    }

    def "DslNodeTypedMap rejects keys not declared in schema"() {
        given:
        def src = syntheticSourceAst()
        def stringType = new DslTypeString(src)

        def schema = new DslTypeTypedMap(src, [
                project: stringType
        ])

        and:
        def v = new DslValueString(src, stringType, "x")

        when:
        new DslNodeTypedMapImmutable(src, schema, [
                new DslNodeTypedMap.Entry("nope", v),
        ])

        then:
        def err = thrown(IllegalArgumentException)
        err.message.contains("not declared")
    }
}
