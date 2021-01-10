package xapi.mvn.impl

import spock.lang.Specification

class MvnServiceDefaultTest extends Specification {

    def "We can see our own published pom"() {
        // This test case won't pass on a "normal" project,
        // but we added: `testClasses.dependsOn preparePublishing`,
        // so we are able to find our own published metadata.
        MvnServiceDefault mvn = new MvnServiceDefault()
        URL self = MvnServiceDefaultTest?.protectionDomain?.codeSource?.location

    }
}
