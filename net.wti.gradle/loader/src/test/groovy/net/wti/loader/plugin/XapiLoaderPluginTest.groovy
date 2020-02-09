package net.wti.loader.plugin


import net.wti.gradle.internal.api.MinimalProjectView
import net.wti.gradle.schema.map.SchemaMap
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import xapi.util.X_Namespace

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 30/07/19 @ 4:41 AM.
 */
class XapiLoaderPluginTest extends AbstractSchemaTest<XapiLoaderPluginTest> implements MinimalProjectView{

    @Override
    XapiLoaderPluginTest selfSpec() {
        return this
    }

    def "The loader plugin will create all gradle projects in schema xapi file"() {
        given:
        addSourceCommon()
        addSourceUtil()

        when:
        BuildResult res = runSucceed(
                ':util:xapiReport', ':util:compileGwtJava')
        String reportText = file('util', 'build', 'xapiReport', 'report').text
        then:
        res.task(':util:compileGwtJava').outcome == TaskOutcome.SUCCESS
        res.task(':common:compileApiJava').outcome == TaskOutcome.SUCCESS
        reportText.contains "$rootDir/common/src/api/java"
        reportText.contains "$rootDir/util/src/gwt/java"

    }

    def "A SchemaMap can be generated when only the root schema file exists"() {
        given:
        SchemaMap map = parseSchema()
        expect:
        // one root project, five children
        map.allProjects.size() == 6
        map.getRootProject().children.size() == 5
        map.allProjects.collect { it.name }.toSet().containsAll 'xapiTest', 'common', 'util', 'gwt', 'jre', 'demo'
    }

    def "A SchemaMap will pick up schema files in any included module"() {
        given:
        generateSubprojects('jre')
        SchemaMap map = parseSchema()
        expect:
        map.allProjects.size() == 8
        map.allProjects.collect { it.name }.toSet().containsAll 'xapiTest', 'common', 'util', 'gwt', 'jre', 'demo', 'jreMulti', 'jreSingle'
    }

    def "A SchemaMap will collect all external preloads from child schemas"() {
        given:
        generateSubprojects('gwt', """
        <preload
            name = "gwt"
            url = "${System.getProperty('xapi.mvn.repo', "$testRepo")}"
            version = "$X_Namespace.GWT_VERSION"
            // limits these artifacts to gwt platform, where they will be auto-available as versionless dependencies
            // this inheritance is also given to any platform replacing gwt platform.
            platforms = [ "gwt" ]
            modules = [ main ]
            artifacts = {
                "com.google.gwt" : [
                    "gwt-user",
                    "gwt-dev",
                    "gwt-codeserver",
                ]
            }
        /preload>
""")
        SchemaMap map = parseSchema()
        expect:
        map.allProjects.size() == 8
        map.allPreloads.size() == 2
        map.allPreloads.map({it.name}).contains('gwt')
        map.allPreloads.map({it.name}).contains('util')
    }

}
