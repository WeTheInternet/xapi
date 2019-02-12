package net.wti.gradle.internal.api

import net.wti.gradle.test.MultiProjectTestMixin
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.transform.VariantTransform
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification

/**
 * This is a test where we just manually build what our schemas are designed to generate.
 *
 * This will allow us to get a handle on the attribute mapping, and get a set of test
 * cases that we can then apply with automatic attribute mapping.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/27/18 @ 12:53 AM.
 */
class ManualSchemaTest extends Specification implements MultiProjectTestMixin<ManualSchemaTest> {


    String getAttributes() {
        return """
Attribute platform = Attribute.of('xapi-platform', String)
Attribute archive = Attribute.of('xapi-archive', String)
Attribute mode = Attribute.of('xapi-mode', String)
"""
    }

    def setup() {
        buildFile << """
$attributes
allprojects {
    group = 'test'
    version = '1'
    
   dependencies {
       attributesSchema {
          attribute(platform)
          attribute(archive)
          attribute(mode)
       }
   }
   
   configurations {
     xapiRequire.canBeResolved = false
   }
   
   tasks.create 'xapiReport', {
     doLast {
       long start = System.nanoTime()
       logger.quiet '\\nXapi Report:'
       
       configurations.all { c->
         logger.quiet '\\tConfiguration {}:', c.name
         logger.quiet '\\t\\tFiles: ', c.canBeResolved ? c.resolve() : '<not resolvable>'
         logger.quiet '\\t\\tDebug:', c.dump()
       }
       
       logger.quiet 'Report finished in {}ns\\n\\n', System.nanoTime() - start
     }
   }
}
"""
        withProject('a') {
            buildFile << subproject('a')
            withSource 'main', {
                com {
                    a {
                        'AMain.java'('''
package com.a;

class AMain {
}
''')
                    }
                }
            }
        }
        withProject('b') {
            buildFile << subproject('b')
            withSource 'main', {
                com {
                    b {
                        'BMain.java'('''
package com.b;

class BMain {
}
''')
                    }
                }
            }
        }
    }

    String subproject(String projectName, String defaultConfig = 'xapiMain') {
        return """
$attributes

configurations {
  xapiMain.attributes {
    attribute(platform, 'main')
    attribute(archive, 'main')
  }
  xapiMainSource.attributes {
    attribute(platform, 'main')
    attribute(archive, 'source')
  }
  xapiMainApi.attributes {
    attribute(platform, 'main')
    attribute(archive, 'api')
  }
  xapiMainApiSource.attributes {
    attribute(platform, 'main')
    attribute(archive, 'api-source')
  }
  xapiGwt.attributes {
    attribute(platform, 'gwt')
    attribute(archive, 'main')
  }
  xapiGwtSource.attributes {
    attribute(platform, 'gwt')
    attribute(archive, 'source')
  }
  xapiGwtApi.attributes {
    attribute(platform, 'gwt')
    attribute(archive, 'api')
  }
  xapiGwtApiSource.attributes {
    attribute(platform, 'gwt')
    attribute(archive, 'api-source')
  }
  // gwt uses extension (for now); j2cl uses transformation.
  // in the future, we may also un-super gwt and give it a j2cl-like classworld.
  // Would rather just migrate to j2cl and drop gwt though...
  xapiMain.extendsFrom xapiMainApi
  xapiGwt.extendsFrom xapiMain, xapiGwtSource
  xapiGwtSource.extendsFrom xapiMainSource
  xapiGwtApi.extendsFrom xapiMainApi, xapiMainApiSource
  
  xapiMain.extendsFrom xapiRequire
  xapiMainSource.extendsFrom xapiRequire
  xapiMainApi.extendsFrom xapiRequire
  xapiMainApiSource.extendsFrom xapiRequire
  xapiGwt.extendsFrom xapiRequire
  xapiGwtSource.extendsFrom xapiRequire
  xapiGwtApi.extendsFrom xapiRequire
  xapiGwtApiSource.extendsFrom xapiRequire
}
"""
    }

    def "Source artifacts have independent transitive dependency graph"() {
        setup:
            getProject('a').buildFile << '''
dependencies {
  xapiRequire project(':b')
  xapiGwtApi 'test:b:1'
}
'''
            getProject('b').buildFile << '''
//dependencies {
//    registerTransform {
//        from.attribute(archive, 'main')
//        from.attribute(platform, 'main')
//        to.attribute(archive, 'api')
//        to.attribute(platform, 'gwt')
//    }
//}
'''
        DependencyHandler d;
        when:
            def result = runSucceed('a:xapiReport')
        then:
            result.task(':a:xapiReport').outcome == TaskOutcome.SUCCESS
    }

    @Override
    ManualSchemaTest selfSpec() {
        return this
    }
}
