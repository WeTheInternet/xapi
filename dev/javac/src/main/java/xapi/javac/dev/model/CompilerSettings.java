package xapi.javac.dev.model;

import xapi.file.X_File;
import xapi.fu.Rethrowable;
import xapi.log.X_Log;
import xapi.util.X_Debug;
import xapi.util.X_Namespace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private boolean generateDebugging = true;
    private PreferMode preferMode;
    private ImplicitMode implicitMode;
    private boolean clearGenerateDirectory;

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
        } else {
            ind = sources.indexOf("src/test/java");
            if (ind != -1) {
                sources += File.pathSeparator + sources.replace("src/test/java", "src/test/resources");
            }
        }
            args.add(sources);

        String genDir = getGenerateDirectory();
        args.add("-s");
        args.add(genDir);

        if (isClearGenerateDirectory()) {
            resetGenerateDirectory();
        }

        String processorPath = getProcessorPath();
        args.add("-processorpath");
        args.add(processorPath);

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

        X_Log.info(getClass(), "Javac args:", args);
        return args.toArray(new String[args.size()]);
    }

    public void resetGenerateDirectory() {
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

    enum PreferMode {
        NEWER, SOURCE
    }

    enum ImplicitMode {
        CLASS, NONE
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

    public boolean isGenerateDebugging() {
        return generateDebugging;
    }

    public CompilerSettings setGenerateDebugging(boolean generateDebugging) {
        this.generateDebugging = generateDebugging;
        return this;
    }
}
