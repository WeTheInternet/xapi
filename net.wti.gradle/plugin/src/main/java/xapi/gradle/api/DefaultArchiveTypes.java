package xapi.gradle.api;

import org.gradle.api.Task;
import org.gradle.api.tasks.bundling.Jar;

/**
 * The standard archive types currently supported (excluding {@link DistType},
 * which gets it's own enum).
 *
 * This Api is a work in progress, but the idea is loosely as follows:
 * in order for archives to have transitive dependencies in maven,
 * they must have a top-level artifactId and a pom of their own.
 *
 * However, it's a giant pain to manage piles of poms,
 * and dependencies thereof, without resorting to "just dump everything on classpath".
 *
 * Each archive type loosely corresponds to a SourceSet (or a fraction thereof).
 * Each gradle project will emit one to many jars, for each type,
 * with correct and sane poms setup for transitive inheritance.
 *
 * Within such gradle projects, a plain dependence on another module
 * will auto-wire platform-specific / archive-type-specific wiring as well.
 *
 * For example, GWT inherits transitive source dependencies by default.
 *
 * Note that some jars, like source jars, _might_ be double-published;
 * they will be attached as classifiers to their main counterpart,
 * as well as published with a `-sources` artifactId suffix.
 * This would be moreso for IDE support than anything,
 * and should only be done if a need becomes apparent.
 *
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 1:38 AM.
 */
public enum DefaultArchiveTypes implements ArchiveType {
    API(".class", ".xapi") {
        @Override
        public boolean isApi() {
            return true;
        }
    },
    MAIN {
        @Override
        public Class<? extends Task>[] getTaskTypes() {
            return new Class[]{Jar.class};
        }

        @Override
        public ArchiveType[] getTypes() {
            return new ArchiveType[]{API, SPI};
        }

    },
    SOURCE(".java") {
        @Override
        public boolean isSources() {
            return true;
        }

        @Override
        public boolean isClasses() {
            return false;
        }

        @Override
        public ArchiveType[] getTypes() {
            return new ArchiveType[]{API_SOURCE};
        }
    },
    SPI(".class") {
        @Override
        public boolean isSpi() {
            return true;
        }
    },
    JAVADOC {
        @Override
        public boolean isDocs() {
            return true;
        }

        @Override
        public boolean isClasses() {
            return false;
        }
    },
    TEST {
        @Override
        public boolean isTest() {
            return true;
        }

        @Override
        public ArchiveType[] getTypes() {
            return new ArchiveType[]{MAIN};
        }
    },
    STUB(".class") {
        @Override
        public boolean isStub() {
            return true;
        }

        @Override
        public ArchiveType[] getTypes() {
            return new ArchiveType[]{API, SPI};
        }
    },
    /**
     * A default archive is the result of running a "local dist build"
     * on your
     */
    DEFAULT(".*") {
        @Override
        public boolean isStub() {
            return true;
        }

        @Override
        public boolean isImpl() {
            return true;
        }

        @Override
        public ArchiveType[] getTypes() {
            return new ArchiveType[]{API, SPI, MAIN, STUB};
        }
    },


    TEST_SOURCE(".java") {
        @Override
        public boolean isTest() {
            return true;
        }

        @Override
        public boolean isSources() {
            return true;
        }

        @Override
        public boolean isClasses() {
            return false;
        }

        @Override
        public ArchiveType[] getTypes() {
            return new ArchiveType[]{SOURCE};
        }
    },
    TEST_JAVADOC(".html") {
        @Override
        public boolean isTest() {
            return true;
        }

        @Override
        public boolean isDocs() {
            return true;
        }

        @Override
        public boolean isClasses() {
            return false;
        }
    },
    DEFAULT_SOURCE(".java", ".xapi") {
        @Override
        public boolean isImpl() {
            return true;
        }

        @Override
        public boolean isStub() {
            return true;
        }

        @Override
        public boolean isSources() {
            return true;
        }

        @Override
        public boolean isClasses() {
            return false;
        }

        @Override
        public ArchiveType[] getTypes() {
            return new ArchiveType[]{API_SOURCE, SPI_SOURCE, SOURCE, STUB};
        }
    },
    API_SOURCE(".java", ".xapi") {
        @Override
        public boolean isApi() {
            return true;
        }

        @Override
        public boolean isSources() {
            return true;
        }

        @Override
        public boolean isClasses() {
            return false;
        }
    },
    SPI_SOURCE(".java", ".xapi") {
        @Override
        public boolean isSpi() {
            return true;
        }

        @Override
        public boolean isSources() {
            return true;
        }

        @Override
        public boolean isClasses() {
            return false;
        }
    },

    /**
     * A GWT jar type is sources, classes and resources...
     */
    GWT(".class", ".java", ".gwt.xml", ".js", ".css", ".html", ".png", ".gif", ".jpg", ".svg", ".ttf") {
        @Override
        public boolean isSources() {
            return true;
        }

        @Override
        public boolean isImpl() {
            return true;
        }

        @Override
        public ArchiveType[] getTypes() {
            return new ArchiveType[]{SOURCE};
        }
    },
    /**
     * A jszip, containing output produced by j2cl compiler.
     */
    JSZIP(".js") {
        @Override
        public boolean isImpl() {
            return true;
        }
    },
    /**
     * A zip of js externs, suitable for use by closure compiler.
     */
    EXTERNS(".js") {
        @Override
        public boolean isImpl() {
            return true;
        }
    },
    /**
     * A J2cl jar is classes and .native.js files, suitable for use during j2cl transpilation
     */
    J2CL(".class", ".native.js") {
        @Override
        public boolean isImpl() {
            return true;
        }
    },
    /**
     * J2cl source jars contain the results of pre-processed java->j2cl sources,
     * which are then fed into the javac to produce a j2cl jar that then
     */
    J2CL_SOURCE {
        @Override
        public boolean isClasses() {
            return false;
        }

        @Override
        public boolean isSources() {
            return true;
        }

        @Override
        public boolean isImpl() {
            return true;
        }

        @Override
        public ArchiveType[] getTypes() {
            // ONLY take the Api source.  We don't want main sources, we want _transformed_ main sources.
            return new ArchiveType[]{API_SOURCE};
        }
    },
    /**
     * A distribution archive tailored for running on vert.x.
     *
     * We should make a special DistType instead,
     * which adds the concept of containing other ArchiveTypes.
     *
     *
     */
    VERTX {
        @Override
        public boolean isImpl() {
            return true;
        }

        @Override
        public ArchiveType[] getTypes() {
            return new ArchiveType[]{MAIN};
        }
    };
    private static final String[] EMPTY = {};
    private String[] fileTypes;

    DefaultArchiveTypes() {
        this(".class");
    }

    DefaultArchiveTypes(String... additionalFiles) {
        this.fileTypes = additionalFiles;
    }

    @Override
    public String[] getFileTypes() {
        return fileTypes;
    }

    public void setFileTypes(String... types) {
        this.fileTypes = types;
    }
}
