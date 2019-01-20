package net.wti.gradle.test.api

import org.gradle.util.GFileUtils
/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/15/19 @ 2:32 AM.
 */
class IncludedTestBuild implements TestBuild, TestFileTools, TestBuildDir {

    private TestBuild parent

    IncludedTestBuild(TestBuild parent, String name) {
        setName(name)
        this.parent = parent
        GFileUtils.touch(settingsFile)
    }

    @Override
    File getRootDir() {
        return new File(parent.rootDir, name)
    }
}
