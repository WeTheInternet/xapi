package net.wti.gradle.settings.api;


/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-10 @ 3:22 a.m..
 */
public enum DependencyType implements CharSequence {
    /**
     * A dependency on a different project w/in the same gradle build
     */
    project,
    /**
     * A dependency on a different module withing a single (meta)project
     *
     * metaproject, meaning, within variants / modules / platforms of a given project.
     */
    internal,

    /**
     * A dependency on an external artifact (g:n:v).  A platform:module may also be specified, in which case:
     *
     * If the g:n:v matches an active, included build, we will create an appropriate gradle Dependency (correct project / target configuration)
     *
     * If the g:n:v does not match an included module, we will transform the platform:module into the supplied g:n:v.
     *
     * In (almost) all cases, you should supply the "raw" g:n:v (no .platform or -module segments), and a "platform:module" argument:
     *
     * Example:
     * external "com.foo:mod:1.0", "jre:api"
     *
     * if "com.foo:mod" matches any parsed schema.xapi:
     *      if module is in same gradle build, dependency is:
     *      project(path: 'mod-jre', configuration: 'exportedApi')
     *
     *      if module is in included gradle build: module("com.foo:mod:1.0"), dependency is:
     *      somethingImplementation("com.foo:mod:1.0") {
     *          capabilities {
     *              requireCapability("com.foo.jre:mod-api")
     *          }
     *      }
     * if not found in schema.xapi, decide whether to use capabilities or a mangled g:n:v (like g.platform:n-module:v),
     * based on system / gradle properties:
     * com.foo_use_capability=true
     * com.foo_use_capability_mod=false
     *
     * Above, all of com.foo would use capabilities, except module `mod`, which will use a mangled g:n:v.
     *
     */
    external,
    // foreign, TODO: an external g:n:v that we reconstituted into a project-level dependency of an included gradle build.
    /**
     * Value not specified, use the default for whatever scope we are declared in (probably project() ).
     */
    unknown,
    ;

    @Override
    public int length() {
        return name().length();
    }

    @Override
    public char charAt(int index) {
        return name().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return name().subSequence(start, end);
    }

}

