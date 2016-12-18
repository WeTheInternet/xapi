package xapi.server.api;

import xapi.dev.gwtc.api.GwtcService;
import xapi.fu.In1;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.IsRecompiler;
import xapi.inject.X_Inject;
import xapi.model.api.Model;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/4/16.
 */
public interface ModelGwtc extends Model {

    GwtManifest getManifest();

    ModelGwtc setManifest(GwtManifest manifest);

    GwtcService getService();

    ModelGwtc setService(GwtcService service);

    In1<In1<IsRecompiler>> getRecompiler();

    ModelGwtc setRecompiler(In1<In1<IsRecompiler>> recompiler);

    default GwtcService getOrCreateService() {
        return getOrCreate(this::getService, this::createService, this::setService);
    }

    default GwtManifest getOrCreateManifest() {
        return getOrCreate(this::getManifest, this::createManifest, this::setManifest);
    }

    default GwtManifest createManifest() {
        return X_Inject.instance(GwtManifest.class);
    }

    default GwtcService createService() {
        return X_Inject.instance(GwtcService.class);
    }
}
