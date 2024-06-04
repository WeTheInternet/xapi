package net.wti.gradle.settings.api;

import net.wti.gradle.tools.HasAllProjects;
import org.gradle.api.Named;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.fu.api.DoNotOverride;

import java.io.Serializable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-06-06 @ 6:01 a.m..
 */
public interface SchemaCallbacks extends Serializable {

    void perModule(In1<SchemaModule> callback);

    @DoNotOverride("unless you also override perModule and all other forModule methods")
    default void forModule(In1Out1<CharSequence, Boolean> key, In1<SchemaModule> callback) {
        perModule(callback.filteredMapped(Named::getName, key).ignoreOutput());
    }

    @DoNotOverride("unless you also override perModule and all other forModule methods")
    default void forModule(CharSequence key, In1<SchemaModule> callback) {
        forModule(key::equals, callback);
    }

    void perPlatform(In1<SchemaPlatform> callback);

    default void forPlatform(In1Out1<CharSequence, Boolean> key, In1<SchemaPlatform> callback) {
        perPlatform(callback.filteredMapped(Named::getName, key).ignoreOutput());
    }

    void perPlatformAndModule(In2<SchemaPlatform, SchemaModule> callback);

    void perProject(In1<SchemaProject> callback);
    @DoNotOverride("unless you also override perProject and all other forProject methods")
    default void forProject(In1Out1<CharSequence, Boolean> filter, In1<SchemaProject> callback) {
        perProject(callback.filteredMapped(HasPath::getPath, filter).ignoreOutput());
    }

    @DoNotOverride("unless you also override perProject and all other forProject methods")
    default void forProject(CharSequence key, In1<SchemaProject> callback) {
        String me = key.toString();
        if (me.startsWith(":")) {
            me = me.substring(1);
        }
        me = me.replace(':', '/');
        final In1Out1<CharSequence, Boolean> filter = me::equals;
        forProject(filter, callback);
    }

    void flushCallbacks(HasAllProjects map);

    //    // can't really do this until we figure out how to share data safely across classloaders / possibly processes.
//    void perMap(In1<SchemaMap> callback);
//    void forMap(CharSequence key, In1<SchemaMap> callback);

}
