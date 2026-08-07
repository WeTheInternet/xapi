package net.wti.gradle.settings.api

import spock.lang.Specification
import spock.lang.Unroll

class SchemaPathCodecTest extends Specification {

    @Unroll
    def "filesystem name #logical is portable and reversible"() {
        when:
        String encoded = SchemaPathCodec.encodeFileName(logical)

        then:
        SchemaPathCodec.isPortableFileName(encoded)
        SchemaPathCodec.decodeFileName(encoded) == logical

        where:
        logical << [
            'junit:junit:4.13',
            ':ui:producer',
            'CON',
            'nul.txt',
            'trailing.',
            'trailing ',
            '~already-escaped',
            'unicode-λ'
        ]
    }

    def "escaping cannot collide with an existing portable name"() {
        expect:
        SchemaPathCodec.encodeFileName('a:b') != SchemaPathCodec.encodeFileName('a_b')
        SchemaPathCodec.encodeFileName(':ui:producer') != SchemaPathCodec.encodeFileName('_ui_producer')
        SchemaPathCodec.encodeFileName('CON') != SchemaPathCodec.encodeFileName('~434f4e')
    }

    def "portable names remain readable"() {
        expect:
        SchemaPathCodec.encodeFileName('main') == 'main'
        SchemaPathCodec.encodeFileName('gwtTestTools') == 'gwtTestTools'
    }

    @Unroll
    def "malformed escaped name #encoded is rejected"() {
        when:
        SchemaPathCodec.decodeFileName(encoded)

        then:
        thrown(IllegalArgumentException)

        where:
        encoded << ['~0', '~zz']
    }
}
