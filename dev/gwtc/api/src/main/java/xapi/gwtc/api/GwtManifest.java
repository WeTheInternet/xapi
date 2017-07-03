package xapi.gwtc.api;

import jdk.nashorn.internal.objects.NativeString;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.collect.impl.SimpleFifo;
import xapi.fu.Filter;
import xapi.fu.Immutable;
import xapi.fu.In2;
import xapi.fu.MappedIterable;
import xapi.fu.Out1;
import xapi.fu.iterate.SingletonIterator;
import xapi.log.X_Log;
import xapi.source.X_Source;

import java.lang.reflect.Array;
import java.util.Iterator;

import static xapi.collect.X_Collect.*;
import static xapi.fu.Immutable.immutable1;
import static xapi.fu.In2.in2;
import static xapi.fu.iterate.SingletonIterator.singleItem;
import static xapi.gwtc.api.GwtManifest.CleanupMode.DELETE_ON_SUCCESSFUL_EXIT;

import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.codeserver.CompileStrategy;
import com.google.gwt.dev.util.arg.SourceLevel;

public class GwtManifest {

  static {
    // register this with magic method generator, so gwt knows how
    // to create runtime arrays of Out1's
    Array.newInstance(Out1.class, 0);
  }

  public enum CleanupMode {
    ALWAYS_DELETE, DELETE_ON_SUCCESS, NEVER_DELETE, DELETE_ON_EXIT, DELETE_ON_SUCCESSFUL_EXIT
  }

  public enum MethodNameMode {
      NONE,
      ONLY_METHOD_NAME,
      ABBREVIATED,
      FULL
  }

  private final class ClasspathIterable implements Iterable<String> {
    private final class Itr implements Iterator<String> {
      Iterator<String> src = getSources().forEach().iterator();
      Iterator<String> dep = getDependencies().iterator();
      @Override
      public synchronized boolean hasNext() {
        if (dep == null) {
          return src.hasNext();
        }
        if (!src.hasNext()) {
          src = dep;
          dep = null;
        }
        return src.hasNext();
      }

      @Override
      public String next() {
        hasNext();
        return src.next();
      }

      @Override
      public void remove() {}
    }
    @Override
    public Iterator<String> iterator() {
      return new Itr();
    }
  }

  public static final String GEN_PREFIX = "__gen";

  private static final String ARG_AGGRESSIVE_OPTIMIZE = "XdisableAggressiveOptimization";
  private static final String ARG_AUTO_OPEN = "autoOpen";
  private static final String ARG_CAST_CHECKING = "XdisableCastChecking";
  private static final String ARG_CAST_METADATA = "XdisableClassMetadata";
  private static final String ARG_DEPENDENCIES = "dependencies";
  private static final String ARG_DEPLOY_DIR = "deployDir";
  private static final String ARG_DISABLE_THREADED = "disableThreadedWorkers";
  private static final String ARG_DISABLE_UNIT_CACHE = "gwt.persistentunitcache";
  private static final String ARG_DRAFT_COMPILE = "draftCompile";
  private static final String ARG_ENABLE_ASSERT = "ea";
  private static final String ARG_ENABLE_CLOSURE = "XenableClosureCompiler";
  private static final String ARG_EXTRA_ARGS = "extraArgs";
  private static final String ARG_EXTRAS_DIR = "extra";
  private static final String ARG_FRAGMENTS = "XfragmentCount";
  private static final String ARG_GEN_DIR = "gen";
  private static final String ARG_GWT_VERSION = "gwtVersion";
  private static final String ARG_JVM_ARGS = "jvmArgs";
  private static final String ARG_LOCAL_WORKERS = "localWorkers";
  private static final String ARG_LOG_LEVEL = "logLevel";
  private static final String ARG_SOURCE_LEVEL = "sourceLevel";
  private static final String ARG_OBFUSCATION_LEVEL = "style";
  private static final String ARG_OPEN_ACTION = "openAction";
  private static final String ARG_OPTIMIZATION_LEVEL = "optimize";
  private static final String ARG_PORT = "port";
  private static final String ARG_RUN_ASYNC_ENABLED = "XdisableRunAsync";
  private static final String ARG_SOURCE_DIRS = "src";
  private static final String ARG_SOYC = "soyc";
  private static final String ARG_SOYC_DETAILED = "XsoycDetailed";
  private static final String ARG_STRICT = "strict";
  private static final String ARG_SYS_PROPS = "sysProps";
  private static final String ARG_UNIT_CACHE_DIR = "gwt.persistentunitcachedir";
  private static final String ARG_URL_TO_OPEN = "url";
  private static final String ARG_VALIDATE_ONLY = "validate";
  private static final String ARG_WAR_DIR = "war";
  private static final String ARG_WORK_DIR = "workDir";
  private static final String ARG_INCREMENTAL = "incremental";
  private static final String NEW_ARG = " -";
  private static final String NEW_LINE = "\n ";
  private static final String NEW_ITEM = NEW_LINE + "- ";
  private static final String NEW_LIST_ITEM = NEW_LINE + " - ";

  private static final int DEFAULT_FRAGMENTS = 10;
  private static final int DEFAULT_PORT = 13370;
  private static final int DEFAULT_LOCAL_WORKERS = 6;
  private static final int DEFAULT_OPTIMIZATION = 9;
  private static final Type DEFAULT_LOG_LEVEL = Type.INFO;
  private static final ObfuscationLevel DEFAULT_OBFUSCATION = ObfuscationLevel.OBFUSCATED;
  private static final OpenAction DEFAULT_OPEN_ACTION = OpenAction.IFRAME;
  private static final SourceLevel DEFAULT_SOURCE_LEVEL = SourceLevel.JAVA8;

  private boolean autoOpen;
  private boolean closureCompiler;
  private IntTo<Out1<? extends Iterable<String>>> dependencies = newSet(Out1.class);
  private String deployDir;
  private boolean disableAggressiveOptimize;
  private boolean disableCastCheck;
  private boolean disableClassMetadata;
  private boolean disableRunAsync;
  private boolean disableThreadedWorkers;
  private boolean disableUnitCache;
  private boolean draftCompile;
  private boolean enableAssertions;
  private IntTo<String> extraArgs = newList(String.class);
  private String extrasDir;
  private int fragments = DEFAULT_FRAGMENTS;
  private String genDir;
  private String gwtVersion = "";
  private IntTo<String> jvmArgs =  newList(String.class);
  private int localWorkers = DEFAULT_LOCAL_WORKERS;
  private Type logLevel = DEFAULT_LOG_LEVEL;
  private SourceLevel sourceLevel = DEFAULT_SOURCE_LEVEL;
  private String moduleName;
  private ObfuscationLevel obfuscationLevel = DEFAULT_OBFUSCATION;
  private OpenAction openAction = DEFAULT_OPEN_ACTION;
  private int optimizationLevel = DEFAULT_OPTIMIZATION;
  private int port = DEFAULT_PORT;
  private IntTo<String> sources = newList(String.class);
  private boolean soyc;
  private boolean soycDetailed;
  private boolean strict;
  private IntTo<String> systemProperties = newList(String.class);
  private String unitCacheDir;
  private String urlToOpen;
  private boolean validateOnly;
  private String workDir;
  private String warDir;
  private boolean includeGenDir;
  private CleanupMode cleanupMode = DELETE_ON_SUCCESSFUL_EXIT;
  private StringTo<GwtcXmlBuilder> gwtBuilders = newStringMapInsertionOrdered(GwtcXmlBuilder.class);
  private boolean useCurrentJvm = false;
  private boolean recompile;
  private CompiledDirectory compileDirectory;
  private boolean online;
  private MethodNameMode mode;
  private boolean incremental;
  private String relativeRoot;

  public GwtManifest() {
    includeGenDir = true;
  }

  public GwtManifest(String moduleName) {
    this();
    this.moduleName = moduleName;
  }

  public GwtManifest addDependency(String dep) {
    dependencies.push(immutable1(singleItem(dep)));
    return this;
  }

  public GwtManifest addDependency(Out1<String> dep) {
    dependencies.push(dep.map(SingletonIterator::singleItem));
    return this;
  }

  public GwtManifest addExtraArg(String extraArg) {
    assert !extraArgs.contains(extraArg) : "Extra args already contains "+extraArg;
    extraArgs.push(extraArg);
    return this;
  }
  public GwtManifest addJvmArg(String jvmArg) {
    assert !jvmArgs.contains(jvmArg) : "Jvm args already contains "+jvmArg;
    jvmArgs.push(jvmArg);
    return this;
  }

  public GwtManifest addSource(String src) {
    assert !sources.contains(src) : "Sources already contains "+src;
    sources.push(src);
    return this;
  }
  public GwtManifest addSystemProp(String sysProp) {
    assert !systemProperties.contains(sysProp) : "System Properties already contains "+sysProp;
    systemProperties.push(sysProp);
    return this;
  }
  public GwtManifest clearDependencies() {
    dependencies.clear();
    return this;
  }
  public GwtManifest clearExtraArgs() {
    extraArgs.clear();
    return this;
  }
  public GwtManifest clearJvmArgs() {
    jvmArgs.clear();
    return this;
  }

  public GwtManifest clearSources() {
    sources.clear();
    return this;
  }
  public GwtManifest clearSystemProps() {
    return this;
  }
  public MappedIterable<String> getDependencies() {
//    IntTo<String> unique = newSet(String.class);
    return dependencies.forEachItem()
        .flatten(Out1::out1)
        .filter(Filter.ifNotNull())
//        .filter(unique::add)
        ;
  }

  public String getDeployDir() {
    return deployDir;
  }

  public IntTo<String> getExtraArgs() {
    return extraArgs;
  }

  public String getExtrasDir() {
    return extrasDir;
  }

  public int getFragments() {
    return fragments;
  }

  public String getGenDir() {
    return normalizeGenDir(genDir);
  }

  protected String normalizeWorkDir(String workDir) {
    return workDir;
  }

  protected String normalizeGenDir(String genDir) {
    return genDir;
  }

  public String getGwtVersion() {
    return gwtVersion == null ? "" : gwtVersion;
  }

  public IntTo<String> getJvmArgs() {
    return jvmArgs;
  }

  public int getLocalWorkers() {
    return localWorkers;
  }

  public Type getLogLevel() {
    return logLevel;
  }

  public SourceLevel getSourceLevel() {
    return sourceLevel;
  }

  public String getModuleName() {
    return moduleName;
  }

  public ObfuscationLevel getObfuscationLevel() {
    return obfuscationLevel;
  }

  public OpenAction getOpenAction() {
    return openAction;
  }

  public int getOptimizationLevel() {
    return optimizationLevel;
  }

  public int getPort() {
    return port;
  }

  public IntTo<String> getSources() {
    return sources;
  }

  public IntTo<String> getSystemProperties() {
    return systemProperties;
  }

  public String getUnitCacheDir() {
    return unitCacheDir;
  }

  public String getUrlToOpen() {
    return urlToOpen;
  }

  public String getWarDir() {
    return warDir;
  }

  public String getCompiledWar() {
    return compileDirectory == null ? warDir : compileDirectory.getWarDir();
  }

  public String getWorkDir() {
    return normalizeWorkDir(workDir);
  }

  public boolean isAutoOpen() {
    return autoOpen;
  }

  public boolean isClosureCompiler() {
    return closureCompiler;
  }
  public boolean isDisableAggressiveOptimize() {
    return disableAggressiveOptimize;
  }
  public boolean isDisableCastCheck() {
    return disableCastCheck;
  }
  public boolean isDisableClassMetadata() {
    return disableClassMetadata;
  }
  public boolean isDisableRunAsync() {
    return disableRunAsync;
  }
  public boolean isDisableThreadedWorkers() {
    return disableThreadedWorkers;
  }
  public boolean isDisableUnitCache() {
    return disableUnitCache;
  }
  public boolean isDraftCompile() {
    return draftCompile;
  }
  public boolean isEnableAssertions() {
    return enableAssertions;
  }
  public boolean isSoyc() {
    return soyc;
  }
  public boolean isSoycDetailed() {
    return soycDetailed;
  }
  public boolean isStrict() {
    return strict;
  }
  public boolean isIncludeGenDir() {
    return includeGenDir;
  }
  public boolean isValidateOnly() {
    return validateOnly;
  }
  public GwtManifest setAutoOpen(boolean autoOpen) {
    this.autoOpen = autoOpen;
    return this;
  }
  public GwtManifest setClosureCompiler(boolean closureCompiler) {
    this.closureCompiler = closureCompiler;
    return this;
  }
  public GwtManifest setDependencies(IntTo<String> dependencies) {
    this.dependencies.clear();
    this.dependencies.addAll(dependencies.forEachItem()
        .map(SingletonIterator::singleItem)
        .map(Immutable::immutable1));
    return this;
  }
  public GwtManifest setDeployDir(String deployDir) {
    this.deployDir = deployDir;
    return this;
  }
  public GwtManifest setDisableAggressiveOptimize(boolean disableAggressiveOptimize) {
    this.disableAggressiveOptimize = disableAggressiveOptimize;
    return this;
  }
  public GwtManifest setDisableCastCheck(boolean disableCastCheck) {
    this.disableCastCheck = disableCastCheck;
    return this;
  }
  public GwtManifest setDisableClassMetadata(boolean disableClassMetadata) {
    this.disableClassMetadata = disableClassMetadata;
    return this;
  }
  public GwtManifest setDisableRunAsync(boolean disableRunAsync) {
    this.disableRunAsync = disableRunAsync;
    return this;
  }
  public GwtManifest setDisableThreadedWorkers(boolean disableThreadedWorkers) {
    this.disableThreadedWorkers = disableThreadedWorkers;
    return this;
  }
  public GwtManifest setDisableUnitCache(boolean disableUnitCache) {
    this.disableUnitCache = disableUnitCache;
    return this;
  }
  public GwtManifest setDraftCompile(boolean draftCompile) {
    this.draftCompile = draftCompile;
    return this;
  }
  public GwtManifest setEnableAssertions(boolean enableAssertions) {
    this.enableAssertions = enableAssertions;
    return this;
  }
  public GwtManifest setExtraArgs(IntTo<String> extraArgs) {
    this.extraArgs = extraArgs;
    return this;
  }
  public GwtManifest setExtrasDir(String extrasDir) {
    this.extrasDir = extrasDir;
    return this;
  }
  public GwtManifest setFragments(int fragments) {
    this.fragments = fragments;
    return this;
  }
  public GwtManifest setGenDir(String genDir) {
    this.genDir = genDir;
    return this;
  }
  public GwtManifest setGwtVersion(String gwtVersion) {
    this.gwtVersion = gwtVersion;
    return this;
  }
  public void setJvmArgs(IntTo<String> jvmArgs) {
    this.jvmArgs = jvmArgs;
  }
  public GwtManifest setLocalWorkers(int localWorkers) {
    this.localWorkers = localWorkers;
    return this;
  }

  public GwtManifest setLogLevel(Type logLevel) {
    this.logLevel = logLevel;
    return this;
  }

  public GwtManifest setSourceLevel(SourceLevel sourceLevel) {
    this.sourceLevel = sourceLevel;
    return this;
  }

  public GwtManifest setModuleName(String moduleName) {
    this.moduleName = moduleName;
    return this;
  }

  public GwtManifest setObfuscationLevel(ObfuscationLevel obfuscationLevel) {
    this.obfuscationLevel = obfuscationLevel;
    return this;
  }

  public GwtManifest setOpenAction(OpenAction openAction) {
    this.openAction = openAction;
    return this;
  }

  public GwtManifest setOptimizationLevel(int optimizationLevel) {
    this.optimizationLevel = optimizationLevel;
    return this;
  }

  public GwtManifest setPort(int port) {
    this.port = port;
    return this;
  }

  public GwtManifest setSources(IntTo<String> sources) {
    this.sources = sources;
    return this;
  }

  public GwtManifest setSoyc(boolean soyc) {
    this.soyc = soyc;
    return this;
  }

  public GwtManifest setSoycDetailed(boolean soycDetailed) {
    this.soycDetailed = soycDetailed;
    return this;
  }

  public GwtManifest setStrict(boolean strict) {
    this.strict = strict;
    return this;
  }

  public GwtManifest setIncludeGenDir(boolean includeGenDir) {
    this.includeGenDir = includeGenDir;
    return this;
  }

  public GwtManifest setSystemProperties(IntTo<String> systemProperties) {
    this.systemProperties = systemProperties;
    return this;
  }

  public GwtManifest setUnitCacheDir(String unitCacheDir) {
    this.unitCacheDir = unitCacheDir;
    return this;
  }

  public GwtManifest setUrlToOpen(String urlToOpen) {
    this.urlToOpen = urlToOpen;
    return this;
  }

  public GwtManifest setValidateOnly(boolean validateOnly) {
    this.validateOnly = validateOnly;
    return this;
  }

  public GwtManifest setWarDir(String warDir) {
    this.warDir = warDir;
    return this;
  }

  public GwtManifest setWorkDir(String workDir) {
    this.workDir = workDir;
    return this;
  }

  public String[] toProgramArgArray() {
    IntTo<String> fifo = X_Collect.newList(String.class);
    readProgramArgs((key, value)-> {
      if (!key.trim().isEmpty()) {
        fifo.add("-" + key);
      }
      if (value != null) {
        fifo.add(toString(value).trim());
      }
    });
    return fifo.toArray();
  }

  private String toString(Object value) {
    if (value instanceof SourceLevel) {
      return ((SourceLevel)value).getStringValue();
    }
    return String.valueOf(value);
  }

  public String toProgramArgs() {
    StringBuilder b = new StringBuilder();
    readProgramArgs(in2(
        arg1 -> {
          if (!arg1.trim().isEmpty()) {
            b.append(NEW_ARG);
            b.append(arg1);
          }
          b.append(" ");
        }, arg2->{
      if (arg2 != null) {
        b.append(toString(arg2)).append(" ");
      }
    }));
    return b.toString().trim();
  }
  public void readProgramArgs(In2<String, Object> read) {

    if (deployDir != null) {
      read.in(ARG_DEPLOY_DIR, deployDir);
    }
    if (extrasDir != null) {
      read.in(ARG_EXTRAS_DIR, extrasDir);
    }
    if (fragments != DEFAULT_FRAGMENTS) {
      read.in(ARG_FRAGMENTS, fragments);
    }
    if (genDir != null) {
      read.in(ARG_GEN_DIR, genDir);
    }
    if (localWorkers != 0) {
      read.in(ARG_LOCAL_WORKERS, localWorkers);
    }
    if (logLevel != DEFAULT_LOG_LEVEL) {
      read.in(ARG_LOG_LEVEL, logLevel.name());
    }
    if (sourceLevel != null) {
      read.in(ARG_SOURCE_LEVEL, sourceLevel.getStringValue());
    }
    if (obfuscationLevel != DEFAULT_OBFUSCATION) {
      String lvl;
      // using a switch case instead of enum name, in case enum types are reduced to ints by optimizers (like gwt)
      switch (obfuscationLevel) {
        case DETAILED:
          lvl = "DETAILED";
          break;
        case PRETTY:
          lvl = "PRETTY";
          break;
        case OBFUSCATED:
        default:
          lvl = "OBFUSCATED";
          break;
      }
      read.in(ARG_OBFUSCATION_LEVEL, lvl);
    }
    if (optimizationLevel != DEFAULT_OPTIMIZATION) {
      read.in(ARG_OPTIMIZATION_LEVEL, optimizationLevel);
    }
    if (warDir != null) {
      read.in(ARG_WAR_DIR, warDir);
    }
    if (workDir != null) {
      read.in(ARG_WORK_DIR, workDir);
    }
    if (extraArgs.size() > 0) {
      for (String arg : extraArgs.forEach()) {
        read.in(" ", arg);
      }
    }
    if (recompile) {
      if (port != 0) {
        read.in(ARG_PORT, port);
      }
      sources.forEachValue(read.provide1("src")::in);
      dependencies.forEachItem().map(Out1::out1).filter(Filter.ifNotNull())
        .forEach(read.provide1("src")::in);
    }
    if (closureCompiler) {
      read.in(ARG_ENABLE_CLOSURE, null);
    }
    if (disableAggressiveOptimize) {
      read.in(ARG_AGGRESSIVE_OPTIMIZE, null);
    }
    if (disableCastCheck) {
      read.in(ARG_CAST_CHECKING, null);
    }
    if (disableClassMetadata) {
      read.in(ARG_CAST_METADATA, null);
    }
    if (disableRunAsync) {
      read.in(ARG_RUN_ASYNC_ENABLED, null);
    }
    if (draftCompile) {
      read.in(ARG_DRAFT_COMPILE, null);
    }
    if (enableAssertions) {
      read.in(ARG_ENABLE_ASSERT, null);
    }
    if (soyc) {
      read.in(ARG_SOYC, null);
    }
    if (soycDetailed) {
      read.in(ARG_SOYC_DETAILED, null);
    }
    if (strict) {
      read.in(ARG_STRICT, null);
    }
    if (validateOnly) {
      read.in(ARG_VALIDATE_ONLY, null);
    }
    if (incremental) {
      read.in(ARG_INCREMENTAL, null);
    }
    read.in(" ", moduleName);

  }

  public String[] toClasspathFullCompile(String tempDir, String gwtHome) {
    return toClasspath(false, tempDir, gwtHome, getGwtVersion());
  }

  public String[] toClasspath(boolean includeCodeserver, String tempDir, String gwtHome, String gwtVersion) {
    IntTo<String> cp = newList(String.class);
    prefixClasspath(cp);
    // include our __gen dir?
    if (includeGenDir) {
      cp.add(getGenDir());
    }
    boolean hadGwtUser = false, hadGwtDev = false, hadGwtCodeserver = !includeCodeserver;
    for (String source : sources.forEach()) {
      if (source.contains("gwt-user")) {
        hadGwtUser = true;
      }
      if (source.contains("gwt-codeserver")) {
        hadGwtCodeserver = true;
      }
      if (source.contains("gwt-dev")) {
        hadGwtDev = true;
        addItemsBeforeGwtDev(cp, false);
      }
      cp.add(source);
    }
    for (Out1<? extends Iterable<String>> sourceOut : dependencies.forEach()) {
      if (sourceOut == null) {
        continue;
      }
      for (String source : sourceOut.out1()) {
        if (source == null) {
          continue;
        }
        if (source.contains("gwt-user")) {
          hadGwtUser = true;
        }
        if (source.contains("gwt-codeserver")) {
          hadGwtCodeserver = true;
        }
        if (source.contains("gwt-dev")) {
          hadGwtDev = true;
          addItemsBeforeGwtDev(cp, true);
        }
        cp.add(source);
      }
    }
    if (!hadGwtUser) {
      addGwtArtifact(cp, gwtHome, gwtVersion, "gwt-user");
    }
    if (!hadGwtCodeserver) {
      addGwtArtifact(cp, gwtHome, gwtVersion, "gwt-codeserver");
    }
    if (!hadGwtDev) {
      addGwtArtifact(cp, gwtHome, gwtVersion, "gwt-dev");
    }
    suffixClasspath(cp);
    X_Log.trace(getClass(), cp);
    cp.findRemove("", true);
    return cp.toArray();
  }

  protected void prefixClasspath(IntTo<String> cp) {
    // here for you to override
  }

  protected void suffixClasspath(IntTo<String> cp) {
    // here for you to override
  }

  protected void addGwtArtifact(IntTo<String> cp, String gwtHome, String gwtVersion, String artifact) {
    X_Log.info(getClass(), "Gwt home", gwtHome, "version", gwtVersion, "artifact", artifact);

    gwtHome = gwtHome.replace('\\', '/'); // just in case
    if (gwtHome.contains("gwt-dev")) {
      gwtHome = gwtHome.replace("gwt-dev", artifact);
    }
    if (gwtHome.endsWith("/")) {
      gwtHome = gwtHome.substring(0, gwtHome.length()-1);
    }
    if (gwtVersion.isEmpty()) {
      if (gwtHome.matches(".*([0-9]+[.]){2}[0-9]+$")) {
        gwtVersion = gwtHome.substring(gwtHome.lastIndexOf('/')+1);
      }
    }

    final String file = gwtHome+"/"+
        (gwtVersion.isEmpty() ?
            artifact :
            artifact.contains(gwtVersion) ?
                artifact :
                artifact + "-" + gwtVersion
        )+
        ".jar";

    X_Log.info(getClass(), "Adding gwt file", file);
    cp.add(file);
  }

  /**
   * This hook method is provided to allow subclasses to inject specific dependencies before gwt-dev.
   *
   * You really should be setting up your classpath in the correct order,
   * but if you want to inject certain dependencies and do it before gwt-dev,
   * this method is what you want to override.
   *
   * @param cp - Array of classpath elements; append to inject classpath items.
   * @param inDependencies - Whether gwt-dev was found in source or in dependencies
   *
   * You are recommended to check {@link #getSources()} and {@link #getDependencies()} for
   * duplicate artifacts; the inDependencies boolean will let you know how deeply you should search.
   */
  protected void addItemsBeforeGwtDev(IntTo<String> cp, boolean inDependencies) {
  }
  public String toJvmArgs() {
    return new SimpleFifo<String>(toJvmArgArray()).join(" ");
  }
  public String[] toJvmArgArray() {
    String[] args = new String[
       jvmArgs.size()+
       systemProperties.size()+
       (disableThreadedWorkers?0:2)+
       (disableUnitCache || unitCacheDir!=null?1:0)
     ];
    int pos = 0;
    for (String s : jvmArgs.forEach()) {
      if (!s.startsWith("-")) {
        s = "-"+s;
      }
      args[pos++] = s;
    }
    for (String s : systemProperties.forEach()) {
      if (!s.startsWith("-")) {
        if (!s.startsWith("D")) {
          s = "D"+s;
        }
        s = "-"+s;
      }
      args[pos++] = s;
    }

    if (!disableThreadedWorkers) {
      args[pos++] = "-Dgwt.jjs.permutationWorkerFactory=com.google.gwt.dev.ThreadedPermutationWorkerFactory";
      args[pos++] = "-Dgwt.jjs.maxThreads="+Math.max(1, localWorkers);
    }
    if (unitCacheDir != null) {
      args[pos++] = "-D"+ARG_UNIT_CACHE_DIR+ "="+unitCacheDir;
    } else if (disableUnitCache) {
      args[pos++] = "-D"+ARG_DISABLE_UNIT_CACHE+ "=true";

    }
    return args;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder(moduleName == null ? "null" + "GwtModule" : moduleName);
    b.append(":");
    if (deployDir != null) {
      b.append(NEW_LINE).append(ARG_DEPLOY_DIR).append(": ").append(deployDir);
    }
    if (extrasDir != null) {
      b.append(NEW_LINE).append(ARG_EXTRAS_DIR).append(": ").append(extrasDir);
    }
    if (fragments != DEFAULT_FRAGMENTS) {
      b.append(NEW_LINE).append(ARG_FRAGMENTS).append(": ").append(fragments);
    }
    if (genDir != null) {
      b.append(NEW_LINE).append(ARG_GEN_DIR).append(": ").append(genDir);
    }
    if (gwtVersion != null && gwtVersion.length() > 0) {
      b.append(NEW_LINE).append(ARG_GWT_VERSION).append(": ").append(gwtVersion);
    }
    if (localWorkers != DEFAULT_LOCAL_WORKERS) {
      b.append(NEW_LINE).append(ARG_LOCAL_WORKERS).append(": ").append(localWorkers);
    }
    if (logLevel != DEFAULT_LOG_LEVEL) {
      b.append(NEW_LINE).append(ARG_LOG_LEVEL).append(": ").append(logLevel.ordinal());
    }
    if (sourceLevel != null) {
      b.append(NEW_LINE).append(ARG_SOURCE_LEVEL).append(": ").append(sourceLevel.ordinal());
    }
    if (obfuscationLevel != DEFAULT_OBFUSCATION) {
      b.append(NEW_LINE).append(ARG_OBFUSCATION_LEVEL).append(": ").append(obfuscationLevel.ordinal());
    }
    if (openAction != DEFAULT_OPEN_ACTION) {
      b.append(NEW_LINE).append(ARG_OPEN_ACTION).append(": ").append(openAction.ordinal());
    }
    if (optimizationLevel != DEFAULT_OPTIMIZATION) {
      b.append(NEW_LINE).append(ARG_OPTIMIZATION_LEVEL).append(": ").append(optimizationLevel);
    }
    if (port != DEFAULT_PORT) {
      b.append(NEW_LINE).append(ARG_PORT).append(": ").append(port);
    }
    if (unitCacheDir != null) {
      b.append(NEW_LINE).append(ARG_UNIT_CACHE_DIR).append(": ").append(unitCacheDir);
    }
    if (urlToOpen != null) {
      b.append(NEW_LINE).append(ARG_URL_TO_OPEN).append(": ").append(urlToOpen);
    }
    if (warDir != null) {
      b.append(NEW_LINE).append(ARG_WAR_DIR).append(": ").append(warDir);
    }
    if (workDir != null) {
      b.append(NEW_LINE).append(ARG_WORK_DIR).append(": ").append(workDir);
    }
    if (dependencies.size() > 0) {
      b.append("\n ").append(ARG_DEPENDENCIES).append(":");
      for (int i = 0, m = dependencies.size(); i < m; i++) {
        for (String dep : dependencies.get(i).out1()) {
          b.append(NEW_LIST_ITEM).append(dep);
        }
      }
      b.append("\n");
    }
    if (extraArgs.size() > 0) {
      b.append("\n ").append(ARG_EXTRA_ARGS).append(":");
      for (int i = 0, m = extraArgs.size(); i < m; i++) {
        b.append(NEW_LIST_ITEM).append(extraArgs.get(i));
      }
      b.append("\n");
    }
    if (jvmArgs.size() > 0) {
      b.append("\n ").append(ARG_JVM_ARGS).append(":");
      for (int i = 0, m = jvmArgs.size(); i < m; i++) {
        b.append(NEW_LIST_ITEM).append(jvmArgs.get(i));
      }
      b.append("\n");
    }
    if (sources.size() > 0) {
      b.append("\n ").append(ARG_SOURCE_DIRS).append(":");
      for (int i = 0, m = sources.size(); i < m; i++) {
        b.append(NEW_LIST_ITEM).append(sources.get(i));
      }
      b.append("\n");
    }
    if (systemProperties.size() > 0) {
      b.append("\n ").append(ARG_SYS_PROPS).append(":");
      for (int i = 0, m = systemProperties.size(); i < m; i++) {
        b.append(NEW_LIST_ITEM).append(systemProperties.get(i));
      }
      b.append("\n");
    }
    if (autoOpen) {
      b.append(NEW_ITEM).append(ARG_AUTO_OPEN);
    }
    if (closureCompiler) {
      b.append(NEW_ITEM).append(ARG_ENABLE_CLOSURE);
    }
    if (disableAggressiveOptimize) {
      b.append(NEW_ITEM).append(ARG_AGGRESSIVE_OPTIMIZE);
    }
    if (disableCastCheck) {
      b.append(NEW_ITEM).append(ARG_CAST_CHECKING);
    }
    if (disableClassMetadata) {
      b.append(NEW_ITEM).append(ARG_CAST_METADATA);
    }
    if (disableRunAsync) {
      b.append(NEW_ITEM).append(ARG_RUN_ASYNC_ENABLED);
    }
    if (draftCompile) {
      b.append(NEW_ITEM).append(ARG_DRAFT_COMPILE);
    }
    if (enableAssertions) {
      b.append(NEW_ITEM).append(ARG_ENABLE_ASSERT);
    }
    if (soyc) {
      b.append(NEW_ITEM).append(ARG_SOYC);
    }
    if (soycDetailed) {
      b.append(NEW_ITEM).append(ARG_SOYC_DETAILED);
    }
    if (strict) {
      b.append(NEW_ITEM).append(ARG_STRICT);
    }
    if (disableUnitCache) {
      b.append(NEW_ITEM).append(ARG_DISABLE_UNIT_CACHE);
    }
    if (disableThreadedWorkers) {
      b.append(NEW_ITEM).append(ARG_DISABLE_THREADED);
    }
    if (validateOnly) {
      b.append(NEW_ITEM).append(ARG_VALIDATE_ONLY);
    }
    if (incremental) {
      b.append(NEW_ITEM).append(ARG_INCREMENTAL);
    }

    return b.toString();
  }

  public Iterable<String> getClasspath() {
    return new ClasspathIterable();
  }

  public CleanupMode getCleanupMode() {
    return cleanupMode;
  }

  public void setCleanupMode(CleanupMode cleanupMode) {
    this.cleanupMode = cleanupMode;
  }

  public GwtcXmlBuilder getOrCreateBuilder(String pkgName, String fileName) {
    String key = X_Source.qualifiedName(pkgName, fileName);
    return gwtBuilders.getOrCreate(key, k->new GwtcXmlBuilder(pkgName, fileName));
  }

  public Iterable<GwtcXmlBuilder> getModules() {
    return gwtBuilders.values();
  }

  public boolean isUseCurrentJvm() {
    return useCurrentJvm;
  }

  public void setUseCurrentJvm(boolean useCurrentJvm) {
    this.useCurrentJvm = useCurrentJvm;
  }

  public void addDependencies(Out1<? extends Iterable<String>> out1) {
    if (out1 != null) {
      dependencies.add(out1);
    }
  }

  public boolean isRecompile() {
    return recompile;
  }

  public void setRecompile(boolean recompile) {
    this.recompile = recompile;
  }

  public void setCompileDirectory(CompiledDirectory compileDirectory) {
    this.compileDirectory = compileDirectory;
  }

  public CompiledDirectory getCompileDirectory() {
    return compileDirectory;
  }

  public boolean isOnline() {
    return online;
  }

  public void setOnline(boolean online) {
    this.online = online;
  }

  public MethodNameMode getMethodNameMode() {
    return mode;
  }

  public void setMethodNameMode(MethodNameMode mode) {
    this.mode = mode;
  }

  public boolean isIncremental() {
    return incremental;
  }

  public void setIncremental(boolean incremental) {
    this.incremental = incremental;
  }

  public CompiledDirectory computeCompileDirectory() {
    compileDirectory = new CompiledDirectory();
    compileDirectory.setDeployDir(getDeployDir());
    compileDirectory.setExtraDir(getExtrasDir());
    compileDirectory.setGenDir(getGenDir());
    compileDirectory.setPort(getPort());
    compileDirectory.setWarDir(getWarDir());
    compileDirectory.setWorkDir(getWorkDir());
    compileDirectory.setStrategy(isIncremental() ? CompileStrategy.INCREMENTAL : CompileStrategy.FULL);
    // TODO add logFile and UserAgent mappings.
    // when using a java main, it should send back a response on success
    // containing the actual directories and mappings to use.
    return compileDirectory;
  }

  public String getRelativeRoot() {
    return relativeRoot;
  }

  public void setRelativeRoot(String relativeRoot) {
    this.relativeRoot = relativeRoot;
  }
}
