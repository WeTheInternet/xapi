package net.wti.gradle.internal.api

import net.wti.gradle.test.MultiProjectTestMixin
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/17/18 @ 4:42 AM.
 */
class XapiLibraryTest extends Specification implements MultiProjectTestMixin<XapiLibraryTest>
{

    def "Gwt platform automatically inherits own project sources"() {
        setup:
            withProject ':', {
                buildFile << schemaNoArchives()
            }

            withProject('gwt1', {
                buildFile << """
plugins { 
    id 'java'
    id 'xapi-schema'
}
"""
                addSource('com.foo', 'Main1', '''
package com.foo;

public class Main1 {
  public static void main(String ... a) {
    System.out.println("Hi!");
  }
}
''')
                addSource('gwt', 'com.gwt', 'Gwt1', '''
package com.gwt;

class Gwt1 {
  public static void main(String ... a) {
    com.foo.Main1.main(a);
  }
}
''')
            })

        when:
            BuildResult res = runSucceed(LogLevel.INFO, 'compileGwtJava', 'xapiReport', '-Pxapi.debug=true')
        then:
            res.task(':gwt1:compileGwtJava').outcome == TaskOutcome.SUCCESS
            res.task(':gwt1:compileJava').outcome == TaskOutcome.SUCCESS
            res.output.contains "$rootDir/gwt1/src/main/java"
    }

    String schemaNoArchives() {
'''
plugins {
  id 'xapi-schema'
}
xapiSchema {
  platforms {
    main
    gwt {
      replace main
    }
  }
  archives {
    main
    api
  }
}
'''
    }

    String schemaWithArchives() {
'''
plugins {
  id 'xapi-schema'
}
xapiSchema {
  platforms {
    main
    gwt {
      replace main
    }
  }
  archives {
    main
  }
}
'''
    }

    def declareAttributes() {
        """
            def usage = Attribute.of('usage', String)
            def artifactType = Attribute.of('artifactType', String)
            def platformType = Attribute.of('platformType', String)
                
            allprojects {
                dependencies {
                    attributesSchema {
                        attribute(usage)
                    }
                }
                configurations {
                    compile {
                        attributes.attribute usage, 'api'
                    }
                }
            }
        """
    }

    @Override
    XapiLibraryTest selfSpec() {
        return this
    }
}
