package xapi.server.model;

import xapi.except.FatalException;
import xapi.except.InvalidRequest;
import xapi.fu.*;
import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.gwtc.api.CompiledDirectory;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.ModelNotFoundException;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.service.ModelService;
import xapi.scope.api.Scope;
import xapi.scope.request.RequestScope;
import xapi.scope.spi.RequestLike;
import xapi.scope.spi.ResponseLike;
import xapi.server.api.CrudEndpoint;
import xapi.server.api.ModelGwtc;
import xapi.server.api.WebApp;
import xapi.server.api.XapiServer;
import xapi.source.api.CharIterator;
import xapi.source.impl.StringCharIterator;
import xapi.util.X_Properties;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/28/18 @ 12:08 AM.
 */
public class ModelEndpoint <Req extends RequestScope<?, ?>> implements CrudEndpoint<Req>, ModelCrudMixin {

    private WebApp app;

    @Override
    public void initialize(Scope scope, XapiServer<Req> server) {
        final String modules = X_Properties.getProperty("gwt.modules");
        app = server.getWebApp();
        if (modules != null) {
            for (String module : modules.split("\\s+")) {
                module = module.trim();
                if (!module.isEmpty()) {
                    final String moduleName = module;
                    try {
                        final ModelGwtc mod = app.getOrCreateGwtModules().get(moduleName);
                        mod.getCompiledDirectory((dir, err) -> {
                            if (err == null) {
                                ModelModuleLoader.get().preloadModule(asFinder(dir), moduleName);
                            } else {
                                X_Log.warn(ModelEndpoint.class, "Error preloading", moduleName, err);
                            }
                        });
                    } catch (Throwable t) {
                        X_Log.warn(ModelEndpoint.class, "Unable to load module", moduleName, t);
                    }
                }
            }
        }
    }

    private static In1Out1Unsafe<String,InputStream> asFinder(CompiledDirectory dir) {
        return moduleName -> Files.newInputStream(Paths.get(dir.getDeployDir(), moduleName, "XapiModelLinker", "xapi.rpc"));
    }


    @Override
    public void doDelete(String path, Req req, String payload, In2<Req, Throwable> callback) {
        callback.in(req, new UnsupportedOperationException("DELETE not supported"));
    }

    @Override
    public void doGet(String path, Req scope, String payload, In2<Req, Throwable> callback) {
        final RequestLike req = scope.getRequest();
        final ResponseLike resp = scope.getResponse();
        final ModelService service = getService();
        final String moduleName = req.getHeader("X-Gwt-Module", throwMissing("X-Gwt-Module"));
        final String uri = path;
        final String[] keySections = uri.split("/");
        final String requestType = keySections[keySections.length-4];
        final Mutable<Boolean> succeeded = new Mutable<>();
        if ("query".equals(requestType)) {
            String kind = keySections[keySections.length-2];
            final PrimitiveSerializer primitives = service.primitiveSerializer();
            kind = primitives.deserializeString(new StringCharIterator(kind));
            final CharIterator ident = new StringCharIterator(keySections[keySections.length-1]);

            performQuery(service, primitives, kind, ident, (query, result)->{
                final String serialized = result.serialize(service, primitives);
                resp.buildRawResponse().append(serialized);
                succeeded.useThenSet(was->{
                    if (was == null) {
                        // first one in notifies caller
                        callback.in(scope, null);
                    } else {
                        X_Log.warn(ModelEndpoint.class, "Model load succeeded,\n", result.getModelList(),
                            "\nbut request already " + (was ? "completed once (check for incorrect recursion)" : "timed out / failed (check logs)"));
                    }
                }, true);
            }, failure->{
                X_Log.error(ModelEndpoint.class, "Failed to query", uri, failure);
                succeeded.useThenSet(was->{
                    if (was == null) {
                        // first one, call back
                        callback.in(scope, new FatalException("Unable to query " + uri, failure));
                    } else {
                        X_Log.warn(ModelEndpoint.class, "Model load failed for", uri, "\n", failure,
                            "\nbut request already ", was ? "succeeded (whoever called here needs better state management)" : "timed out / failed (check logs)");
                    }
                }, false);
            });

            return;
        }
        final String type = req.getHeader("X-Model-Type", throwMissing("X-Model-Type"));

        performGet(moduleName, uri, type, (manifest, model) -> {
            final String serialized = X_Model.serialize(manifest, model);
            resp.buildRawResponse().append(serialized);
            succeeded.useThenSet(was->{
                if (was == null) {
                    callback.in(scope, null);
                } else {
                    X_Log.warn(ModelEndpoint.class, "Model load succeed for ", model,
                        "but callback was already invoked", was?"":" with failure");
                }
            }, true);
        }, failure -> {
            if (failure instanceof ModelNotFoundException) {
                X_Log.trace(ModelEndpoint.class, "Failed to read", uri, failure);
            } else {
                X_Log.error(ModelEndpoint.class, "Failed to read", uri, failure);
            }

            if (failure instanceof TimeoutException && succeeded.isNull()) {
                if (!scope.isReleased()) {
                    callback.in(scope, failure);
                }
            } else {
                succeeded.useThenSet(was->{
                    if (was == null) {
                        callback.in(scope, failure);
                    } else {
                        X_Log.warn(ModelEndpoint.class, "Model load failed for", uri,
                            "failed, but request already", was ? "succeeded" : "failed");
                    }
                }, false);
            }
        });
    }

    @Override
    public void doPost(String path, Req scope, String payload, In2<Req, Throwable> callback) {
        final RequestLike req = scope.getRequest();
        Out1<String> loader = Lazy.deferred1(req::getBody);

        final String type = req.getHeader("X-Model-Type", throwMissing("X-Model-Type"));
        final String moduleName = req.getHeader("X-Gwt-Module", throwMissing("X-Gwt-Module"));

        ResponseLike resp = scope.getResponse();
        performPost(moduleName, type, loader, (manifest, model)->{
            final String serialized = X_Model.serialize(manifest, model);
            resp.buildRawResponse().append(serialized);
            callback.in(scope, null);
        }, failure -> {
            X_Log.error(ModelEndpoint.class, "Failed to save", loader.out1(), failure);
            callback.in(scope, new FatalException("Unable to save " + loader.out1(), failure));
        });

    }

    private Out1<String> throwMissing(String missing) {
        return Out1.out1Unsafe(()-> {
            throw new InvalidRequest("Missing Header: " + missing);
        });
    }

    @Override
    public void doPut(String path, Req req, String payload, In2<Req, Throwable> callback) {
        callback.in(req, new UnsupportedOperationException("PUT not supported"));
    }

    @Override
    public In1Out1<String, InputStream> findManifest(String moduleName) {
        final ModelGwtc mod = app.getOrCreateGwtModules().get(moduleName);
        final Mutable<In1Out1<String, InputStream>> wait = new Mutable<>();
        mod.getCompiledDirectory((dir, err) ->{
            if (err == null) {
                wait.in(asFinder(dir));
            } else {
                wait.in(In1Out1.alwaysThrow(Out1.immutable(err)));
            }
        });
        final In1Out1<String, InputStream> result = wait.block(60_000);
        if (result == null) {
            throw Rethrowable.firstRethrowable(this, mod)
                .rethrow(new TimeoutException("Waited 60s for manifest " + moduleName + " to load"));
        }
        return wait.out1();
    }
}
