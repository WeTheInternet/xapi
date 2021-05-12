package net.wti.gradle.schema.view;

import net.wti.gradle.require.api.PlatformModule;
import org.gradle.api.Named;
import xapi.fu.In1;
import xapi.fu.itr.SizedIterable;

/**
 * IndexViewInitial:
 * <p><p>
 *      The IndexViewInitial is responsible for knowing about the project : platform : module map very early
 *      in the parsing / schema processing lifecycle. From the moment we start reading a schema.xapi file,
 *      the IndexViewInitial will be how we add new schema.xapi, manipulate schema in AST form, and collect
 *      callbacks, to execute later, when "the schema is almost done baking".
 * <p><p>
 *     If you get greedy and call {@link #resolve()} to access an IndexViewMutable(), the next stage of parsing.
 * <p><p>
 *     Once you resolve the initial index (you don't have to do this, it is done naturally, as late as possible),
 *     it will freeze further attempts to use the various mutable methods in the index view.
 * <p><p>
 * Created by James X. Nelson (James@WeTheInter.net) on 08/05/2021 @ 4:02 a.m..
 */
public interface IndexViewInitial extends ViewChain<IndexViewMutable> {

    IndexViewMutable resolve();

    interface IndexProject extends Named {

        @Override // explicit, from Named
        String getName();

        String getPath();

        IndexProject getParent();

        IndexProject forModulesAll(In1<PlatformModule> callback);
        IndexProject forModulesLive(In1<PlatformModule> callback);
        IndexProject forModulesPrunable(In1<PlatformModule> callback);

        IndexViewInitial doneProject();
    }

    IndexViewInitial addSchemaXapi(String filePath);

    IndexViewInitial forEachProject(In1<IndexProject> callback);
    default IndexViewInitial forNamedProject(CharSequence key, In1<IndexProject> callback) {
        return forNamedProject(false, key, callback);
    }
    IndexViewInitial forNamedProject(boolean strict, CharSequence key, In1<IndexProject> callback);

    IndexProject getOrCreateProject(CharSequence key);

}