package xapi.gradle.api;

import groovy.lang.Closure;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import xapi.fu.Out1;
import xapi.fu.data.SetLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.java.X_Jdk;
import xapi.string.X_String;

import java.util.concurrent.Callable;

import static xapi.fu.itr.ArrayIterable.iterate;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 1:15 AM.
 */
public interface ArchiveType {

    // The only non-default method, and it's implemented for you when creating enums.
    String name();

    default String sourceName() {
        return name().toLowerCase();
    }

    default String[] getFileTypes() {
        return new String[0];
    }
    default Class<? extends Task>[] getTaskTypes() {
        return new Class[0];
    }
    default String getExtension() {
        return "jar";
    }
    default boolean isClasses() {
        return true;
    }
    default boolean isSources() {
        return false;
    }
    default ArchiveType sourceFor() {
        return null;
    }
    default boolean isDocs() {
        return false;
    }

    /**
     * @return true for the "main artifact of this module".
     *
     * This archive should include, if present, any api or spi module,
     * (where api maps to gradle api configuration, and spi maps to implementation)
     * as well as any "common glue" (shared abstractions that should be transitive dependencies).
     *
     * This can allow you to move heavier dependencies to your spi layer (each impl will depend on spi directly),
     * to protect downstream api consumers from heavy classpaths and superfluous rebuilds.
     *
     * The main module will be published without classifiers, and will contain all default transitive dependencies.
     * The main module will also have a compileOnly view of any stub or staging modules,
     * so you can reference types that "will exist in the future" (i.e. each mutually-exclusive impl creates com.foo.MyImpl).
     *
     * You can think of the main module as supplying "core" or "common" code.
     * Has compile-time transitivity on api, and run-time transitivity on spi.
     *
     * For very simple modules, there may only be a main module.
     *
     */
    default boolean isMain() {
        return false;
    }

    /**
     * @return
     */
    default boolean isTest() {
        return false;
    }

    /**
     * @return true for artifacts that define the set of interfaces / types
     * that consuming code would compile against to use this code.
     *
     * This should contain the minimum possible set of classes needed to define a service,
     * with minimum transitive dependencies.
     *
     * You should feel free to add code to api modules, but be afraid to take any away.
     */
    default boolean isApi() {
        return false;
    }

    /**
     * @return true for artifacts the define the set of interfaces / types
     * that implementing code would need to fulfill in order to be fulfill a given API.
     *
     * That is, where an api artifact defines how a module can be used,
     * and a main artifact artifact defines common tools to implement that api,
     * the spi (service provider interface) defines the api surface of "native magic"
     * that is needed in common code to fulfill exposed requests.
     *
     * Both main (common) artifacts and implementation artifacts should reference the spi.
     * The main artifact has common glue to "use an spi to fulfill an api",
     * while the impl artifact just references the spi and native code to "do stuff";
     * it may or may not even reference the exposed api (i.e. the spi is for i18n messages, db code, etc)
     */
    default boolean isSpi() {
        return false;
    }
    default boolean isStub() {
        return false;
    }
    default boolean isImpl() {
        return isStub();
    }

    default boolean isIncludeAll() {
        final String[] types = getFileTypes();
        return types.length == 0 ||
            ( types.length == 1 && "*".equals(types[0]) );
    }

    default ArchiveType[] getTypes() {
        return new ArchiveType[0];
    }

    default MappedIterable<ArchiveType> allTypes() {
        SetLike<ArchiveType> all = X_Jdk.setLinked();
        for (ArchiveType type : getTypes()) {
            if (all.addIfMissing(type)) {
                all.addNow(type.allTypes());
            }
        }
        return all;
    }

    static ArchiveType coerceArchiveType(Object o) {
        if (o instanceof ArchiveType) {
            return (ArchiveType) o;
        }
        if (o instanceof CharSequence) {
            String s = o.toString();
            final ArchiveType type = PlatformType.find(s)
                .ifAbsent(DefaultArchiveType::find, s)
                .ifAbsent(DistType::find, s)
                .getOrThrow(() ->
                    new InvalidUserDataException("Unknown archive type " + s)
                );
            return type;
        }
        if (o instanceof Out1) {
            final Object val = ((Out1) o).out1();
            return coerceArchiveType(val);
        }
        if (o instanceof Provider) {
            final Object val = ((Provider) o).get();
            return coerceArchiveType(val);
        }
        if (o instanceof Closure) {
            final Object val = ((Closure) o).call();
            return coerceArchiveType(val);
        }
        if (o instanceof Callable) {
            final Object val;
            try {
                val = ((Callable<Object>) o).call();
            } catch (Exception e) {
                e.printStackTrace();
                throw new GradleException("Unexpected exception calling " + o, e);
            }
            return coerceArchiveType(val);
        }

        throw new InvalidUserDataException("Unsupported archive type " + o);
    }

    default boolean includes(ArchiveType type) {
        return type == this || iterate(getTypes()).containsReference(type);
    }

    default String prefixedName(String prefix) {
        return toTypeName(prefix, sourceName());
    }

    static String toTypeName(String prefix, String type) {
        if (X_String.isEmpty(prefix)) {
            return type;
        }
        return prefix + X_String.toTitleCase(type);
    }

}
