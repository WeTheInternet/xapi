package xapi.gradle.api;

import xapi.fu.In1Out1;
import xapi.fu.Maybe;
import xapi.fu.data.MapLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;
import xapi.fu.java.X_Jdk;

import static xapi.fu.X_Fu.toLowerCase;
import static xapi.gradle.api.DefaultArchiveType.*;

/**
 * A platform type corresponds to a finalized, native-aware implementation module.
 *
 * It is related to gradle variants / flavors;
 * each platform type will be responsible for implementing the spi layer of your application.
 *
 * In order to use an api that comes with an spi,
 * you should have a platform type for that spi.
 *
 * Rather than trying to magically swap the IDE around like Android,
 * we will instead wire up sourcesets for the platform types
 * to have the correct, altered classpath for your "native classworld".
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 1:38 AM.
 */
public enum PlatformType implements ArchiveType {
    DEV {
        @Override
        public boolean isImpl() {
            return true;
        }

        @Override
        public ArchiveType[] getTypes() {
            return new ArchiveType[]{JRE};
        }
    },
    JRE {
        @Override
        public boolean isImpl() {
            return true;
        }

        @Override
        public ArchiveType[] getTypes() {
            return new ArchiveType[]{MAIN};
        }
    },

    // TODO: consider moving J2CL and GWT types into their own enums...
    /**
     * A GWT jar type is sources, classes and resources;
     * for a monolithic compiler like gwt, fatter archives are preferred over more archives.
     * (see com.google.gwt.dev.resource.impl.ResourceAccumulatorManager and friends for details)
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
            return new ArchiveType[]{API, SPI, SOURCE, SPI_SOURCE};
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
        public ArchiveType sourceFor() {
            return J2CL;
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
    private String[] fileTypes;

    static final MapLike<String, ArchiveType> all = X_Jdk.mapOrderedInsertion();

    static {
        final In1Out1<ArchiveType, String> name = ArchiveType::name;
        final In1Out1<ArchiveType, String> sourceName = ArchiveType::sourceName;

        final PlatformType[] values = values();
        all.putFromValues(name, values);
        all.putFromValues(sourceName, values);
        all.putFromValues(name.mapOut(toLowerCase()), values);
        all.putFromValues(sourceName.mapOut(toLowerCase()), values);
    }

    PlatformType() {
        this(".class");
    }

    PlatformType(String... additionalFiles) {
        this.fileTypes = additionalFiles;
    }

    public static MappedIterable<ArchiveType> all() {
        return all.mappedValues().unique(X_Jdk.setHashIdentity());
    }

    @Override
    public String[] getFileTypes() {
        return fileTypes;
    }

    public void setFileTypes(String... types) {
        this.fileTypes = types;
    }

    public static Maybe<ArchiveType> find(String key) {
        return all.getMaybe(key);
    }

    public static ArchiveType register(String key, ArchiveType type) {
        final ArchiveType was = all.put(key, type);
        assert was == null || was == type : "Cannot override type " + key +"; was: " + was + ", set: " + type;
        return type;
    }
}
