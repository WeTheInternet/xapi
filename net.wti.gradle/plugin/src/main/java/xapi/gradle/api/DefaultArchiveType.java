package xapi.gradle.api;

import org.gradle.api.Task;
import org.gradle.api.tasks.bundling.Jar;
import xapi.fu.In1Out1;
import xapi.fu.Maybe;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;

import static xapi.fu.X_Fu.toLowerCase;

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
public enum DefaultArchiveType implements ArchiveType {
    API(".class", ".xapi") {
        @Override
        public boolean isApi() {
            return true;
        }
    },
    SPI(".class", ".xapi") {
        @Override
        public boolean isSpi() {
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
     * on any given module; it uses STUB implementations, if any,
     * and should likely be used only for local/automated testing.
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
    }
    ;
    private String[] fileTypes;

    static final MapLike<String, DefaultArchiveType> all = X_Jdk.mapOrderedInsertion();

    static {
        final In1Out1<DefaultArchiveType, String> name = DefaultArchiveType::name;
        final In1Out1<DefaultArchiveType, String> sourceName = DefaultArchiveType::sourceName;

        final DefaultArchiveType[] values = values();
        all.putFromValues(name, values);
        all.putFromValues(sourceName, values);
        all.putFromValues(name.mapOut(toLowerCase()), values);
        all.putFromValues(sourceName.mapOut(toLowerCase()), values);
    }

    DefaultArchiveType() {
        this(".class");
    }

    DefaultArchiveType(String... additionalFiles) {
        this.fileTypes = additionalFiles;
    }

    @Override
    public String[] getFileTypes() {
        return fileTypes;
    }

    public void setFileTypes(String... types) {
        this.fileTypes = types;
    }

    public static Maybe<DefaultArchiveType> find(String key) {
        return all.getMaybe(key);
    }
}
