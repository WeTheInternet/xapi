package net.wti.lang.parser

import spock.lang.Specification

import java.nio.charset.StandardCharsets

class SignedUiAttributeSpec extends Specification {

    def "XApi UI attributes accept signed integer literals"() {
        when:
        def element = JavaParser.parseXapi(new ByteArrayInputStream(
            '<cell x = -2 y = 10 />'.getBytes(StandardCharsets.UTF_8)))

        then:
        element.getAttributeRequiredString('x') == '-2'
        element.getAttributeRequiredString('y') == '10'
    }
}
