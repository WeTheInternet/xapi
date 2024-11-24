package com.github.javaparser

import com.github.javaparser.ast.expr.JsonContainerExpr
import com.github.javaparser.ast.expr.UiContainerExpr

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
    def "empty json container does not have errors"() {
        given:
        JsonContainerExpr expr = JavaParser.parseJsonContainer("""[ ]""")
        JsonContainerExpr compressed = JavaParser.parseJsonContainer(expr.toSourceCompressed())
        expect:
        expr.toSource() == """[]"""
        expr.toSourceCompressed() == """[]"""
        expr.toSource() == compressed.toSource()
    }

    def "debug gwtc.xapi"() {
        given:
        String xapiSchema = """<xapi-schema
    inherit = false
    platforms = [
        <main
            modules = [
                <api
                    description = "Xapi - Gwt Compiler Api"
                    requires = {
                        @transitive(compile_only)
                        external : "net.wti.gradle:xapi-gradle-api:\$version",
                        project : { model : main, process : main, io : main, collect: main, inject : jre },
                        project : { ":ui:service" : main, ":dev:api" : main },
                        project : { ":dev:file" : main },
                        external : "net.wetheinter:gwt-user:2.8.0",
                        project : { ":dev:scanner": main },

                        @transitive(compile_only)
                        external : "junit:junit:4.12",
                        @transitive(compile_only)
                        external : "net.wetheinter:gwt-dev:2.8.0",
                        @transitive(compile_only)
                        external : "net.wetheinter:gwt-user:2.8.0",

                        @transitive(annotation_processor)
                        external : "net.wti.gradle:xapi-dev-mirror:\$version",
                    }
                /api>,
                <compiler
                    requires = {
                        internal : api,
                        project : {
                            inject : jre,
                            "dev:api" : main,
                            model : main,
                            ":xapi-dev-parent:xapi-dev-shell" : main,
                        },
                        @transitive(compile_only)
                        project : { base : gwtTestTools },

                        @transitive(compile_only)
                        external : "junit:junit:4.13", // 12345678901234567890123456789012345678
                        external : [
                            "net.wetheinter:gwt-dev:2.8.0",
                            "org.eclipse.jetty:jetty-server:9.2.14.v20151106",
                            "net.wetheinter:gwt-user:2.8.0",
                            "net.wti.core:xapi-fu:\$version",
                            "net.wti.core:xapi-lang-core:\${version}"
                        ],
                    }
                /compiler>
            ]
        /main>,
        <gwt
            description = "Xapi - GWT client to run xapi-gwt-compiler"
            replaces=main
            modules = [
                <testTools
                    requires = {
                        @transitive
                        project : { ":gwtc-compiler" : main },
                        @transitive
                        external : [
                            "net.wetheinter:gwt-user:2.8.0",
                            "net.wetheinter:gwt-dev:2.8.0",
                        ],
                    }
                /testTools>,
                <compiler
                    requires = {
                        @transitive(test)
                        internal : gwtTestTools,
                        @transitive(test)
                        project : {
                            gwt : gwtUber,
                            inject : jre,
                            base : gwtTestTools,
                            ":dev:maven" : main,
                        }
                    }
                /compiler>,
                <sample
                    requires = {
                        @transitive(test)
                        internal : gwtTestTools,
                        project : {
                            gwtc : api,
                            inject : main,
                            ":dev:maven" : main,
                        },
                        @transitive(annotation_processor)
                        project : {
                            ":ui:generator" : gwt,
                            "inject" : "jre",
                            ":xapi-dev-parent:xapi-dev-javac" : main,
                        },
                        external: [
                            "net.wetheinter:gwt-user:2.8.0",
                        ],

                    }
                /sample>
            ]
        /gwt>
    ]
/xapi-schema>"""
        UiContainerExpr expr = JavaParser.parseUiContainer(xapiSchema)
        expect:
        // we don't really care; just making sure it parses
        expr.name == 'xapi-schema'
    }

    def "debug ui.xapi"() {
        given:
        String xapiSchema = '''<xapi-schema
    inherit = true
    multiplatform = true
    virtual = true

    modules = [
        <api />,
        <spi />,
        <main include = [ api, spi ] />,
        <testTools include = "main" published = true />,
        <sample
            include = "main"
            force = true
            published = true />,
    ]

    projects = {
        // the projects below all have gwt, jre and other platforms
        multiplatform: [
            <service
                description = "XApi - UI service layer"
                platforms = [
                    <main
                        modules = [
                            <main
                                requires = {
                                    @transitive
                                    project : [ ":ui:api" ],
                                    @transitive
                                    project : ":xapi-lang-dev",
                                    project : [ ":dev:file", ":core:scope", io, util ],
                                    @transitive
                                    project : ":core:event",
                                    @transitive
                                    project : ":model",
                                    project : ":dev:scanner",
                                    external : "javax.validation:validation-api:1.0.0.GA",
                                    @transitive(compile_only)
                                    external : "net.wetheinter:jsinterop-annotations:2.8.0",

                                    @transitive(test)
                                    project : { model : jre },
                                    @transitive(test)
                                    project : { base : testTools },
                                    @transitive(test)
                                    external : [ "junit:junit:4.12", "org.assertj:assertj-core:3.2.0" ],
                                }
                            /main>
                        ]
                    /main>,
                    <gwt
                        replaces = main
                        modules = [
                            <main
                                requires = {
                                    project : {
                                        //":ui:service" : main,
                                        collect : main
                                    },
                                    @transitive(compile_only)
                                    external : "net.wetheinter:gwt-user:2.8.0"
                                }
                            /main>
                        ]
                    /gwt>
                ]
            /service>,
            <autoui
                inherit = true
                platforms = [
                    <main
                        modules = [
                            <main
                                requires = {
                                    project : [ util, collect, ":ui:service" ],
                                    external : "javax.inject:javax.inject:1:sources",
                                }
                            /main>,
                        ]
                    /main>,
                    <gwt
                        modules = [
                            <main
                                requires = {
                                    project : {
                                        ":collect" : main,
                                        ":ui:service" : main,
                                        ":util" : main,
                                        ":core:event" : main,
                                        ":ui:autoui" : main,
                                        ":ui:html" : main,
                                        ":model" : gwt,
                                        ":inject" : jre,
                                    },
                                    @transitive(compile_only)
                                    external : [
                                        "net.wetheinter:gwt-dev:2.8.0",
                                        "net.wetheinter:gwt-user:2.8.0",
                                    ],
                                    @transitive(test)
                                    project : {
                                        ":ui:autoui" : sample,
                                        base : gwtTestTools,
                                    },
                                    @transitive(test)
                                    external : [
                                        "net.wetheinter:gwt-dev:2.8.0",
                                        "net.wetheinter:gwt-user:2.8.0",
                                        "net.wetheinter:gwt-codeserver:2.8.0",
                                        "net.sourceforge.htmlunit:htmlunit:2.19",
                                    ]
                                }
                            /main>
                        ]
                    /gwt>
                ]
            /autoui>,
            <html
                inherit = true
                description = "Xapi - Core html utils"
                platforms = [
                    <main
                        modules = [
                            <main
                                requires = {
                                    project : [ collect, ":ui:autoui", ":ui:service", util ],
                                    @transitive(compile_only)
                                    external : "net.wetheinter:gwt-user:2.8.0",

                                    @transitive(test)
                                    project : {
                                        inject : jre,
                                        base : testTools,
                                    },
                                    @transitive(test)
                                    external : "junit:junit:4.12",
                                }
                            /main>,
                            <sample requires = {
                                    project : ":ui:autoui"
                            } /sample>
                        ]
                    /main>,
                    <gwt
                        modules = [
                            <main
                                requires = {
                                    @transitive
                                    project : {
                                        ":ui:autoui" : gwt,
                                        ":ui:service" : gwt,
                                    },
                                    project : {
                                        ":ui:html" : main,
                                        inject : jre,
                                        util : main,
                                    },
                                    external : "net.wetheinter:gwt-elemental:2.8.0",
                                    @transitive(compile_only)
                                    external : [
                                        "net.wetheinter:gwt-dev:2.8.0",
                                        "net.wetheinter:gwt-codeserver:2.8.0",
                                        "net.wetheinter:gwt-user:2.8.0",
                                    ],
                                    @transitive(test)
                                    project : {
                                        ":ui:autoui" : sample,
                                        base : testTools,
                                    },
                                    external : [
                                        "net.sourceforge.htmlunit:htmlunit:2.19",
                                        "net.wetheinter:gwt-dev:2.8.0",
                                        "net.wetheinter:gwt-codeserver:2.8.0",
                                    ],
                                }
                            /main>
                        ]
                    /gwt>
                ]
            /html>,
        ],

        // the projects below are single-module projects.
        standalone: [
            <api
                inherit = false
                modules = <main
                    requires = {
                        @transitive
                        project : { base : api },
                        @transitive
                        external : "javax.validation:validation-api:1.0.0.GA",
                    }
                /main>
            /api>,
            <components
                inherit = false
                description = "Xapi - UI component types"
                requires = {
                    project : ":ui:service",

                    @transitive(test)
                    project : {
                        inject : jre,
                        base : testTools,
                    },
                    @transitive(test)
                    external : "junit:junit:4.12",
                }
            /components>,
            <generator
                inherit = false
                description = "Xapi - Core UI Generator"
                requires = {
                    project : [
                        ":ui:service",
                        "xapi-dev-parent:xapi-dev-javac",
                    ],
                    @transitive
                    project : ":xapi-lang-dev",
                    @transitive(test)
                    external : [
                        "junit:junit:4.12", "org.assertj:assertj-core:3.2.0"
                    ],
                    @transitive(test)
                    project : [
                        ":inject-jre",
                        ":base-testTools",
                    ],
                }
            /generator>,
        ],
    }
/xapi-schema>
'''
        UiContainerExpr expr = JavaParser.parseUiContainer(xapiSchema)
        expect:
        // we don't really care; just making sure it parses
        expr.name == 'xapi-schema'
    }
    def "comments after 4096 characters do not cause a parse error"() {
        given:
        String ten = "1234567890"
        String filler = ten * 405 // 26 chars left

        when:
        JsonContainerExpr expr = JavaParser.parseJsonContainer("""{x:2, //$filler
}""")
        JsonContainerExpr compressed = JavaParser.parseJsonContainer(expr.toSourceCompressed())
        then:
        expr.toSource() == """{ x : 2 }"""
        expr.toSourceCompressed() == """{x:2}"""
        expr.toSource() == compressed.toSource()

        when:
        UiContainerExpr ui = JavaParser.parseUiContainer("""<wrap per = {
//$filler
@ano
x:12345$ten, // killer $ten
} />""")
        println """<wrap per = {
//$filler
x:12345$ten, """.length()
        println "Parsing: " + ui.toSourceCompressed()
        UiContainerExpr compressedUi = JavaParser.parseUiContainer(ui.toSourceCompressed())
        then:
        ui.toSource() == """<wrap
  per = { x : 12345$ten }/>"""
        ui.toSourceCompressed() == """<wrap per={x:12345$ten}/>"""
        ui.toSource() == compressedUi.toSource()
    }
}