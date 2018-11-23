package xapi.gradle.api;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 1:41 AM.
 */
public enum DistType implements ArchiveType {
    VERTX(DefaultArchiveTypes.API, DefaultArchiveTypes.MAIN, DefaultArchiveTypes.GWT, DefaultArchiveTypes.JSZIP),
    GWT_DEV(DefaultArchiveTypes.API, DefaultArchiveTypes.MAIN, DefaultArchiveTypes.GWT, DefaultArchiveTypes.SOURCE),
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

    DistType(){
        types = new ArchiveType[]{DefaultArchiveTypes.MAIN, DefaultArchiveTypes.API};
    }
    DistType(ArchiveType ... types) {
        this.types = types;
    }

    @Override
    public ArchiveType[] getTypes() {
        return types;
    }
}
