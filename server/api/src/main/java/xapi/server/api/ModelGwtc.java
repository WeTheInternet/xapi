package xapi.server.api;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileMessage;
import xapi.dev.gwtc.api.GwtcProjectGenerator;
import xapi.dev.gwtc.api.GwtcService;
import xapi.except.NotConfiguredCorrectly;
import xapi.except.NotYetImplemented;
import xapi.fu.In2;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.ServerRecompiler;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.model.api.Model;
import xapi.reflect.X_Reflect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

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

    String getPrecompileLocation();

    ModelGwtc setPrecompileLocation(String location);

    default GwtcService getOrCreateService() {
        return getOrCreate(this::getService, this::createService, this::setService);
    }

    default GwtManifest getOrCreateManifest() {
        return getOrCreateManifest("Gen" + System.identityHashCode(this));
    }
    default GwtManifest getOrCreateManifest(String modName) {
        return getOrCreate(this::getManifest, ()->createManifest(modName), this::setManifest);
    }

    default GwtManifest createManifest(String modName) {
        final GwtManifest manifest = X_Inject.instance(GwtManifest.class);
        final GwtcService service = getService();
        if (service != null) {
            final GwtcProjectGenerator project = service.getProject(modName);
            String root = project.getSuggestedRoot();
            manifest.setRelativeRoot(root);
        }
        return manifest;
    }

    default GwtcService createService() {
        return X_Inject.instance(GwtcService.class);
    }

    default void warmupCompile() {
        final GwtcService gwtc = getOrCreateService();
        final GwtManifest manifest = getOrCreateManifest();
        CompileMessage status = gwtc.getJobManager().getStatus(manifest.getModuleName());
        if (status == null) {
            gwtc.recompile(manifest, null, (comp, err)->{
                if (err != null) {
                    X_Log.error(ModelGwtc.class, "Failure in warmup compile", err, "module:\n", manifest);
                }
            });
        }
    }

    default void getCompiledDirectory(In2<CompiledDirectory, Throwable> callback) {
        final GwtManifest manifest = getOrCreateManifest();
        if (manifest.getCompileDirectory() != null) {
            callback.in(manifest.getCompileDirectory(), null);
            return;
        }
        final GwtcService service = getOrCreateService();
        final CompileMessage status = service.getJobManager().getStatus(manifest.getModuleName());
        if (status == null) {
            // If there is no recompiler event started, we should use the production compile directory
            String loc = getPrecompileLocation();
            if (loc == null) {
                // We're about to do some really ugly guessing in here, which I'm sure will
                // not work in all environments, but will suffice for our flagship use case;
                // a more robust solution can be added later.
                final Class<?> mainClass = X_Reflect.getMainClass();
                String webInf = X_Reflect.getFileLoc(mainClass);
                int ind = webInf.indexOf("WEB-INF");
                if (ind == -1) {
                    // we're basically boned.  Give up
                    throw new NotYetImplemented("Unable to guess gwt compile location from " + webInf+"; " +
                        "please call setPrecompileLocation(\"path/to/gwt/output\") on your ModelGwtc: " + this);
                }
                loc = webInf.substring(0, ind) + manifest.getModuleName();
            }

            // hokay!  we've found where gwt was compiled.  Now, extract our props.xapi file...
            File props = new File(loc, "props.xapi");
            if (!props.isFile()) {
                throw new NotConfiguredCorrectly("In order to guess your gwt module structure, please inherit " +
                    "xapi.X_Gwtc, or manually added MetaLinker to your gwt compile");
            }
            final UiContainerExpr container;
            try (
                FileInputStream in  = new FileInputStream(props);
            ) {
                container = JavaParser.parseXapi(in);
            } catch (IOException e) {
                X_Log.error(ModelGwtc.class, "Failed to read props.xapi file");
                throw new NotConfiguredCorrectly("Failed to read props.xapi file");
            } catch (ParseException e) {
                X_Log.error(ModelGwtc.class, "Malformed props.xapi file; please submit a ticket on xapi github");
                throw new NotConfiguredCorrectly("Malformed props.xapi file; please submit a ticket on xapi github");
            }
            // Alright!  We've got our ui container
            assert container.getName().equals("gwt-props") : "Malformed props.xapi";


            CompiledDirectory dir = new CompiledDirectory();
            container
                .getAttribute("genDir")
                .readIfPresent(attr->dir.setGenDir(attr.getStringExpression(false)));
            container
                .getAttribute("warDir")
                .readIfPresent(attr->dir.setWarDir(attr.getStringExpression(false)));
            container
                .getAttribute("workDir")
                .readIfPresent(attr->dir.setWorkDir(attr.getStringExpression(false)));
            container
                .getAttribute("deployDir")
                .readIfPresent(attr->dir.setDeployDir(attr.getStringExpression(false)));
            container
                .getAttribute("extraDir")
                .readIfPresent(attr->dir.setExtraDir(attr.getStringExpression(false)));
            // TODO: figure out sourcemapdir, port, etc...

            callback.in(dir, null);
        } else {
            X_Log.info(ModelGwtc.class, "Status already known; ", status, " re-running compilation");
            Long millis = manifest.getMaxCompileMillis();
            service.doCompile(manifest, millis == null ? 0 : millis, millis == null ? null : TimeUnit.MILLISECONDS, callback);
        }
    }
}
