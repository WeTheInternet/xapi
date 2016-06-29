package xapi.javac.dev.model;

import xapi.file.X_File;
import xapi.fu.Rethrowable;
import xapi.log.X_Log;
import xapi.util.X_Debug;
import xapi.util.X_Namespace;
import xapi.util.X_String;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
public class CompilerSettings implements Rethrowable {

    private boolean verbose;
    private String outputDirectory;
    private boolean test;

    private String root;
    private String sourceDirectory;
    private String generateDirectory;
    private String processorPath;
    private List<String> plugins;
    private Set<String> classpath;
    private boolean generateDebugging;
    private boolean useRuntimeClasspath;
    private PreferMode preferMode;
    private ImplicitMode implicitMode;
    private ProcessorMode processorMode;
    private boolean clearGenerateDirectory;
    private boolean addedRuntimePath;

    public CompilerSettings() {
        generateDebugging = true;
    }

    public String[] toArguments(String... files) {
        List<String> args = new ArrayList<>();
        if (isVerbose()) {
            args.add("-verbose");
        }
        String output = getOutputDirectory();
        args.add("-d");
        args.add(output);

        String sources = getSourceDirectory();
        args.add("-sourcepath");
        int ind = sources.indexOf("src/main/java");
        if (ind != -1) {
            sources += File.pathSeparator + sources.replace("src/main/java", "src/main/resources");
            sources += File.pathSeparator + sources.replace("src/main/java", "target/generated-sources/annotations");
        } else {
            ind = sources.indexOf("src/test/java");
            if (ind != -1) {
                sources += File.pathSeparator + sources.replace("src/test/java", "src/test/resources");
                sources += File.pathSeparator + sources.replace("src/test/java", "target/generated-test-sources/test");
            }
        }
            args.add(sources);

        String genDir = getGenerateDirectory();
        args.add("-s");
        args.add(genDir);

        if (isClearGenerateDirectory()) {
            resetGenerateDirectory();
        }

        final ProcessorMode proc = getProcessorMode();
        final boolean addProcessor;
        switch (proc) {
            case CompileOnly:
                args.add("-proc:none");
                addProcessor = false;
                break;
            case Both:
                addProcessor = true;
                break;
            case ProcessOnly:
                args.add("-proc:only");
                addProcessor = true;
                break;
            default:
                throw new AssertionError("Can't get here");
        }
        if (addProcessor) {
            String processorPath = getProcessorPath();
            args.add("-processorpath");
            args.add(processorPath);
        }

        final Set<String> path = getClasspath();
        if (path != null && !path.isEmpty()) {
            args.add("-cp");
            args.add(X_String.join(File.pathSeparator, path));
        }

        getPlugins().forEach(plugin -> {
            if (!plugin.startsWith("-Xplugin:")) {
                plugin = "-Xplugin:" + plugin;
            }
            args.add(plugin);
        });
        if (isGenerateDebugging()) {
            args.add("-g");
        }
        PreferMode prefer = getPreferMode();
        args.add("-Xprefer:" + prefer.name().toLowerCase());

        ImplicitMode implicit = getImplicitMode();
        args.add("-implicit:" + implicit.name().toLowerCase());

        try {

            for (String file : files) {
                File asFile = new File(file);
                if (asFile.exists()) {
                    args.add(asFile.getCanonicalPath());
                } else {
                    asFile = new File(getSourceDirectory(), file);
                    if (asFile.exists()) {
                        args.add(asFile.getCanonicalPath());
                    } else {
                        asFile = new File(getGenerateDirectory(), file);
                        if (asFile.exists()) {
                            args.add(asFile.getCanonicalPath());
                        } else {
                            asFile = new File(getOutputDirectory(), file);
                            if (asFile.exists()) {
                                args.add(asFile.getCanonicalPath());
                            } else {
                                throw new IllegalArgumentException("Cannot find file " + file + " in any classpath directories");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw rethrow(e);
        }

        X_Log.info(getClass(), "Javac args:\n",
              "javac " + X_String.join(" ", args));
        return args.toArray(new String[args.size()]);
    }

    public CompilerSettings resetGenerateDirectory() {
        final String genDir = getGenerateDirectory();
        File genFile = new File(genDir);
        if (!genFile.exists()) {
            if (!genFile.mkdirs()) {
                X_Log.error(getClass(), "Cannot create generate directory ", genFile, "expect subsequent failures");
            }
        }
        X_File.deepDelete(genDir);
        boolean result = genFile.mkdirs();
        assert result : "Unable to create generate directory " + genFile;
        return this;
    }

    public PreferMode getPreferMode() {
        if (preferMode == null) {
            preferMode = PreferMode.NEWER;
        }
        return preferMode;
    }

    public CompilerSettings setPreferMode(PreferMode preferMode) {
        this.preferMode = preferMode;
        return this;
    }

    public ImplicitMode getImplicitMode() {
        if (implicitMode == null) {
            implicitMode = ImplicitMode.NONE;
        }
        return implicitMode;
    }

    public CompilerSettings setImplicitMode(ImplicitMode implicitMode) {
        this.implicitMode = implicitMode;
        return this;
    }

    public boolean isClearGenerateDirectory() {
        return clearGenerateDirectory;
    }

    public CompilerSettings setClearGenerateDirectory(boolean clearGenerateDirectory) {
        this.clearGenerateDirectory = clearGenerateDirectory;
        return this;
    }

    public enum PreferMode {
        NEWER, SOURCE
    }

    public enum ImplicitMode {
        CLASS, NONE
    }

    public enum ProcessorMode {
        CompileOnly, ProcessOnly, Both
    }

    public String getRoot() {
        if (root == null) {
            try {
                root = new File(".").getCanonicalPath();
            } catch (IOException e) {
                throw rethrow(e);
            }
        }
        return root;
    }

    public CompilerSettings setRoot(String root) {
        this.root = root;
        return this;
    }

    @Override
    public RuntimeException rethrow(Throwable e) {
        X_Debug.debug(e);
        return Rethrowable.super.rethrow(e);
    }

    public boolean isVerbose() {
        return verbose;
    }

    public CompilerSettings setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public String getOutputDirectory() {
        if (outputDirectory == null) {
            final File classesDir = new File(getRoot(), "target/" + (test ? "test-" : "") + "classes");
            try {
                if (!classesDir.exists()) {
                    classesDir.mkdirs();
                }
                outputDirectory = classesDir.getCanonicalPath();
            } catch (IOException e) {
                throw rethrow(e);
            }
        }
        return outputDirectory;
    }

    public CompilerSettings setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    public boolean isTest() {
        return test;
    }

    public CompilerSettings setTest(boolean test) {
        this.test = test;
        if (test && generateDirectory == null) {
            try {
                final File f = new File(getRoot(), "target/generated-test-sources/test");
                if (!f.exists()) {
                    f.mkdirs();
                }
                return setGenerateDirectory(f.getCanonicalPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public String getSourceDirectory() {
        if (sourceDirectory == null) {
            final File resourcesDir = new File(getRoot(), "src/" + (test ? "test" : "main") + "/resources");
            try {
                sourceDirectory = resourcesDir.getCanonicalPath();
                if (!resourcesDir.exists()) {
                    resourcesDir.mkdirs();
                }
            } catch (IOException e) {
                throw rethrow(e);
            }
        }
        String genDir = getGenerateDirectory();
        if (!sourceDirectory.contains(genDir)) {
            return X_String.join(File.pathSeparator, sourceDirectory, genDir);
        }
        return sourceDirectory;
    }

    public CompilerSettings setSourceDirectory(String sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
        return this;
    }

    public String getGenerateDirectory() {
        if (generateDirectory == null) {
            try {
                File f = new File(getRoot(), "target/generated-" + (test ? "test-" : "") + "sources/gwt");
                generateDirectory = f.getCanonicalPath();
                if (!f.exists()) {
                    f.mkdirs();
                }
            } catch (IOException e) {
                throw rethrow(e);
            }
        }
        return generateDirectory;
    }

    public CompilerSettings setGenerateDirectory(String generateDirectory) {
        this.generateDirectory = generateDirectory;
        return this;
    }

    public String getProcessorPath() {
        if (processorPath == null) {
            try {
                final File f = new File(root, "target/xapi-dev-javac-" + X_Namespace.XAPI_VERSION + ".jar");
                processorPath = f.getCanonicalPath();
            } catch (IOException e) {
                throw rethrow(e);
            }
        }
        return processorPath;
    }

    public CompilerSettings setProcessorPath(String processorPath) {
        this.processorPath = processorPath;
        return this;
    }

    public List<String> getPlugins() {
        if (plugins == null) {
            plugins = new ArrayList<>();
            plugins.add("MagicMethodPlugin");
            plugins.add("GwtCreatePlugin");
            plugins.add("XapiCompilerPlugin");
        }
        return plugins;
    }

    public void setPlugins(List<String> plugins) {
        this.plugins = plugins;
    }

    public Set<String> getClasspath() {
        if (classpath == null) {
            classpath = new LinkedHashSet<>();
        }
        if (isUseRuntimeClasspath() && !addedRuntimePath) {
            addedRuntimePath = true;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            while (cl != null) {
                if (cl instanceof URLClassLoader) {
                    for (URL url : ((URLClassLoader) cl).getURLs()) {
                        classpath.add(url.toExternalForm().replace("file:", ""));
                    }
                }
                cl = cl.getParent();
            }
        }
        return classpath;
    }

    public CompilerSettings setClasspath(Set<String> classpath) {
        this.classpath = classpath;
        return this;
    }

    public CompilerSettings addToClasspath(String loc) {
        this.classpath.add(loc);
        return this;
    }

    public boolean isGenerateDebugging() {
        return generateDebugging;
    }

    public CompilerSettings setGenerateDebugging(boolean generateDebugging) {
        this.generateDebugging = generateDebugging;
        return this;
    }

    public ProcessorMode getProcessorMode() {
        if (processorMode == null) {
            processorMode = ProcessorMode.Both;
        }
        return processorMode;
    }

    public CompilerSettings setProcessorMode(ProcessorMode processorMode) {
        this.processorMode = processorMode;
        return this;
    }

    public boolean isUseRuntimeClasspath() {
        return useRuntimeClasspath;
    }

    public CompilerSettings setUseRuntimeClasspath(boolean useRuntimeClasspath) {
        this.useRuntimeClasspath = useRuntimeClasspath;
        return this;
    }
}
