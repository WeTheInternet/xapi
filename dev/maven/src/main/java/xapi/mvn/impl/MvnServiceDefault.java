package xapi.mvn.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import xapi.annotation.inject.SingletonDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.InitMap;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.collect.init.AbstractMultiInitMap;
import xapi.collect.init.InitMapDefault;
import xapi.dev.api.ProjectSources;
import xapi.dev.resource.impl.StringDataResource;
import xapi.dev.scanner.X_Scanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.file.X_File;
import xapi.fu.Filter.Filter1;
import xapi.fu.*;
import xapi.fu.Out1.Out1Unsafe;
import xapi.fu.itr.*;
import xapi.fu.java.X_Jdk;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.model.X_Model;
import xapi.model.impl.ModelSerializerDefault;
import xapi.mvn.X_Maven;
import xapi.mvn.api.MvnDependency;
import xapi.mvn.service.MvnService;
import xapi.reflect.X_Reflect;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Debug;
import xapi.constants.X_Namespace;
import xapi.util.X_Properties;
import xapi.util.X_Util;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import static xapi.fu.itr.Chain.startChain;
import static xapi.fu.itr.SingletonIterator.singleItem;
import static xapi.util.X_Properties.getProperty;
import static xapi.string.X_String.isEmpty;

@SingletonDefault(implFor = MvnService.class)
public class MvnServiceDefault implements MvnService {

  private final MvnCacheImpl cache;
  private final Lazy<SizedIterable<Path>> workspaces;
  private final Lazy<SizedIterable<String>> searchGroups;
  private final Lazy<StringTo<Model>> localProjects;
  private final Lazy<StringTo<ProjectSources>> localSources;
  private final Lazy<In1Out1<String, Boolean>> allowedToResolve;
  private final Lazy<In1Out1<String, Boolean>> notAllowedToResolve;

  private final InitMap<Artifact, ArtifactResult> lookupCache;

  public MvnServiceDefault() {
    cache = new MvnCacheImpl(this);
    workspaces = Lazy.deferred1(this::loadWorkspaces);
    searchGroups = Lazy.deferred1(this::loadSearchGroups);
    localProjects = Lazy.deferred1(this::loadLocalProjects);
    localSources = Lazy.deferred1(this::loadLocalSources);
    allowedToResolve = Lazy.deferred1(this::loadAllowedToResolve);
    notAllowedToResolve = Lazy.deferred1(this::loadNotAllowedToResolve);
    lookupCache = new InitMapDefault<>(
        Artifact::toString, this::resolveArtifact);
  }

  private ArtifactResult resolveArtifact(Artifact artifact) {
      Moment before = X_Time.now();
      RepositorySystem repoSystem = this.repoSystem.out1();
      RepositorySystemSession session = this.session.out1();
      try {
        final LocalArtifactRequest localRequest = new LocalArtifactRequest(artifact, remoteRepos(), null);
        final LocalArtifactResult result = session.getLocalRepositoryManager().find(session, localRequest);
        ArtifactRequest request = new ArtifactRequest(artifact, remoteRepos(), null);
        if (result.isAvailable()) {
          final ArtifactResult artifactResult = new ArtifactResult(request);
          final Artifact withFile = artifact.setFile(result.getFile());
          artifactResult.setArtifact(withFile);
          artifactResult.setRepository(result.getRepository());
          return artifactResult;
        }
        // TODO: add a flag to cache any missed resources into the xapiBuild cache.
        return repoSystem.resolveArtifact(session, request);
      } catch (ArtifactResolutionException e) {
          boolean forPom = "pom".equals(artifact.getExtension());
          final LogLevel level = forPom ? LogLevel.TRACE : getLogLevel();
          X_Log.log(MvnServiceDefault.class, level, "Resolved? ", e.getResult().isResolved(), e.getResult().getExceptions());
          X_Log.log(MvnServiceDefault.class, level, "Could not download " + artifact, e);
          if (forPom) {
              // we'll ignore missing-pom failures for now...
              return null;
          } else {
              throw X_Debug.rethrow(e);
          }
      } finally {
        if (X_Log.loggable(LogLevel.DEBUG)) {
          X_Log.debug("Resolved: " + artifact.toString() + " in "
              + X_Time.difference(before));
        }
      }
  }

  protected In1Out1<String, Boolean> loadAllowedToResolve() {
    final String regex = X_Properties.getProperty(X_Namespace.PROPERTY_MAVEN_RESOLVABLE, ".*");
    final Pattern pattern = Pattern.compile(regex);
    return In1Out1.of(pattern::matcher)
        .mapIn(String::toString)
        .mapOut(Matcher::matches);
  }

  protected In1Out1<String, Boolean> loadNotAllowedToResolve() {
    final String regex = X_Properties.getProperty(X_Namespace.PROPERTY_MAVEN_UNRESOLVABLE, ".*uber.*");
    if (isEmpty(regex)) {
      return In1Out1.RETURN_FALSE;
    }
    final Pattern pattern = Pattern.compile(regex);
    return In1Out1.of(pattern::matcher)
        .mapIn(String::toString)
        .mapOut(Matcher::matches);
  }

  protected StringTo<ProjectSources> loadLocalSources() {
    StringTo<ProjectSources> models = X_Collect.newStringMap(ProjectSources.class);

    return models;
  }

  protected StringTo<Model> loadLocalProjects() {
    StringTo<Model> models = X_Collect.newStringMap(Model.class);
    workspaces.out1().forAllUnsafe(p->{
        Path resolved;
        // hm...  in reality, we should be reading the xapi.schema file here,
        // instead of relying on publication snooping... ...but, we don't intend
        // to support third party use cases at this time.
        resolved = p.resolve("build/publications");
        if (Files.exists(resolved)) { // if we have publications, use the defaults
            try (DirectoryStream<Path> publications = Files.newDirectoryStream(resolved)) {
                for (Path module : publications) {
                    final Path pom = module.resolve("pom-default.xml");
                    if (Files.exists(pom)) {
                        final Model root = loadPomFile(pom.toString());
                        if (root != null) {
                            recursiveLoadLocalProject(models, root, pom.getParent());
                        }
                    }
                }
            }
            return; // if build/publications exists, use it.
        }
        Model pom = findAPom(p);
        if (pom != null) {
            final Path asPath = Paths.get(pom.getPomFile().getAbsolutePath());
            recursiveLoadLocalProject(models, pom, asPath.getParent());
        }
    });
    return models;
  }

  protected void recursiveLoadLocalProject(StringTo<Model> models, Model root, Path path)
  throws IOException, XmlPullParserException {
      findXapiMeta(root, path);
    String groupId = findGroupId(root);
    if (groupId != null) {
      models.put(groupId + ":" + root.getArtifactId(), root);
    }
    models.putIfUnchanged(root.getArtifactId(), null, root);
      final List<String> modules = root.getModules();
      if (modules != null) {
          for (String module : modules) {
              final Path modDir = path.resolve(module);
              final Model child = findAPom(modDir);
              if (child != null) {
                  Path asPath = Paths.get(child.getPomFile().getAbsolutePath());
                  try {
                      recursiveLoadLocalProject(models, child, asPath.getParent());
                  } catch (XmlPullParserException | IOException e) {
                      X_Log.warn(MvnServiceDefault.class, "Invalid pom @ ", asPath, e);
                  }
              } else {
                  // hideous warning...?
                  X_Log.warn(MvnServiceDefault.class, "Unable to find a module at ", modDir);
              }
          }

      }
  }

    private void findXapiMeta(Model root, Path path) {
        final Path mavenPath = path.resolve("target/xapi-paths/META-INF/xapi/settings.xapi");
        if (Files.exists(mavenPath)) {
            addXapiMeta(root, mavenPath);
        } else {
            // As a backup for maven...  gradle may be used to produce the settings files,
            // despite the fact that poms are still left in the project (current state of xapi).
            // Be forgiving in such cases, and grab those gradle paths.
            final Path gradlePath = path.resolve("build/xapi-paths/META-INF/xapi/settings.xapi");
            if (Files.exists(gradlePath)) {
                addXapiMeta(root, gradlePath);

            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addXapiMeta(Model root, Path path) {
        try {
            String settings = MappedIterable.mapped(Files.readAllLines(path)).join("\n");
            final UiContainerExpr el = JavaParser.parseUiContainer(path.toString(), settings);
            final ComposableXapiVisitor<MvnServiceDefault> vis = ComposableXapiVisitor.onMissingFail(MvnServiceDefault.class);

            ChainBuilder<String> sources = startChain(), resources = startChain(), outputs = startChain();
            ChainBuilder<String>[] target = new ChainBuilder[1];
            vis
                .withUiContainerTerminal((els, ignr)-> {
                    els.getAttribute("sources")
                        .readIfPresent(s->s.accept(vis, ignr));
                    els.getAttribute("resources")
                        .readIfPresent(s->s.accept(vis, ignr));
                    els.getAttribute("outputs")
                        .readIfPresent(s->s.accept(vis, ignr));
                })
                .withJsonContainerRecurse(In2.ignoreAll())
                .withJsonPairRecurse(In2.ignoreAll())
                .withIntegerLiteralTerminal(In2.ignoreAll())
                .withUiAttrTerminal((attr, ignr)->{
                    switch (attr.getNameString()) {
                        case "sources":
                            target[0] = sources;
                            attr.getExpression().accept(vis, ignr);
                            target[0] = null;
                            break;
                        case "resources":
                            target[0] = resources;
                            attr.getExpression().accept(vis, ignr);
                            target[0] = null;
                            break;
                        case "outputs":
                            target[0] = outputs;
                            attr.getExpression().accept(vis, ignr);
                            target[0] = null;
                            break;
                    }
                })
                .withStringLiteralTerminal((str, ignr)->{
                    assert target[0] != null : "Invalid settings.xapi; visited string literal without first visiting " +
                        "a supported attribute; bad source: " + el.toSource();
                    final String val = str.getValue();
                    if (new File(val).exists()) {
                        target[0].add(val);
                    }
                })
                .visit(el, this);

            if (sources.isEmpty() && resources.isEmpty() && outputs.isEmpty()) {
                return;
            }
            final Properties props = root.getProperties();
            props.setProperty("xapi-paths", "true");
            if (sources.isNotEmpty()) {
                props.setProperty("xapi-source-length", Integer.toString(sources.size()));
                sources.forAllIndexed((src, i)->props.setProperty("xapi-source-" + i, src));
            }
            if (resources.isNotEmpty()) {
                props.setProperty("xapi-resource-length", Integer.toString(sources.size()));
                sources.forAllIndexed((src, i)->props.setProperty("xapi-resource-" + i, src));
            }
            if (outputs.isNotEmpty()) {
                props.setProperty("xapi-output-length", Integer.toString(sources.size()));
                sources.forAllIndexed((src, i)->props.setProperty("xapi-output-" + i, src));
            }
        } catch (IOException|ParseException e) {
            X_Log.warn(MvnServiceDefault.class, "Error reading ", path, e);
        }
    }

    private String findGroupId(Model root) {
      if (root.getGroupId() != null) {
        return root.getGroupId();
      }
      if (root.getParent() == null) {
        // malformed pom...
        return null;
      }
      return root.getParent().getGroupId();
  }

  protected SizedIterable<String> loadSearchGroups() {
    String configured = System.getProperty(X_Namespace.PROPERTY_MAVEN_SEARCH_GROUPS);
    if (configured != null) {
      return ArrayIterable
          .iterate(configured.split("[ ]"))
          .cached();
    }
    // not configured.  Compute from workspace root poms.
    return workspaces.out1()
        .mapUnsafe(p->{
            final Model f = findAPom(p);
            return f == null ? null : f.getGroupId();
        })
        .filterNull()
        .cached();
  }

    protected Model findAPom(Path p) throws IOException, XmlPullParserException {

        Path resolved = p.resolve("build/publications/_main_main/pom-default.xml");
        if (Files.exists(resolved)) {
            return loadPomFile(resolved.toString());
        }

        resolved = p.resolve("build/generated/poms/pom.xml");
        if (Files.exists(resolved)) {
            return loadPomFile(resolved.toString());
        }

        // we deprioritize a plain pom.xml, since our builds are all gradle builds, these days.
        // if someone in a maven project wants to put in some wiring to optionally test this sooner,
        // a pull request is welcome.
        resolved = p.resolve("pom.xml");
        if (Files.exists(resolved)) {
            return loadPomFile(resolved.toString());
        }

        // finally, in case it's a jar in a directory w/ it's gradle artifactId .pom, look at sibling directories.
        p = p.resolve(p.getFileName().toString().replaceAll(".jar$", ".pom"));
        if (Files.exists(p)) {
            return loadPomFile(p.toString());
        } else {
            return null;
        }
    }

    protected SizedIterable<Path> loadWorkspaces() {
    String locations = System.getProperty(X_Namespace.PROPERTY_MAVEN_WORKSPACE, "/opt/xapi /opt/wti");
    return ArrayIterable
        .iterate(locations.split("(?<!\\\\)\\s+"))
        .map(p->{
          Path path = Paths.get(p.replaceAll("\\\\\\s+", " "));
          if (!Files.exists(path)) {
            path = tryToGuessWorkspace(p);
          }
          return path;
        })
        .filterNull()
        .mapUnsafe(Path::toRealPath)
        .cached();
  }

  protected Path tryToGuessWorkspace(String p) {
    if ("/opt/xapi".equals(p)) {
      // This one is special; we can find it from our own codesource.
      final String localLoc = X_Reflect.getFileLoc(MvnServiceDefault.class);
      if (localLoc != null) {
        Path local = Paths.get(localLoc);
        if (Files.isDirectory(local)) {
          // winning!  remove dev/maven/target/classes
          assert "classes".equals(local.getFileName());
          local = local.getParent();
          assert "target".equals(local.getFileName());
          local = local.getParent();
          assert "maven".equals(local.getFileName());
          local = local.getParent();
          assert "dev".equals(local.getFileName());
          return local.getParent(); // return workspace root :-)
        }
      }
    }
    return null;
  }

  private static final Pattern POM_PATTERN = Pattern.compile(".*pom.*xml");

  protected final DefaultServiceLocator maven = MavenRepositorySystemUtils.newServiceLocator();

  protected final Lazy<RepositorySystem> repoSystem = Lazy.deferred1(() -> {
      // use netty to stream from maven

      maven.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
      maven.addService( TransporterFactory.class, FileTransporterFactory.class );
      maven.addService( TransporterFactory.class, HttpTransporterFactory.class );
      return maven.getService(RepositorySystem.class);
    }
  );

  @Override
  public RepositorySystemSession getRepoSession() {
    return session.out1();
  }

  protected final Lazy<RepositorySystemSession> session = Lazy.deferred1(this::initLocalRepo);

  private LogLevel logLevel = LogLevel.INFO;

  private final class ResourceMap extends
      AbstractMultiInitMap<Integer, ClasspathResourceMap, ClassLoader> {
    @SuppressWarnings("unchecked")
    public ResourceMap() {
      super(TO_STRING);
    }

    @Override
    protected ClasspathResourceMap initialize(Integer key, ClassLoader params) {
      return X_Scanner.scanClassloader(params);
    }
  }

  @Override
  public ArtifactResult loadArtifact(String groupId, String artifactId, String version) {
    return loadArtifact(groupId, artifactId, "", "jar", version);
  }

  @Override
  public ArtifactResult loadArtifact(String groupId, String artifactId,
      String classifier, String extension, String version) {

    DefaultArtifact artifact = new DefaultArtifact( groupId,artifactId, normalize(classifier), isEmpty(extension) ? "jar" : extension, version);

    return lookupCache.get(artifact);
  }

  @Override
  public LocalArtifactResult loadLocalArtifact(String groupId, String artifactId, String version) {
    return loadLocalArtifact(groupId, artifactId, "", "jar", version);
  }

  @Override
  public LocalArtifactResult loadLocalArtifact(String groupId, String artifactId,
      String classifier, String extension, String version) {
    Moment before = X_Time.now();
    RepositorySystemSession session = this.session.out1();
    DefaultArtifact artifact = new DefaultArtifact(
        groupId,
        artifactId,
        normalize(classifier),
        isEmpty(extension) ? "jar" : extension,
        version
    );

    try {
      LocalArtifactRequest request = new LocalArtifactRequest(artifact, remoteRepos(), null);
      return session.getLocalRepositoryManager().find(session, request);
    } finally {
      if (X_Log.loggable(LogLevel.DEBUG)) {
        X_Log.debug("Resolved: " + artifact.toString() + " in "
            + X_Time.difference(before));
      }
    }
  }

  @Override
  public String normalize(String classifier) {
    if (classifier == null) {
      return "";
    }

    if ("${os.detected.classifier}".equals(classifier)) {
      String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
      if (os.contains("win")) {
        return "windows-x86_64";
      } else if (os.contains("mac")) {
        return "osx-x86_64";
      } else {
        return "linux-x86_64";
      }
    }
    return classifier;
  }

  protected LogLevel getLogLevel() {
    return logLevel;
  }

  @Override
  public void setLogLevel(LogLevel logLevel) {
    this.logLevel = logLevel;
  }

  protected RepositorySystemSession initLocalRepo() {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    String localLocation = getProperty(X_Namespace.PROPERTY_MAVEN_REPO, ()->{
      String userHome = getProperty("user.home");
      File f = new File(userHome, ".m2" + File.separator + "repository");
      if (f.exists()) {
        return f.getAbsolutePath();
      }
      if (new File("target").exists()) {
        f = new File("target", "local-repo");
      } else {
        f = X_File.createTempDir("xapi", true);
      }
      if (!f.exists()) {
        if ("false".equals(X_Properties.getProperty(X_Namespace.PROPERTY_MAVEN_REPO_AUTOCREATE))) {
          X_Log.error(MvnServiceDefault.class, "Could not find .m2 repository in " + f.getAbsolutePath());
          throw new IllegalStateException("No local repository available.  Set system property xapi.mvn.repo.autocreate to create repository in " + f.getAbsolutePath());
        }
        final boolean success = f.mkdirs();
        if (!success) {
          X_Log.warn(MvnServiceDefault.class, "Unable to create new local repository in ", f);
        }
      }
      return f.getAbsolutePath();
    });

    LocalRepository localRepo = new LocalRepository(localLocation);
    localRepo.getBasedir().mkdirs();
    session.setLocalRepositoryManager(repoSystem.out1()
        .newLocalRepositoryManager(session, localRepo));
    return session;
  }

  @Override
  public Model loadPomFile(String pomLocation) throws IOException,
      XmlPullParserException {
    File pomfile = new File(pomLocation);
    if (!pomfile.isFile()) {
        return null;
    }
    FileReader reader;
    MavenXpp3Reader mavenreader = new MavenXpp3Reader();
    reader = new FileReader(pomfile);
    Model model = mavenreader.read(reader);
    model.setPomFile(pomfile);
    return model;
  }

  @Override
  public Model loadPomString(String pomString) throws XmlPullParserException {
    try {
      return new MavenXpp3Reader().read(new StringReader(pomString));
    } catch (IOException ignored) {
      throw X_Util.rethrow(ignored);
    }
  }

  @Override
  public List<String> loadDependencies(Artifact artifact, Filter1<Dependency> filter) {
    Map<String, String> dependencies = Collections.synchronizedMap(new LinkedHashMap<>());

    loadInto(dependencies, artifact, filter);

    // We use the ListIterator interface in our transform implementation
    final ArrayList<String> values = new ArrayList<>(dependencies.values());
    final Set<String> transformed = transform(values);

    return new ArrayList<>(transformed);
  }

  protected Set<String> transform(List<String> values) {
    Set<String> items = new LinkedHashSet<>();
    for (String dependency : values) {

        final ProjectSources sources = localSources.out1().getOrCreate(dependency, d -> {
          if (shouldTransform(dependency)) {
            ProjectSources replaceWith = maybeTransform(dependency);
            return replaceWith;
          }
          return null;
        });

        if (sources != null) {
          // TODO: a much less broad brush...
          sources.sources().forAll(items::add);
          sources.resources().forAll(items::add);
          if (dependency.toLowerCase(Locale.ENGLISH).contains("test")) {
            sources.testSources().forAll(items::add);
            sources.testResources().forAll(items::add);
          }
          // check for properties that define gradle output directories
          sources.outputs().forAll(items::add);
        } else if (new File(dependency).isDirectory()) {
          if (!dependency.endsWith("/")) {
            items.add(dependency + "/");
          } else {
            items.add(dependency);
          }
        } else {
          items.add(dependency);
        }
      }
      return items;
  }

  protected boolean shouldTransform(String dependency) {
    return allowedToResolve.out1().io(dependency) && !notAllowedToResolve.out1().io(dependency);
  }

  protected ProjectSources maybeTransform(String dependency) {
    if (dependency.endsWith(".jar")) {
      // whenever possible, transform a jar from maven
      // into a set of local workspace workspace directories.
      // While it would be nice to load the actual reactors,
      // and figure out what we should add, we'll just brute
      // force this with settings used in our projects,
      // and leave hooks (this method) to allow others to do the same.
      try {
        // This is... a bit scary, but, well... it works.
        final Path jarPath = Paths.get(dependency).toRealPath();
        final Path versionPath = jarPath.getParent();
        final Path artifactPath = versionPath.getParent();
        // Now, it may be possible that someone is using a maven
        // repository that is not in the default system location;
        // by default, group.id is in .../repository/group/id
        // so, to find groupId from the path, we will have a set
        // of registered search groups to match, plus we will
        // break if we encounter a path segment named "repository".
        final SizedIterable<String> searchGroups = getSearchGroups();
        final SizedIterable<Path> searchDirs = workspaces.out1();

        Path lastGroupId = artifactPath.getParent();
        String lastGroup = lastGroupId.getFileName().toString();
        String groupId = null;
        final String[][] search = ArrayIterable
            .iterate(searchGroups.toArray(String[]::new))
            .reversed()
            .map2(String::split, "[.]")
            // lets only consider groupIds where the last segment of said groupId
            // is the current parent of the artifactId directory.
            .filter(searches->lastGroup.equals(searches[searches.length-1]))
            .toArray(String[][]::new);
        if (search.length != 0) {
          // there is / are valid search group(s).
          // try each one.
          Path test = lastGroupId;
          search:
          for (String[] gId : search) {
            for (int i = gId.length; i-->0;) {
              if (gId[i].equals(test.getFileName().toString())) {
                test = test.getParent();
              } else {
                continue search;
              }
            }
            // If we made it here, we have ourselves a match!
            groupId = test.relativize(lastGroupId).toString().replace('/', '.');
            break;
          }
        }
        if (groupId == null) {
          // try looking at parents until we encounter "repository".
          ChainBuilder<String> gId = startChain();
          while (lastGroupId != null) {
            if (lastGroupId.getFileName() == null) {
              groupId = gId.join(".");
              X_Log.error(MvnServiceDefault.class, "Null filename on path ", lastGroupId, " using ", groupId, " for dependency ", dependency);
              break;
            } else {
                String last = lastGroupId.getFileName().toString();
                if ("repository".equals(last) || "repo".equals(last)) {
                  groupId = gId.join(".");
                  break;
                }
                gId.insert(lastGroupId.getFileName().toString());
            }
            lastGroupId = lastGroupId.getParent();
          }
        }
        // at long last, we've derived the groupId and artifactId for this dependency!
        final String artifactId = artifactPath.getFileName().toString();
        final StringTo<Model> locals = localProjects.out1();
        Model model = null;
        if (groupId != null) {
          // no groupId found, but there might be an artifactId-only mapping.
          model = locals.get(groupId + ":" + artifactId);
        }
        if (model == null) {
          model = locals.get(artifactId);
          if (model == null) {
            return null;
          }
        }
        return deriveSourcePaths(model);
      } catch (IOException e) {
        X_Log.warn(MvnServiceDefault.class, "Unable to examine dependency jar", dependency, e);
      }
    }
    return null;
  }

  protected ProjectSources deriveSourcePaths(Model model) throws IOException {
    // URLClassLoader expects all directories to end with a trailing /
    final In1Out1<String, String> slashify = path->
        Files.isDirectory(Paths.get(path))
            ? path.endsWith("/")
            ? path
          : path + "/"
        : path;

    ProjectSources project = X_Model.create(ProjectSources.class);

    final Properties props = model.getProperties();
    if (props != null && "true".equals(props.getProperty("xapi-paths"))) {
      grabPaths(project, props, "source", project::addSource);
      grabPaths(project, props, "resource", project::addResource);
      grabPaths(project, props, "output", project::addOutput);
    } else {
      String output = model.getBuild() == null ? null : model.getBuild().getOutputDirectory();
      if (output == null) {
        output = Paths.get(model.getProjectDirectory().toString(), "target/classes").toString();
      }
      output = slashify.io(output);
      Path outputPath = Paths.get(output);
      if (!Files.isDirectory(outputPath)) {
        try {
          Files.createDirectories(outputPath);
        } catch (IOException e) {
          X_Log.warn(ModelSerializerDefault.class, "Unable to create output directory", outputPath, e);
        }
      }
      project.addOutput(output);
    }

    String staging = cache.getProperty(model, "xapi.staging", ()->
        Paths.get(model.getProjectDirectory().toString(), "src/main/staging").toString()
    );
    project.setStaging(slashify.io(staging));

    final Filter1<String> filter = X_File::exists;

    final MappedIterable<String> sources = getSources(model).filter(filter).map(slashify);
    if (sources.isNotEmpty()) {
      project.sources()
          .addAll(sources);
    }

    final MappedIterable<String> resources = getResources(model).filter(filter).map(slashify);
    if (resources.isNotEmpty()) {
      project.resources()
          .addAll(resources);
    }

    final MappedIterable<String> testSources = getTestSources(model).filter(filter).map(slashify);
    if (testSources.isNotEmpty()) {
      project.testSources()
          .addAll(testSources);
    }

    final MappedIterable<String> testResources = getTestResources(model).filter(filter).map(slashify);
    if (testResources.isNotEmpty()) {
      project.testResources()
          .addAll(testResources);
    }

    return project;
  }

    private void grabPaths(ProjectSources project, Properties props, String type, In1<String> addSource) {
        final String length = props.getProperty("xapi-" + type + "-length");
        if (length != null) {
            int len = Integer.parseInt(length);
            for (int i = 0; i < len; i++) {
                final String source = props.getProperty("xapi-" + type + "-" + i);
                addSource.in(source);
            }
        }
    }

    protected SizedIterable<String> getSources(Model model) throws IOException {
    // TODO: snoop on build-helper-maven-plugin ... should really use a live reactor for this.
    // See DefaultMaven.newRepositorySession for getting a real maven parsing of the root model.
    if (model.getBuild() == null || isEmpty(model.getBuild().getSourceDirectory())) {
      File defaultSources = new File(model.getProjectDirectory(), "./src/main/java");
      return singleItem(defaultSources.getCanonicalPath());
    }
    return getCanonical(model, model.getBuild().getSourceDirectory());
  }

  private SingletonIterator<String> getCanonical(Model model, String dir) throws IOException {
    File source = new File(dir);
    if (!source.isDirectory()) {
      File f = new File(model.getProjectDirectory(), dir);
      if (f.isDirectory()) {
        return singleItem(f.getCanonicalPath());
      }
      if (dir.contains("$")) {
        String resolved = dir;
        while (resolved.contains("$")) {
          String newProp = cache.getProperty(model, resolved);
          if (resolved.equals(newProp)) {
            break;
          }
          resolved = newProp;
        }
        source = new File(resolved);
        if (source.isDirectory()) {
          return singleItem(source.getCanonicalPath());
        }
        source = new File(model.getProjectDirectory(), resolved);
        if (source.isDirectory()) {
          return singleItem(source.getCanonicalPath());
        }
        throw new IllegalArgumentException("Unable to resolve " + dir + "; last tried: " + source);
      }
      return singleItem(f.getCanonicalPath());
    } else {
      return singleItem(source.getCanonicalPath());
    }
  }

  protected SizedIterable<String> getTestSources(Model model) throws IOException {
    if (model.getBuild() == null || isEmpty(model.getBuild().getTestSourceDirectory())) {
      File defaultSources = new File(model.getProjectDirectory(), "./src/test/java");
      return singleItem(defaultSources.getCanonicalPath());
    }
    return getCanonical(model, model.getBuild().getTestSourceDirectory());
  }

  protected SizedIterable<String> getResources(Model model) throws IOException {
    final List<Resource> resources;
    if (model.getBuild() == null || X_Jdk.isEmpty(
        (resources = model.getBuild().getResources())
    )) {
      File defaultResources = new File(model.getProjectDirectory(), "./src/main/resources");
      return singleItem(defaultResources.getCanonicalPath());
    }
    return SizedIterable.of(resources::size, resources)
          .map(Resource::getDirectory)
          .mapUnsafe(d->getCanonical(model, d).getItem())
        ; // TODO consider targetPath for super-sourced stuff?
  }

  protected SizedIterable<String> getTestResources(Model model) throws IOException {
    final List<Resource> testResources;
    if (model.getBuild() == null || X_Jdk.isEmpty(
        (testResources = model.getBuild().getTestResources())
    )) {
      File defaultResources = new File(model.getProjectDirectory(), "./src/test/resources");
      return singleItem(defaultResources.getCanonicalPath());
    }
    return SizedIterable.of(testResources::size, testResources)
            .map(Resource::getDirectory) // TODO consider targetPath for super-sourced stuff?
            .mapUnsafe(d->getCanonical(model, d).getItem());
  }

  @Override
  public Out1<MappedIterable<String>> downloadDependencies(MvnDependency dep) {

    final Lazy<MappedIterable<String>> request = Lazy.deferred1(()->{
        try {

          final LocalArtifactResult localArtifact = X_Maven.loadLocalArtifact(
              dep.getGroupId(),
              dep.getArtifactId(),
              dep.getClassifier(),
              dep.getPackaging(),
              dep.getVersion()
          );
          Artifact artifact = null;
          if (localArtifact.isAvailable()) {
            artifact = localArtifact.getRequest().getArtifact();
            // not sure why maven is lame and doesn't set this for us...
            artifact = artifact.setFile(localArtifact.getFile());
          }
          if (artifact == null){
            final ArtifactResult result = X_Maven.loadArtifact(
                dep.getGroupId(),
                dep.getArtifactId(),
                dep.getClassifier(),
                dep.getPackaging(),
                dep.getVersion()
            );
            if (result == null) {
                return EmptyIterator.none();
            }
            artifact = result.getArtifact();
          }
          return MappedIterable.mapped(loadDependencies(artifact, this::shouldLoad));
        } catch (Throwable t) {
            X_Log.error(MvnServiceDefault.class, "Error encountered downloading artifact ", dep, t);
            return EmptyIterator.none();
        }
    });
    // Start download of artifact info immediately, but do not block
    runLater(request.ignoreOut1().toRunnable());
    // Return a string output that will block on the lazy initializer
    return request;
  }

  protected boolean shouldLoad(Dependency check) {
    return
           !"test".equals(check.getScope())
        && !"system".equals(check.getScope())
        && !check.isOptional();
  }

  protected void runLater(Runnable runnable) {
    X_Time.runLater(runnable);
  }

  private void loadInto(Map<String, String> dependencies, Artifact artifact, Filter1<Dependency> filter) {
    String artifactString = toArtifactString(artifact);
    if (!dependencies.containsKey(artifactString)) {
      String fileLoc = artifact.getFile().getAbsolutePath();
      dependencies.put(artifactString, fileLoc);
      try (
          JarFile jar = new JarFile(artifact.getFile())
       ) {
        final ZipEntry pomEntry = jar.getEntry("META-INF/maven/" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/pom.xml");
        final Model pom;
        if (pomEntry != null) {
          // some jars, like javax.inject, do not package a pom inside the jar :-/
          String pomString = X_IO.toStringUtf8(jar.getInputStream(pomEntry));
          pom = loadPomString(pomString);
          loadDependencies(dependencies, jar, pom, filter);
        } else {
          pom = null;
        }
        // TODO: get the platform type into here as well, so we can select the correct runtime.
        // We can likely infer it from the artifact's embedded pom... later.
        final ZipEntry xapiEntry = jar.getEntry("META-INF/xapi/settings.xapi");
        if (xapiEntry != null) {
          String xapiString = X_IO.toStringUtf8(jar.getInputStream(xapiEntry));
          try {
            UiContainerExpr el = JavaParser.parseUiContainer(artifact.getFile().getAbsolutePath()+"!META-INF/xapi/settings.xapi", xapiString);
            // Pull any source / output paths, and make those available as well...

          } catch (ParseException e) {
            X_Log.error(MvnServiceDefault.class, "Invalid xapi source", xapiString, " from ", artifact.getFile(), "\n", e);
          }
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      } catch (XmlPullParserException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private String toArtifactString(Artifact artifact) {
    if (isEmpty(artifact.getExtension()) || "jar".equals(artifact.getExtension())) {
      if (isEmpty(artifact.getClassifier())) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
      } else {
        // classifier is the fifth coordinate type, which implicitly uses jar for extension / packaging type
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":jar:" + artifact.getClassifier() + ":" + artifact.getVersion();
      }
    } else {
      if (isEmpty(artifact.getClassifier())) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getExtension() + ":" + artifact.getVersion();
      } else {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getExtension() + ":" + artifact.getClassifier() + ":" + artifact.getVersion();
      }
    }
  }

  private void loadDependencies(Map<String, String> dependencies, JarFile jar, Model pom, Filter1<Dependency> filter) {
    final List<Dependency> deps = pom.getDependencies();
    deps.stream().forEach(dependency -> {
      if (filter.filter1(dependency)) {
        final ZipEntry pomEntry = jar.getEntry("META-INF/maven/" + cache.resolveProperty(pom, "${pom.groupId}") + "/" + dependency.getArtifactId() + "/pom.xml");
        if (pomEntry != null) {
          // If the pom of the dependency is in the jar, it is very likely a shaded jar that already contains the
          // rest of the contents of the dependency, so we should skip it.
          return;
        }
        if (dependency.getVersion() == null) {
          // dependency management was used somewhere along the way :-/
          if (pom.getDependencyManagement() != null) {
            for (Dependency dep : pom.getDependencyManagement().getDependencies()) {
              if (dep.getGroupId().equals(dependency.getGroupId()) && dep.getArtifactId().equals(dependency.getArtifactId())) {
                loadDependency(dependencies, pom, dep, filter);
                return;
              }
            }
          }
          // ugh.  We have to look up parent dependency chains.
          Parent parent = pom.getParent();
          IntTo<Out1Unsafe<Model>> stack = X_Collect.newList(Out1Unsafe.class);
          loadParent(stack, parent);
          String targetGroupId = cache.resolveProperty(pom, dependency.getGroupId());
          String targetArtifactId = cache.resolveProperty(pom, dependency.getArtifactId());
          String targetClassifier = cache.resolveProperty(pom, normalize(dependency.getClassifier()));
          while (stack.isNotEmpty()) {
            final Model parentPom = stack.pop().out1();
            if (parentPom.getDependencyManagement() != null) {
              int depIndex = 0;
              for (Dependency dep : parentPom.getDependencyManagement().getDependencies()) {
                if ("import".equals(dep.getScope())) {
                  // An imported parent dependency... gross.
                  // We'll push it onto the head of the search stack,
                  loadImportDependency(stack, parentPom, dep, depIndex++);
                  continue;
                }
                if (!targetGroupId.equals(cache.resolveProperty(parentPom, dep.getGroupId()))) {
                  continue;
                }
                if (!targetArtifactId.equals(cache.resolveProperty(parentPom, dep.getArtifactId()))) {
                  continue;
                }
                if (!targetClassifier.equals(cache.resolveProperty(parentPom, normalize(dep.getClassifier())))) {
                  continue;
                }
                loadDependency(dependencies, parentPom, dep, filter);
                return;
              }
            }

          }
        } else {
          loadDependency(dependencies, pom, dependency, filter);
        }
      }
    });

  }

  private void loadParent(IntTo<Out1Unsafe<Model>> stack, Parent parent) {
    if (parent == null) {
      return;
    }
    // parents we will add to the end of the lookup list
    stack.add(()->{
      final ArtifactResult parentArtifact = loadArtifact(
          parent.getGroupId(),
          parent.getArtifactId(),
          "",
          "pom",
          parent.getVersion()
      );
      final Model parentPom = parentArtifact == null ? null : loadPomFile(parentArtifact.getArtifact().getFile().getAbsolutePath());
      // If we have a parent, push a provider onto the search stack
      if (parentPom != null) {
        loadParent(stack, parentPom.getParent());
      }
      return parentPom;
    });
  }

  private void loadImportDependency(
      IntTo<Out1Unsafe<Model>> stack,
      Model parentPom,
      Dependency dep,
      int depIndex
  ) {
    if (dep == null) {
      return;
    }
    // TODO: handle non-pom dependencies
    if (!"pom".equals(dep.getType())) {
      throw new IllegalArgumentException("Cannot load non-pom dependency: " + dep);
    }
    if (!"import".equals(dep.getScope())) {
      throw new IllegalArgumentException("Cannot load non-import dependency: " + dep);
    }
    // We'll insert items at correct index, to ensure we lookup results in a delayed, depth-first, top-down manner.
    stack.insert(depIndex, ()->{
      final ArtifactResult parentArtifact = loadArtifact(
          resolveProperties(parentPom, dep.getGroupId()),
          resolveProperties(parentPom, dep.getArtifactId()),
          resolveProperties(parentPom, normalize(dep.getClassifier())),
          "pom",
          resolveProperties(parentPom, dep.getVersion())
      );
      final Model pom = parentArtifact == null ? null : loadPomFile(parentArtifact.getArtifact().getFile().getAbsolutePath());
      // we won't load the parent of an import, as that is not the correct semantics
      return pom;
    });
  }

  private String resolveProperties(Model model, String value) {
    return value.startsWith("$") ? cache.getProperty(model, value) : value;
  }

  private void loadDependency(Map<String, String> dependencies, Model pom, Dependency dependency, Filter1<Dependency> filter) {
    String artifactString = cache.toArtifactString(pom, dependency);
    if (!dependencies.containsKey(artifactString)) {
      final Artifact artifact = cache.loadArtifact(pom, dependency);
      loadInto(dependencies, artifact, filter);
    }
  }

  @Override
  public String mvnHome() {
    return System.getenv("M2_HOME");
  }

  @Override
  public String localRepo() {
    return session.out1().getLocalRepository().getBasedir().getAbsolutePath();
  }

  @Override
  public List<RemoteRepository> remoteRepos() {
    return Arrays.asList(new RemoteRepository
      .Builder("maven-central", "default", "http://repo1.maven.org/maven2/")
      .build()
    );
  }

  private final ResourceMap loaded = new ResourceMap();

  @Override
  public Iterable<Model> findPoms(final ClassLoader loader) {
    final Iterable<StringDataResource> poms = loaded.get(loader.hashCode(),
        loader).findResources("", POM_PATTERN);
    class Itr implements Iterator<Model> {
      Iterator<StringDataResource> iterator = poms.iterator();

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Model next() {
        StringDataResource next = iterator.next();
        try {
          return loadPomString(next.readAll());
        } catch (Exception e) {
          X_Log.error("Unable to load resouce " + next.getResourceName(), e);
          throw X_Util.rethrow(e);
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

    }
    return new Iterable<Model>() {
      @Override
      public Iterator<Model> iterator() {
        return new Itr();
      }
    };
  }

  public SizedIterable<String> getSearchGroups() {
    return searchGroups.out1();
  }

}
