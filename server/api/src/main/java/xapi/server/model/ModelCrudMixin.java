package xapi.server.model;

import xapi.fu.In1.In1Unsafe;
import xapi.fu.In1Out1;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.jre.model.ModelServiceJre;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.*;
import xapi.model.service.ModelService;
import xapi.source.api.CharIterator;
import xapi.source.impl.StringCharIterator;
import xapi.util.X_String;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

import java.io.InputStream;
import java.util.concurrent.TimeoutException;

import static xapi.time.X_Time.print;

/**
 * An interface with useful default methods for request-processing APIs to reuse.
 *
 * Used as a shared space for moving from ModelPersistServlet to ModelEndpoint.
 *
 * This is a functional interface, because the only thing you need to supply
 * is a factory from gwt module name to an input stream of ModelModule source
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/28/18 @ 1:02 AM.
 */
@FunctionalInterface
public interface ModelCrudMixin {

    In1Out1<String,InputStream> findManifest(String moduleName);

    default ModelService getService() {
        return X_Model.getService();
    }

    default void performGet(String moduleName, String uri, String type, In2Unsafe<ModelManifest, Model> success, In1Unsafe<Throwable> failure) {

        final String[] keySections = uri.split("/");
        final ModelModule module = ModelModuleLoader.get().loadModule(findManifest(moduleName), moduleName);
        final RemovalHandler handler = ModelServiceJre.registerModule(module);
        try {
            final PrimitiveSerializer primitives = X_Model.getService().primitiveSerializer();
            String namespace = keySections[keySections.length-3];
            namespace = primitives.deserializeString(new StringCharIterator(namespace));
            String kind = keySections[keySections.length-2];
            final CharIterator ident = new StringCharIterator(keySections[keySections.length-1]);
            final ModelManifest manifest = module.getManifest(type);
            final int keyType = primitives.deserializeInt(ident);
            final String id = ident.consumeAll().toString();

            final ModelKey key = X_Model.newKey(namespace, kind, id).setKeyType(keyType);
            final Mutable<Boolean> wait = new Mutable<>();
            final Class<Model> modelType = (Class<Model>) manifest.getModelType();
            X_Model.load(modelType, key, SuccessHandler.handler( m -> {
                try {
                    success.in(manifest, m);
                    wait.set(true);
                } catch (final Throwable t) {
                    X_Log.error(ModelCrudMixin.class, "Error calling user callback", success, "with model", m, t);
                    throw t;
                } finally {
                    wait.setIfNull(Out1.FALSE);
                }

            }, fail -> {
                try {
                    failure.in(fail);
                } finally {
                    wait.setIfNull(Out1.FALSE);
                }
            }));
            final Boolean result = wait.block(handler, millisGet());
            if (result == null) {
                // purposely not using useThenSet, to avoid mutex();
                // we know some other code _should_ be running in the mutex(),
                // so it's guaranteed deadlock if we try to take the lock.
                final Boolean was = wait.out1();
                wait.in(false);
                if (was == null) {
                    failure.in(new TimeoutException("Waited " + print(millisGet()) + " to load " + modelType + ": " + key ));
                }
            }
        } catch (Throwable t){
            failure.in(t);
        } finally {
            handler.remove();
        }
    }

    default double millisGet() {
        return 15_000;
    }

    default void performQuery(final ModelService service, PrimitiveSerializer primitives,
                              final String kind, final CharIterator queryString,
                              In2Unsafe<ModelQuery, ModelQueryResult> success,
                              In1Unsafe<Throwable> failure
    ) {
        final ModelQuery query = ModelQuery.deserialize(service, primitives, queryString);
        final Mutable<Boolean> wait = new Mutable<>();
        final SuccessHandler callback = SuccessHandler.handler( (ModelQueryResult t) -> {
                try {
                    success.in(query, t);
                    wait.set(true);
                } catch (final Exception e) {
                    X_Log.error(ModelCrudMixin.class, "Query completed successfully", query, "\n:\n", t,
                        "but user's callback produced error ", e);
                } finally {
                    wait.setIfNull(Out1.FALSE);
                }
            }, fail -> {
                X_Log.error(ModelCrudMixin.class, "Error saving model", fail);
                failure.in(fail);
                wait.setIfNull(Out1.FALSE);
            }
        );

        if ("".equals(kind)) {
            service.query(query, callback);
        } else {
            final Class<? extends Model> modelClass = service.typeToClass(kind);
            service.query(modelClass, query, callback);
        }

        if (wait.block(35_000) == null) {
            failure.in(new TimeoutException("Waited 35s to service " + query));
        }
    }

    default void performPost(String moduleName, String type,
                             Out1<String> loader,
                             In2Unsafe<ModelManifest, Model> success,
                             In1Unsafe<Throwable> failure) {

        final String asString = loader.out1();
        final ModelModule module = ModelModuleLoader.get().loadModule(findManifest(moduleName), moduleName);
        final RemovalHandler handler = ModelServiceJre.registerModule(module);
        try {
            ModelManifest meta = module.getManifest(type);
            if (meta == null) {
                // egregious workaround... TODO: reconcile the model type uppercase-lowercase issue (likely on client, in generated code).
                meta = module.getManifest(X_String.firstCharToLowercase(type));
            }
            final ModelManifest manifest = meta;
            final Model model;
            try {
                model = X_Model.deserialize(meta, asString);
            } catch (final Throwable e) {
                String moduleText, manifestText;
                try {
                    moduleText = ModelModule.serialize(module);
                } catch (final Throwable e1) {
                    moduleText = String.valueOf(module);
                }
                try {
                    manifestText = ModelManifest.serialize(meta);
                } catch (final Throwable e1) {
                    manifestText = String.valueOf(meta);
                }
                X_Log.error(ModelCrudMixin.class, "Error trying to deserialize model; ",e,"source: ","|"+asString+"|"
                    , "\nManifest: ","|"+manifestText+"|"
                    , "\nModule: ","|"+moduleText+"|");
                failure.in(e);
                return;
            }
            final Mutable<Boolean> wait = new Mutable<>();

            X_Model.persist(model, SuccessHandler.handler( m -> {
                        try {
                            success.in(manifest, m);
                            wait.set(true);
                        } catch (final Exception e) {
                            X_Log.error(ModelCrudMixin.class, "Error saving model", e);
                        } finally {
                            wait.setIfNull(Out1.FALSE);
                        }
                    }, fail -> {
                        X_Log.error(ModelCrudMixin.class, "Error saving model", fail);
                        failure.in(fail);
                        wait.setIfNull(Out1.FALSE);
                }));

            final Boolean result = wait.block(12_500);
            if (result == null) {
                failure.in(new TimeoutException("Waited 12.5s to save model " + asString));
            }
        } finally {
            handler.remove();
        }
    }
}
