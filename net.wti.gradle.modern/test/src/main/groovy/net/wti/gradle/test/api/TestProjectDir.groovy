package net.wti.gradle.test.api
/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/26/18 @ 2:45 AM.
 */
trait TestProjectDir implements HasProjectFiles {

    private File buildFile

    File getBuildFile() {
        this.@buildFile ?: (this.@buildFile = file('build.gradle'))
    }

    private File propertiesFile

    File getPropertiesFile() {
        this.@propertiesFile ?: (this.@propertiesFile = file('gradle.properties'))
    }

}
