package xapi.source.read;

public class SourceUtil {

    private SourceUtil() {}

    public static String toSourceName(String simpleName) {
        return simpleName.replace('$', '.');
    }

    public static String toFlatName(String simpleName) {
        return simpleName.replace('$', '_').replace('.', '_');
    }

    public static String toSetterName(String methodFragment) {
        if (methodFragment.startsWith("set")) {
            return methodFragment;
        }
        return "set"
            + (
            Character.toUpperCase(methodFragment.charAt(0)) +
                (methodFragment.length() == 1 ? "" : methodFragment.substring(1))
        );
    }

    public static String toGetterName(String clsName, String methodFragment) {
        if (methodFragment.startsWith("get")) {
            return methodFragment;
        }
        return
            (clsName.equalsIgnoreCase("boolean") ? "is" : "get")
                + (
                Character.toUpperCase(methodFragment.charAt(0)) +
                    (methodFragment.length() == 1 ? "" : methodFragment.substring(1))
            );
    }
}
