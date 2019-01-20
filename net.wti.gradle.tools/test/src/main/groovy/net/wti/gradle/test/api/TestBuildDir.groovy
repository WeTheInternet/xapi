package net.wti.gradle.test.api
/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/15/19 @ 1:45 AM.
 */
trait TestBuildDir extends TestProjectDir implements HasBuildFiles {

    private File settingsFile

    File getSettingsFile() {
        this.@settingsFile ?: (this.@settingsFile = file('settings.gradle'))
    }

}
