package xapi.gwtc.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.impl.SimpleFifo;
import xapi.log.X_Log;

import com.google.gwt.core.ext.TreeLogger.Type;

public class GwtManifest {

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
  private static final String ARG_GEN_DIR = "genDir";
  private static final String ARG_GWT_VERSION = "gwtVersion";
  private static final String ARG_JVM_ARGS = "jvmArgs";
  private static final String ARG_LOCAL_WORKERS = "localWorkers";
  private static final String ARG_LOG_LEVEL = "logLevel";
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
  
  private boolean autoOpen;
  private boolean closureCompiler;
  private IntTo<String> dependencies = X_Collect.newList(String.class);
  private String deployDir;
  private boolean disableAggressiveOptimize;
  private boolean disableCastCheck;
  private boolean disableClassMetadata;
  private boolean disableRunAsync;
  private boolean disableThreadedWorkers;
  private boolean disableUnitCache;
  private boolean draftCompile;
  private boolean enableAssertions;
  private IntTo<String> extraArgs = X_Collect.newList(String.class);
  private String extrasDir;
  private int fragments = DEFAULT_FRAGMENTS;
  private String genDir; 
  private String gwtVersion = "";
  private IntTo<String> jvmArgs =  X_Collect.newList(String.class);
  private int localWorkers = DEFAULT_LOCAL_WORKERS;
  private Type logLevel = DEFAULT_LOG_LEVEL;
  private String moduleName;
  private ObfuscationLevel obfuscationLevel = DEFAULT_OBFUSCATION;
  private OpenAction openAction = DEFAULT_OPEN_ACTION;
  private int optimizationLevel = DEFAULT_OPTIMIZATION;
  private int port = DEFAULT_PORT;
  private IntTo<String> sources = X_Collect.newList(String.class);
  private boolean soyc;
  private boolean soycDetailed;
  private boolean strict;
  private IntTo<String> systemProperties = X_Collect.newList(String.class);
  private String unitCacheDir;
  private String urlToOpen;
  private boolean validateOnly;
  private String workDir;
  private String warDir;

  public GwtManifest() {
  }
  
  public GwtManifest(String moduleName) {
    this.moduleName = moduleName;
  }

  public GwtManifest addDependency(String dep) {
    assert !dependencies.contains(dep) : "Dependencies already contains "+dep;
    dependencies.push(dep);
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
  public IntTo<String> getDependencies() {
    return dependencies;
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

  public String getWorkDir() {
    return workDir;
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
    this.dependencies = dependencies;
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

  public String toProgramArgs() {
    return toProgramArgs(false);
  }
  
  public String toProgramArgs(boolean isRecompile) {
    StringBuilder b = new StringBuilder();
    
    if (deployDir != null) {
      b.append(NEW_ARG).append(ARG_DEPLOY_DIR).append(" ").append(deployDir);
    }
    if (extrasDir != null) {
      b.append(NEW_ARG).append(ARG_EXTRAS_DIR).append(" ").append(extrasDir);
    }
    if (fragments != DEFAULT_FRAGMENTS) {
      b.append(NEW_ARG).append(ARG_FRAGMENTS).append(" ").append(fragments);
    }
    if (genDir != null) {
      b.append(NEW_ARG).append(ARG_GEN_DIR).append(" ").append(genDir);
    }
    if (localWorkers != 0) {
      b.append(NEW_ARG).append(ARG_LOCAL_WORKERS).append(" ").append(localWorkers);
    }
    if (logLevel != DEFAULT_LOG_LEVEL) {
      b.append(NEW_ARG).append(ARG_LOG_LEVEL).append(" ").append(logLevel.name());
    }
    if (obfuscationLevel != DEFAULT_OBFUSCATION) {
      b.append(NEW_ARG).append(ARG_OBFUSCATION_LEVEL).append(" ");
      switch (obfuscationLevel) {
        case DETAILED:
          b.append("DETAILED");
          break;
        case PRETTY:
          b.append("PRETTY");
          break;
        case OBFUSCATED:
          b.append("OBFUSCATED");
          break;
      }
    }
    if (optimizationLevel != DEFAULT_OPTIMIZATION) {
      b.append(NEW_ARG).append(ARG_OPTIMIZATION_LEVEL).append(" ").append(optimizationLevel);
    }
    if (warDir != null) {
      b.append(NEW_ARG).append(ARG_WAR_DIR).append(" ").append(warDir);
    }
    if (workDir != null) {
      b.append(NEW_ARG).append(ARG_WORK_DIR).append(" ").append(workDir);
    }
    if (extraArgs.size() > 0) {
      for (String arg : extraArgs.forEach()) {
        b.append(" ").append(arg);
      }
    }
    if (isRecompile) {
      if (port != 0) {
        b.append(NEW_ARG).append(ARG_PORT).append(" ").append(port);
      }
      for (String source : sources.forEach()) {
        b.append(NEW_ARG).append("src ").append(source);
      }
      for (String source : dependencies.forEach()) {
        b.append(NEW_ARG).append("src ").append(source);
      }
    }
    if (closureCompiler) {
      b.append(NEW_ARG).append(ARG_ENABLE_CLOSURE);
    }
    if (disableAggressiveOptimize) {
      b.append(NEW_ARG).append(ARG_AGGRESSIVE_OPTIMIZE);
    }
    if (disableCastCheck) {
      b.append(NEW_ARG).append(ARG_CAST_CHECKING);
    }
    if (disableClassMetadata) {
      b.append(NEW_ARG).append(ARG_CAST_METADATA);
    }
    if (disableRunAsync) {
      b.append(NEW_ARG).append(ARG_RUN_ASYNC_ENABLED);
    }
    if (draftCompile) {
      b.append(NEW_ARG).append(ARG_DRAFT_COMPILE);
    }
    if (enableAssertions) {
      b.append(NEW_ARG).append(ARG_ENABLE_ASSERT);
    }
    if (soyc) {
      b.append(NEW_ARG).append(ARG_SOYC);
    }
    if (soycDetailed) {
      b.append(NEW_ARG).append(ARG_SOYC_DETAILED);
    }
    if (strict) {
      b.append(NEW_ARG).append(ARG_STRICT);
    }
    if (validateOnly) {
      b.append(NEW_ARG).append(ARG_VALIDATE_ONLY);
    }
    
    b.append(" ").append(moduleName);
    
    return b.toString().trim();
  }
  
  public String[] toClasspathFullCompile(String gwtHome) {
    return toClasspath(false, gwtHome, getGwtVersion());
  }
  public String[] toClasspath(boolean includeCodeserver, String gwtHome, String gwtVersion) {
    IntTo<String> cp = X_Collect.newList(String.class);
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
    for (String source : dependencies.forEach()) {
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
    if (!hadGwtUser) {
      addGwtArtifact(cp, gwtHome, gwtVersion, "gwt-user");
    }
    if (!hadGwtCodeserver) {
      addGwtArtifact(cp, gwtHome, gwtVersion, "gwt-codeserver");
    }
    if (!hadGwtDev) {
      addGwtArtifact(cp, gwtHome, gwtVersion, "gwt-dev");
    }
    X_Log.trace(getClass(), cp);
    return cp.toArray();
  }
  protected void addGwtArtifact(IntTo<String> cp, String gwtHome, String gwtVersion, String artifact) {
    if (gwtHome.contains("gwt-dev")) {
      gwtHome = gwtHome.replace("gwt-dev", artifact);
    }
    cp.add(gwtHome+
        (gwtHome.endsWith("/")?"":"/")+
        artifact+
        (gwtVersion.length()>0?"-"+gwtVersion:"")+
        ".jar");
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
    assert moduleName != null : "ModuleName is the only field that cannot be null";
    StringBuilder b = new StringBuilder(moduleName);
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
        b.append(NEW_LIST_ITEM).append(dependencies.get(i));
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
    
    return b.toString();
  }
  
}
