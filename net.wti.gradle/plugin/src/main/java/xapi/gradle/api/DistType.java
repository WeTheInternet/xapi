package xapi.gradle.api;

import xapi.fu.In1Out1;
import xapi.fu.Maybe;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;

import static xapi.fu.X_Fu.toLowerCase;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 1:41 AM.
 */
public enum DistType implements ArchiveType {
    VERTX(DefaultArchiveType.API, DefaultArchiveType.MAIN, PlatformType.GWT, PlatformType.JSZIP),
    GWT_DEV(DefaultArchiveType.API, DefaultArchiveType.MAIN, PlatformType.GWT, DefaultArchiveType.SOURCE),
    JAVAFX {
        @Override
        public String[] getFileTypes() {
            return new String[]{".class", ".fxml", ".css"};
        }
    },
    GRADLE {
        @Override
        public String[] getFileTypes() {
            return new String[]{".class", ".groovy", ".kts", ".gradle", ".properties"};
        }
    },
    MAVEN {
        @Override
        public String[] getFileTypes() {
            return new String[] {".class", ".xml", ".properties"};
        }
    }
    ;
    private final ArchiveType[] types;


    static final MapLike<String, DistType> all = X_Jdk.mapOrderedInsertion();

    static {
        final In1Out1<DistType, String> name = DistType::name;
        final In1Out1<DistType, String> sourceName = DistType::sourceName;
        final DistType[] values = values();
        all.putFromValues(name, values);
        all.putFromValues(sourceName, values);
        all.putFromValues(name.mapOut(toLowerCase()), values);
        all.putFromValues(sourceName.mapOut(toLowerCase()), values);
    }

    DistType(){
        types = new ArchiveType[]{DefaultArchiveType.MAIN, DefaultArchiveType.API};
    }
    DistType(ArchiveType ... types) {
        this.types = types;
    }

    @Override
    public ArchiveType[] getTypes() {
        return types;
    }

    public static Maybe<DistType> find(String key) {
        return all.getMaybe(key);
    }
}
