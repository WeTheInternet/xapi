package xapi.dev.gwtc.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import xapi.dev.gwtc.api.GwtcJob;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.dev.gwtc.api.GwtcProjectGenerator;
import xapi.dev.gwtc.api.GwtcService;
import xapi.fu.Lazy;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.test.gwtc.cases.CaseEntryPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

import com.google.gwt.core.ext.TreeLogger.Type;

/**
 * We want to test that the various permutations of Gwt compilations will work correctly.
 *
 * This will run both the compiler+recompiler both using:
 * local process + shared classloader,
 * local process + isolated classloader,
 * remote process
 *
 * All configurations must be able to handle compilation of a simple entry point,
 * as well as responding to changes with a recompile of said entry point.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/20/17.
 */
@RunWith(Parameterized.class)
public class GwtcCompilationTest {

    private static final String MODULE_NAME = "xapi.test.client.Module";

    @Parameter
    public TestOptions options;

    Throwable uncaught;

    @Before
    public void before() {
        uncaught = null;
        Thread.setDefaultUncaughtExceptionHandler((thread, caught)->{
            uncaught = caught;
            X_Log.error(GwtcCompilationTest.class, "Uncaught exception in", thread, caught);
        });
    }

    Lazy<GwtcService> service = Lazy.deferSupplier(X_Inject::instance, GwtcService.class);
    Lazy<CompiledDirectory> compilerResult = Lazy.deferred1Unsafe(this::doCompile);
    Lazy<GwtcProjectGenerator> project = Lazy.deferred1(()->{
        GwtcService gwtc = service.out1();
        return gwtc.getProject(MODULE_NAME);
    });


    private CompiledDirectory doCompile() throws Throwable {
        CompiledDirectory[] result = {null};
        Throwable[] failure = {null};

        final GwtcService gwtc = service.out1();
        final GwtcProjectGenerator project = this.project.out1();
        final GwtManifest manifest = project.getManifest();
        configureProject(project);
        gwtc.doCompile(manifest, 80, TimeUnit.MINUTES, (success, fail)->{
            result[0] = success;
            failure[0] = fail;
        });
        if (failure[0] != null) {
            throw failure[0];
        }
        return result[0];
    }

    protected void configureProject(GwtcProjectGenerator project) {
        final GwtManifest manifest = project.getManifest();
        project.addClass(CaseEntryPoint.class);
        manifest.setUseCurrentJvm(options.localProcess);
//        manifest.setDebugPort(7788);
//        manifest.addDependency(X_Reflect.getFileLoc(GwtcServiceImpl.class));
        if (options.localProcess) {
            manifest.setIsolateClassLoader(!options.sharedClassloader);
        }
        manifest.setLogLevel(Type.INFO);
        manifest.setLogFile(options.localProcess ? GwtcJobMonitor.NO_LOG_FILE : GwtcJobMonitor.STD_OUT_TO_STD_ERR);
    }

    public static class TestOptions {

        private final boolean localProcess;
        private final boolean sharedClassloader;

        public TestOptions(boolean localProcess, boolean sharedClassloader) {
            this.localProcess = localProcess;
            this.sharedClassloader = sharedClassloader;
        }

        @Override
        public String toString() {
            return "GwtcCompilationTest (" +
                (localProcess ? "local " +
                    (sharedClassloader ? " shared classloader" : "isolated classloader")
                    : "remote") + ")";
        }
    }

    @Parameters(name = "{0}")
    public static Collection<TestOptions> permutations() {
        final Collection<TestOptions> list = new ArrayList<>();
        list.add(new TestOptions(true, true));
        list.add(new TestOptions(true, false));
        // classloader config meaningless for remote process
        list.add(new TestOptions(false, false));
        return list;
    }

    private GwtcJob getJob() {
        return service.out1().getJobManager().getJob(
            project.out1().getModuleName()
        );
    }

    @Test
    public void testCompilation() {
        X_Log.info(GwtcCompilationTest.class, "\n\n\nRunning compilation ", options, "\n\n");
        final CompiledDirectory result = compilerResult.out1();
        assertNotNull("", result);
        // Now... to handle recompilation :-)

        // for now, just tell any running job to shutdown...
        X_Log.info(GwtcCompilationTest.class, "Shutting down job after successful completion. Current state" + getJob().getState());
        getJob().destroy();
        project.out1().getManifest().setOnline(false);
    }

}
