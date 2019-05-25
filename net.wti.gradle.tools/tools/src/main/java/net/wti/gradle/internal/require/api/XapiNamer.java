package net.wti.gradle.internal.require.api;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 14/05/19 @ 4:18 AM.
 */
public class XapiNamer {

    public static String moduleName(String base, String plat, String mod) {
        if ("main".equals(mod)) {
            if ("main".equals(plat)) {
                return base;
            }
            return base + "-" + plat;
        } else {
            if ("main".equals(plat)) {
                return base + "-" + mod;
            }
            return base + "-" + plat + "-" + mod;

        }
    }
}
