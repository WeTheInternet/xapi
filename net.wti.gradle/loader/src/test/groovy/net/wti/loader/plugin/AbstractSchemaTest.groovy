package net.wti.loader.plugin

import net.wti.gradle.api.MinimalProjectView
import net.wti.gradle.schema.map.SchemaMap
import net.wti.gradle.schema.parser.DefaultSchemaMetadata
import net.wti.gradle.schema.parser.SchemaParser
import net.wti.gradle.settings.ProjectDescriptorView
import net.wti.gradle.test.AbstractMultiBuildTest
import org.gradle.api.initialization.Settings

import java.util.concurrent.Callable

import static xapi.util.X_Namespace.XAPI_VERSION

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 30/07/19 @ 4:41 AM.
 */
abstract class AbstractSchemaTest <S extends AbstractSchemaTest> extends AbstractMultiBuildTest<S> implements MinimalProjectView {

    static final String VERSION = "1.51"
    protected String extraProjects
    protected List<CharSequence> pluginList

    String getTestRepo() {
        return "$rootDir.absolutePath/build/test/repo"
    }

    Callable initProject
    @Override
    void doWork() {
        initProject.call()
        initProject = {}
        super.doWork()
    }

    @Override
    void setup() {
        version = VERSION
        group = rootProjectName
        pluginList = initPlugins()
        initProject = {
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
plugins {
${massagePlugins(pluginList)}
}
allprojects {
    version = "$VERSION"
    group = "${rootProjectName}"
}
"""

            // create a basic schema.xapi
            file('schema.xapi').text = """
<xapi-schema
    name = "$rootProjectName"
    version = "$VERSION"
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
        <main include = [ api, "spi" ] />,
        <test include = [ main ] />,
    ]
    projects = {
        // the projects below all have a single "main" platform (potentially w/ multiple modules like api and testTools though!)
        standalone: [
            "common",
        ],
        // the projects below all have gwt, jre and other platforms
        multiPlatform: [
            "util", "consumer", "producer" ${extraProjects ?: ''}
        ],
        // the projects below have no source of their own; they are effectively parents of multiple child projects.
        // it will be left to the schema.xapi of these projects to determine whether
        // the child modules are multiPlatform, standalone, or nested virtual
        virtual: ["gwt", "jre" ],
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
    }

    protected static String massagePlugins(List<CharSequence> list) {
    return "    " + list.collect {
        item ->
            item.startsWithAny("id ") ? item : item.startsWithAny('"', "'") ?
                    "id $item" : "id \"$item\""
        }.join("\n    ")
    }

    String getExtraProjects() {
        return this.@extraProjects ?: ''
    }

    @Override
    String getRootProjectName() {
        return getClass().simpleName
    }

    Settings settings = Mock(Settings, {
        // initialize the mock with some state...
        Map<String, ProjectDescriptorView> mockProjects = new LinkedHashMap<>();
        settings.findProject(_) >> { args ->
            Object key = args[0]
            if (key instanceof File) {
                throw new UnsupportedOperationException("Only string keys are allowed for settings.findProject during tests.")
            }
            if (!key.startsWith(':')) {
                key = ":$key"
            }
            final String k = "${ key.startsWith(':') ? '' : ':'}$key"
            File projectDir = new File(rootDir, k.substring(1).replace(':' as char, File.separatorChar))
            mockProjects.computeIfAbsent(k, { K ->
                     new ProjectDescriptorView(k, settings, mockProjects, {projectDir})
            })
        }

    })

    SchemaMap parseSchema() {
        // make sure we actually write out any generated project files before we try parsing
        doWork()
        SchemaParser parser = {this} as SchemaParser
        XapiLoaderPlugin plugin = new XapiLoaderPlugin()
        DefaultSchemaMetadata schema = parser.parseSchema(this)
        return plugin.buildMap(settings, parser, schema)
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

    void addSourceCommon(boolean withParser = true) {
        withProject'common', {
            it.buildFile << """
version='$VERSION'${withParser ? """
apply plugin: 'xapi-parser'""" : ''}
"""
            it.addSource 'api', 'test.common.api', 'CommonApi', '''
package test.common.api;
public class CommonApi {}
'''
        }
    }

    void addSourceUtil(boolean xapiRequire = false, boolean extraDeps = true, boolean noPlugins = false) {
        withProject 'util', {
            it.buildFile << """
version='$VERSION'
apply plugin: '${xapiRequire ? '''xapi-require'
xapiRequire.project 'common''' : noPlugins ? 'base' : 'xapi-parser'}'
"""
            if (!xapiRequire) {
                // create a util/schema.xapi which dependsOn common...
                it.file('schema.xapi').text = """<xapi-schema
    requires = {
        project: common
    }
    platforms = [
        <gwt
            requires = {
                ${extraDeps ? 'external: "net.wti:gwt-user:3.0"' : ""}
            }
        /gwt>
    ]
    modules = [
        <main
            requires = {
                ${extraDeps ? 'external: { "tld.ext:art:1.0" : "api" },' : ""}
                ${extraDeps ? 'external: [ "tld.ext:ifact:1.0" ]' : ""}
            }
        /main>,
        <testIntegration
            // this include makes this module "transitively extend" 'test' module.
            // anyone consuming testIntegration will inherit test.
            include = [ "test" ]
            // these internal "extensions" are present at runtime within the scope of this xapi-schema document,
            // but are otherwise non-transitive to the outside world (downstream consumers can just add the desired pieces they want).
            // nobody consuming testIntegration will inherit requires={ internal: [...] } entries
            requires = { internal : [ "jre", gwt, "android" ] }
        /testIntegration>,
    ]${!extraDeps ? '' : """
    projects = [
        <some-project
            requires = {
                // applies to all modules of some-project
                project: { "other-proj" : "main:api" },
                project: [ "another-proj" ],

                // applies only to the api module of some-project
                api: {
                    project: { "other-proj" : "spi" }
                },
                
                // shorthand for test: { project: [ "another-proj", "main:test" ] },
                // add a project dependency to main test module of another-proj.
                project_test: [ "another-proj", "main:test" ],
            }
        /some-project>
    ]
"""}
/xapi-schema>
"""
            }
            it.addSource 'gwt', 'test.util.main', 'UtilMain', '''
package test.util.main;
class UtilMain extends test.common.api.CommonApi {}
'''
        }
    }

    List<CharSequence> initPlugins() {
        []
    }
}
