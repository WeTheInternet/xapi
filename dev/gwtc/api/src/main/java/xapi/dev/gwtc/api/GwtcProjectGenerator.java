package xapi.dev.gwtc.api;

import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.source.SourceBuilder.JavaType;
import xapi.dev.source.XmlBuffer;
import xapi.fu.Out1;
import xapi.gwtc.api.GwtManifest;

import java.io.File;
import java.lang.reflect.Method;

import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.tools.GWTTestSuite;

/**
 * A collection of tools around building up gwt.xml and other artifacts necessary
 * for a Gwt compilation.
 *
 * Designed for annotating classes and methods with configuration that we will
 * generate on-demand when we want to run a Gwt compilation.
 *
 * The {@link GwtcGeneratedProject} is an older iteration of this, but it
 * got really messy, so we are going to treat it as a mere container for
 * information, with all the brains pulled out of it into this class
 * (as well as any brains that live in {@link xapi.dev.gwtc.api.GwtcService})
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/15/17.
 */
public interface GwtcProjectGenerator {

    void addClass(Class<?> clazz);
    void addMethod(Method method);
    void addMethod(Method method, boolean onNewInstance);
    void addPackage(Package pkg, boolean recursive);
    void addGwtTestCase(Class<? extends GWTTestCase> subclass);
    void addGwtTestSuite(Class<? extends GWTTestSuite> asSubclass);

    File getTempDir();

    default File getSourceDir() {
        return getTempDir();
    }

    boolean addJUnitClass(Class<?> clazz);
    void addAsyncBlock(Class<? extends RunAsyncCallback> asSubclass);

    void generateAll(File source, String moduleName, XmlBuffer head, XmlBuffer body);

    void addGwtModules(Class<?> clazz);
    void addClasspath(Class<?> cls);
    MethodBuffer addMethodToEntryPoint(String methodDef);
    void addGwtInherit(String inherit);
    String getModuleName();
    SourceBuilder createJavaFile(String pkg, String filename, JavaType type);

    void copyModuleTo(String module, GwtManifest manifest);

    void createFile(String pkg, String filename, Out1<String> sourceProvider);
    String modifyPackage(String pkgToUse);
    MethodBuffer getOnModuleLoad();

    String generateCompile(GwtManifest manifest);

    String getSuggestedRoot();

    GwtManifest getManifest();
}
