package net.wti.loader.plugin

import net.wti.gradle.internal.api.MinimalProjectView
import net.wti.gradle.schema.map.SchemaMap
import net.wti.gradle.schema.parser.SchemaMetadata
import net.wti.gradle.schema.parser.SchemaParser
import net.wti.gradle.test.AbstractMultiBuildTest
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.internal.extensibility.DefaultConvention
import org.gradle.internal.reflect.DirectInstantiator

import static xapi.util.X_Namespace.XAPI_VERSION

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 30/07/19 @ 4:41 AM.
 */
abstract class AbstractSchemaTest <S extends AbstractSchemaTest>  extends AbstractMultiBuildTest<S> implements MinimalProjectView {

    private DefaultConvention convention

    String getTestRepo() {
        return "$rootDir.absolutePath/build/test/repo"
    }

    void setup() {
        convention = new DefaultConvention(DirectInstantiator.INSTANCE);

        settingsFile.text = """
buildscript {
    dependencies {
        classpath files(
            "${new File(".", "build/testRuntime.classpath").text.replaceAll("[:;]", "\", \"")}"
        )
    }
}
apply plugin: 'xapi-loader'
"""

        buildFile.text = """
buildscript {
    dependencies {
        classpath files(
            "${new File(".", "build/testRuntime.classpath").text.replaceAll("[:;]", "\", \"")}"
        )
    }
}
version = "1.0"
"""

        // create a basic schema.xapi
        file('schema.xapi').text = """
<xapi-schema
    rootName = "xapiTest"
    defaultRepoUrl = "$testRepo"
    schemaLocation = "schema/schema.gradle"
    platforms = [
        <main />,
        <jre replace = "main" published = true/>,
        <gwt replace = "main" published = true/>,
    ]
    modules = [
        <api />,
        <spi />,
        <main require = [ api, "spi" ] />,
        <test require = [ main ] />,
    ]
    projects = {
        // the projects below all have a single "main" platform (potentially w/ multiple modules like api and testTools though!)
        standalone: [
            "common",
        ],
        // the projects below all have gwt, jre and other platforms
        multiPlatform: [
            "util"
        ],
        // the projects below have no source of their own; they are effectively parents of multiple child projects.
        // it will be left to the schema.xapi of these projects to determine whether
        // the child modules are multiPlatform, standalone, or nested virtual
        virtual: ["gwt", "jre", "demo"],
    }
    // declare any external dependencies here,
    // so we can handle pre-emptively syncing jars (and maybe source checkouts) to a local cache,
    // then just reference these "blessed artifacts" w/out versions anywhere;
    // leaving it up to the system
    external = [
        // preload elements will be downloaded once, on build startup, into a local filesystem repository (xapi.repo)
        <preload
            name = "util"
            url = "${System.getProperty('xapi.mvn.repo', 'https://wti.net/repo')}"
            version = "${System.getProperty('xapi.version', XAPI_VERSION)}"
            platforms = [ "main" ]
            modules = [ main ]
            artifacts = {
                "net.wti.core" : [
                    "xapi-fu",
                ]
            }
        /preload>
        ,
    ]
/xapi-schema>

"""
        file('schema', 'schema.gradle').text = """
plugins {
    id 'xapi-schema'
}

tasks.create 'testSchema', {
    doLast {
        logger.quiet "\$xapiSchema"
    }
}
"""

    }

    @Override
    File getProjectDir() {
        return rootDir
    }

    @Override
    Object findProperty(String s) {
        return System.getProperty(s, System.getenv(s.toUpperCase().replace('.', '_')));
    }

    @Override
    ExtensionContainer getExtensions() {
        return convention
    }

    SchemaMap parseSchema() {
        SchemaParser parser = {this} as SchemaParser
        XapiLoaderPlugin plugin = new XapiLoaderPlugin()
        SchemaMetadata schema = parser.parseSchema(this)
        return plugin.buildMap(parser, schema)
    }

    void generateSubprojects(String module='util', external = '') {
        file(module, 'schema.xapi').text = """
<xapi-schema
    projects = {
        multiPlatform: [
            "${module}Multi",
        ],
        standalone: [
            "${module}Single"
        ],
    }
    ${!external ? '' : """external = [
        ${external.trim()}
    ]"""}
/xapi-schema>
"""
    }

    void addSourceCommon() {
        withProject'common', {
            it.buildFile << '''
version='1.0'
apply plugin: 'xapi-parser'
'''
            it.addSource 'api', 'test.common.api', 'CommonApi', '''
package test.common.api;
public class CommonApi {}
'''
        }
    }

    void addSourceUtil() {
        withProject 'util', {
            it.buildFile << '''
version='1.0'
apply plugin: 'xapi-require'
xapiRequire.project 'common'
'''
            it.addSource 'gwt', 'test.util.main', 'UtilMain', '''
package test.util.main;
class UtilMain extends test.common.api.CommonApi {}
'''
        }
    }
}
