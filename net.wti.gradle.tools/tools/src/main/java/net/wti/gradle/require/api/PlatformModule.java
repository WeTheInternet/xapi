package net.wti.gradle.require.api;

import net.wti.gradle.system.tools.GradleCoerce;
import xapi.gradle.fu.LazyString;

import static net.wti.gradle.system.tools.GradleCoerce.isEmptyString;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-10 @ 4:38 a.m..
 */
public class PlatformModule implements CharSequence {

    public static final PlatformModule UNKNOWN = new PlatformModule(null, null);

    public static final LazyString defaultPlatform = LazyString.nonNullString(
            ()->System.getenv("XAPI_DEFAULT_PLATFORM"),
            ()->System.getProperty("xapi.default.platform", "main")
    );
    public static final LazyString defaultModule = LazyString.nonNullString(
            ()->System.getenv("XAPI_DEFAULT_MODULE"),
            ()->System.getProperty("xapi.default.module", "main")
    );

    private final String platform, module;

    public PlatformModule(String platform, String module) {
        this.platform = platform;
        this.module = module;
    }

    public static String unparse(PlatformModule platMod) {
        return unparse(platMod.getPlatform(), platMod.getModule());
    }
    public static String unparse(CharSequence platform, CharSequence module) {
        String plat = platform.toString();
        String mod = module.toString();
        if ("main".equals(plat)) {
            return mod;
        }
        if ("main".equals(mod)) {
            return plat;
        }
        return plat + Character.toUpperCase(mod.charAt(0)) +
                (mod.length() > 1 ? mod.substring(1) : "");
    }

    public static PlatformModule parse(CharSequence platModStr) {
        final String[] bits = platModStr.toString().split(":");
        if (bits.length > 2) {
            throw new IllegalArgumentException("A platform:module pair cannot have more than one : \nyou sent " + platModStr);
        }
        return new PlatformModule(bits.length < 2 ? "main" : bits[0], bits.length == 2 ? bits[1] : bits[0]);
    }

    public String getPlatform() {
        return platform;
    }

    public String getModule() {
        return module;
    }

    public PlatformModule edit(String platform, String module) {
        return new PlatformModule(
            platform == null ? this.platform : platform,
            module == null ? this.module : module
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PlatformModule))
            return false;

        final PlatformModule that = (PlatformModule) o;

        if (getPlatform() != null ? !getPlatform().equals(that.getPlatform()) : that.getPlatform() != null)
            return false;
        return getModule() != null ? getModule().equals(that.getModule()) : that.getModule() == null;
    }

    @Override
    public int hashCode() {
        int result = getPlatform() != null ? getPlatform().hashCode() : 0;
        result = 31 * result + (getModule() != null ? getModule().hashCode() : 0);
        return result;
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    @Override
    public String toString() {
        return platform == null ? module == null ? defaultModule.toString() : module : platform + ':' + module;
    }
    public String toStringStrict() {
        return toStringStrict(null, null);
    }
    public String toStringStrict(CharSequence defaultPlatform, CharSequence defaultModule) {
        return isEmptyString(platform) ? isEmptyString(module) ?
                fixPlat(defaultPlatform) + ":" + fixMod(defaultModule) :
                fixPlat(defaultPlatform) + ":" + module :
                isEmptyString(module) ?
                platform + ':' + fixMod(defaultModule) :
                platform + ':' + module;
    }

    private static CharSequence fixPlat(CharSequence supplied) {
        return supplied == null || supplied.length() == 0 ? defaultPlatform : supplied;
    }

    private static CharSequence fixMod(CharSequence supplied) {
        return supplied == null || supplied.length() == 0 ? defaultModule : supplied;
    }
}
