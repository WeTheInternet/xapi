package xapi.server.api;

import xapi.dev.gwtc.api.GwtcService;
import xapi.fu.In1;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.IsRecompiler;
import xapi.gwtc.api.ServerRecompiler;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.model.api.Model;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/4/16.
 */
public interface ModelGwtc extends Model {

    GwtManifest getManifest();

    ModelGwtc setManifest(GwtManifest manifest);

    GwtcService getService();

    ModelGwtc setService(GwtcService service);

    ServerRecompiler getRecompiler();

    ModelGwtc setRecompiler(ServerRecompiler recompiler);

    default GwtcService getOrCreateService() {
        return getOrCreate(this::getService, this::createService, this::setService);
    }

    default GwtManifest getOrCreateManifest() {
        return getOrCreate(this::getManifest, this::createManifest, this::setManifest);
    }

    default GwtManifest createManifest() {
        final GwtManifest manifest = X_Inject.instance(GwtManifest.class);
        final GwtcService service = getService();
        if (service != null) {
            String root = service.getSuggestedRoot();
            manifest.setRelativeRoot(root);
        }
        return manifest;
    }

    default GwtcService createService() {
        return X_Inject.instance(GwtcService.class);
    }

    default void warmupCompile() {
        if (getRecompiler() == null) {
            ChainBuilder<In1<IsRecompiler>> queued = Chain.startChain();
            setRecompiler(queued::add);
            final GwtcService gwtc = getOrCreateService();
            final GwtManifest manifest = getOrCreateManifest();
            gwtc.recompile(manifest, (comp, err)->{
                if (err == null) {
                    setRecompiler(comp);
                    queued.forAll(comp::useServer);
                    queued.clear();
                } else {
                    X_Log.error(getClass(), "Failure in warmup compile", err, "module:\n", manifest);
                }
            });
        }
    }
}
