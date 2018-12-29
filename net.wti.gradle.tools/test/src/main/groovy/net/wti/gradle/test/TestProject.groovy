package net.wti.gradle.test


import org.gradle.api.Named
import org.gradle.api.NonNullApi

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/26/18 @ 2:16 AM.
 */
@NonNullApi
class TestProject implements Named, TestProjectDir {
    private final String name
    private final File rootDir
    private boolean included
    private String projectName

    TestProject(String name, File rootDir) {
        this.name = name
        this.rootDir = rootDir
    }

    @Override
    String getName() {
        return name
    }

    String getPath() {
        return ":" == name ? ":" : ":$name"
    }

    @Override
    File getRootDir() {
        return rootDir
    }

    boolean isIncluded() {
        return this.@included
    }

    void setIncluded(boolean included) {
        this.@included = included
    }

    String getProjectName() {
        if (this.@projectName) {
            return this.@projectName
        }
        setProjectName(name == ':' ? getRootDir().name : name)
        return this.@projectName
    }

    void setProjectName(String projectName) {
        if (this.@projectName) {
            if (this.@projectName != projectName) {
                throw new IllegalStateException("Cannot change project name from ${this.@projectName} to $projectName")
            }
        } else {
            this.@projectName = projectName
        }
    }
}
