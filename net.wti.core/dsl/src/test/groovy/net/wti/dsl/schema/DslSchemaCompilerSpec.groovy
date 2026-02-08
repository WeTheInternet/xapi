package net.wti.dsl.schema

import net.wti.dsl.parser.DslModel
import net.wti.dsl.parser.DslParser
import net.wti.dsl.test.AbstractDslSpec
import net.wti.dsl.type.DslTypeEnum
import net.wti.dsl.type.DslTypeJson
import net.wti.dsl.type.DslTypeString

///
/// DslSchemaCompilerSpec:
///
/// Verifies behavior of {@link DslSchemaCompiler} using “reader-first” tests:
///  - compile known-good schema files and inspect the resulting compiled schema graph,
///  - compile targeted “edge case” schemas and assert precise failures.
///
/// This test suite is intentionally comprehensive and opinionated:
/// it exists to prevent accidental semantic drift when we evolve the schema language.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 03:30
final class DslSchemaCompilerSpec extends AbstractDslSpec {

    private static DslSchema compileResource(final String resourcePath) {
        final DslParser parser = new DslParser()
        final DslModel model = parser.parseResource(resourcePath)
        return new DslSchemaCompiler().compile(model)
    }

    // -------------------------------------------------------------------------
    // Reader-first: compile known DSLs and inspect schema graph
    // -------------------------------------------------------------------------

    def "compiling simple-dsl produces a schema with root tag and element registry"() {
        when:
        final DslSchema schema = compileResource("META-INF/xapi/simple-dsl.xapi")

        then:
        schema != null
        schema.getName() != null
        schema.getPackageName() != null
        schema.getRootTag() != null

        and: "root tag exists as an element"
        schema.getElement(schema.getRootTag()) != null

        and: "elements map is non-empty"
        !schema.getElements().isEmpty()
    }

    def "compiling test-dsl captures enum and json attribute types"() {
        when:
        final DslSchema schema = compileResource("META-INF/xapi/test-dsl.xapi")

        then: "schema compiles and root exists"
        schema.getElement(schema.getRootTag()) != null

        and: "enum type is preserved with expected values"
        final DslSchemaElement child = schema.getElement("child")
        child != null
        final DslSchemaAttribute kindAttr = child.getAttribute("kind")
        kindAttr != null
        kindAttr.getType() instanceof DslTypeEnum
        ((DslTypeEnum) kindAttr.getType()).getValues() == ["alpha", "beta", "gamma"]

        and: "json type is supported"
        final DslSchemaElement config = schema.getElement("config")
        config != null
        final DslSchemaAttribute rawAttr = config.getAttribute("raw")
        rawAttr != null
        rawAttr.getType() instanceof DslTypeJson
    }

    def "schema-level attribute registry contains and reuses element attributes by name (promotion behavior)"() {
        when:
        final DslSchema schema = compileResource("META-INF/xapi/schema-attrs-promote.xapi")

        then: "attributes promoted into top-level registry"
        schema.getAttributes().containsKey("name")
        schema.getAttributes().containsKey("title")

        and: "element attribute references reuse the canonical schema attribute instance"
        final DslSchemaAttribute nameAttr = schema.getAttribute("name")
        nameAttr != null

        final DslSchemaElement a = schema.getElement("a")
        final DslSchemaElement b = schema.getElement("b")
        a != null && b != null

        a.getAttribute("name").is(nameAttr)
        b.getAttribute("name").is(nameAttr)
    }

    def "explicit schema-level attributes are reused by element definitions that reference the same name"() {
        when:
        final DslSchema schema = compileResource("META-INF/xapi/schema-attrs-explicit.xapi")

        then: "schema has explicit attributes"
        schema.getAttributes().containsKey("name")
        schema.getAttributes().containsKey("labels")

        and: "elements reuse those instances"
        final DslSchemaAttribute nameAttr = schema.getAttribute("name")
        final DslSchemaElement a = schema.getElement("a")
        final DslSchemaElement b = schema.getElement("b")

        a.getAttribute("name").is(nameAttr)
        b.getAttribute("name").is(nameAttr)

        and: "types are as declared"
        nameAttr.getType() instanceof DslTypeString
    }

    // -------------------------------------------------------------------------
    // Contract tests: failure modes (these define current semantics)
    // -------------------------------------------------------------------------

    def "conflicting required= across schema-level and element attribute definitions fails"() {
        when:
        compileResource("META-INF/xapi/schema-attrs-conflict-required.xapi")

        then:
        final IllegalArgumentException ex = thrown()
        ex.message.contains("Conflicting required=")
    }

    def "conflicting default= presence across schema-level and element attribute definitions fails"() {
        when:
        compileResource("META-INF/xapi/schema-attrs-conflict-default.xapi")

        then:
        final IllegalArgumentException ex = thrown()
        ex.message.contains("Conflicting default=")
    }

    def "conflicting type class across schema-level and element attribute definitions fails"() {
        when:
        compileResource("META-INF/xapi/schema-attrs-conflict-type.xapi")

        then:
        final IllegalArgumentException ex = thrown()
        ex.message.contains("Conflicting type")
    }

    def "rootTag must match an element-def entry"() {
        when:
        compileResource("META-INF/xapi/schema-bad-rootTag.xapi")

        then:
        final IllegalArgumentException ex = thrown()
        ex.message.contains("rootTag=") || ex.message.contains("rootTag='")
    }
}
