package xapi.gradle.config;

import org.gradle.api.Project;
import xapi.gradle.api.Freezable;

/**
 * This configuration bean is exposed in the gradle dsl via `xapi { publish { ... } }`.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/5/18 @ 2:14 AM.
 */
public class PublishConfig implements Freezable {

    /**
     * Set to true to prevent the artifacts in this module from being published at all.
     */
    private boolean hidden;

    /**
     * Whether or not to publish sources.
     */
    private boolean sources;

    /**
     * see {@link PublishConfig#isTests}
     */
    private boolean tests;

    private boolean frozen;

    /**
     * If set to true, tests will be published as their own `test-` prefixed archive name.
     *
     * Test prefixing happens after project prefixing and before impl name prefixing;
     * suppose a project named `inject` in build name 'xapi':
     * xapi-inject: main artifact, suitable for compile-time dependency of other main artifacts.
     *              adds compile-time transitive dependency on xapi-inject-api
     *              adds run-time transitive dependency on xapi-inject-spi
     *              group:xapi-inject:version
     *              src/main/java|resources
     * xapi-inject-api: api artifact, defines minimum api necessary to use injection tools.
     *              group:xapi-inject-api:version
     *              src/api/java|resources
     * xapi-inject-spi: spi artifact, defines minimum api necessary to implement injection tools.
     *              group:xapi-inject-spi:version
     *              src/spi/java|resources
     * xapi-inject-gwt: gwt impl artifacts; depends on xapi-inject, xapi-inject-api.
     *              group:xapi-inject-gwt:version
     *              src/gwt/java|resources
     *
     * when publishTests is true, we will create additional artifacts (* for only-if-exists semantics):
     * xapi-inject-test: main test artifact.  Exports transitive dependency graph suitable for post-publishing use.
     *                   adds compile-time transitive dependency on xapi-inject, and xapi-inject-test-api*
     *                   adds run-time transitive dependency on xapi-inject-test-spi*
     *                   group:xapi-inject-test:version
     *                   src/test/java|resources -> Code to be transitively exported
     * xapi-inject-test-main: tests to run for the main artifact.  Not exported.  Depends on xapi-inject-test.
     *
     */
    public boolean isTests() {
        return tests;
    }

    /**
     * see {@link PublishConfig#isTests}
     */
    public void setTests(boolean tests) {
        freezeCheck(frozen);
        this.tests = tests;
    }

    /**
     * @return true if this project should not be published.
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @param hidden true to prevent publishing, false to allow it (for all main, api, etc. modules)
     */
    public void setHidden(boolean hidden) {
        freezeCheck(frozen);
        this.hidden = hidden;
    }

    @Override
    public void freeze(Project from, Object... context) {
        this.frozen = true;
    }

    public boolean isSources() {
        return sources;
    }

    public void setSources(boolean sources) {
        freezeCheck(frozen);
        this.sources = sources;
    }

    @Override
    public String toString() {
        return "PublishConfig{" +
            "hidden=" + hidden +
            ", sources=" + sources +
            ", tests=" + tests +
            ", frozen=" + frozen +
            ", frozenAfter=XapiExtension.onPrepare" +
            '}';
    }
}
