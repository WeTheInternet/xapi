package xapi.javac.dev.plugin;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.iterate.ReverseIterable;
import xapi.fu.Out2;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.InjectionResolver;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.api.MagicMethodInjector;
import xapi.javac.dev.api.MethodMatcher;
import xapi.javac.dev.api.SourceTransformationService;
import xapi.javac.dev.search.MagicMethodFinder;
import xapi.log.X_Log;
import xapi.source.read.JavaModel.IsNamedType;
import xapi.util.X_Debug;

import static xapi.collect.X_Collect.newList;
import static xapi.collect.X_Collect.newStringMap;

import java.util.Optional;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/13/16.
 */
public class MagicMethodPlugin implements Plugin {

  private final IntTo<MethodMatcher<MagicMethodInjector>> matchers;
  private final StringTo<MagicMethodInjector> injectors;
  private final IntTo<InjectionResolver> changes;
  private JavacService service;

  public MagicMethodPlugin() {
    if ("true".equals(System.getProperty("xapi.no.javac.plugin"))) {
      matchers = null;
      changes = null;
      injectors = null;
      return;
     }
    matchers = newList(MethodMatcher.class);
    changes = newList(InjectionResolver.class);
    injectors = newStringMap(MagicMethodInjector.class);
  }

  @Override
  public String getName() {
    return "MagicMethodPlugin";
  }

  @Override
  public void init(JavacTask javacTask, String... strings) {
    if ("true".equals(System.getProperty("xapi.no.javac.plugin"))) {
      return;
    }
    service = JavacService.instanceFor(javacTask);
    service.remember(MagicMethodPlugin.class, this);
    service.readProperties((key, value)->{
      if (key.startsWith("MagicMethod")) {
        String[] bits = key.split("[|]");
        if (bits.length != 2) {
          X_Log.error(getClass(), "Malformed MagicMethod property; ",
              "Expected format is: MagicMethod|fully.qualified.ClassName.methodName ",
              "you may optionally include parameters with java binary types names as well: ",
              "ex: MagicMethod:java.lang.String.equals(Ljava/lang/Object;)",
              "You sent: ", key, "=", value);
          return;
        }
        int paramIndex = key.indexOf('(');
        if (paramIndex == -1) {
          // match all methods with this name.
          matchers.add(exe->{
            if (!nameMatch(exe, bits[1])) {
              return Optional.empty();
            }
            return Optional.of(this.createInjector(value));
          });
        } else {
          // match only methods with this name and parameters
          matchers.add(exe->{
            if (!nameAndParametersMatch(exe, bits[1])) {
              return Optional.empty();
            }
            return Optional.of(this.createInjector(value));
          });
        }
      }
    });

    javacTask.addTaskListener(new TaskListener() {
      @Override
      public void started(TaskEvent taskEvent) {
        if (taskEvent.getKind() == Kind.ANALYZE) {
          final CompilationUnitTree cup = taskEvent.getCompilationUnit();
          final MagicMethodFinder finder = new MagicMethodFinder(matchers.forEach(), service, cup);
          finder.visitTopLevel((JCCompilationUnit) cup);
          final IntTo<Out2<MethodInvocationTree, MagicMethodInjector>> matched = finder.getMatched();
          if (matched.isNotEmpty()) {
            SourceTransformationService editor = SourceTransformationService.instanceFrom(service);
            matched.readWhileTrue((index, vals) -> {
              final InjectionResolver resolver = editor.createInjectionResolver(cup);
              final MethodInvocationTree method = vals.out1();
              final MagicMethodInjector injector = vals.out2();
              injector.performInjection(cup, method, resolver);
              if (resolver.hasChanges()) {
                changes.add(resolver);
              }
              return true;
            });

          }
        }
      }

      @Override
      public void finished(TaskEvent taskEvent) {
        if (changes.isNotEmpty()) {
          // We have some source to transform!
          // For now, we will naively run the transforms in reverse order;
          // in the future, we will want to be able to apply multiple transformations steps,
          // such that one transformation which changes another will cause that change to be reprocessed.
          // In practice, this will likely be done by the magic method resolver, which could, upon finding an injection,
          // traverse up the scope to find enclosing injections, and mark them as deferred (requiring multiple transforms).
          for (InjectionResolver injectionResolver : ReverseIterable.reverse(changes.forEach())) {
            injectionResolver.commit();
          }
          changes.clear();
        }
      }
    });

  }

  private MagicMethodInjector createInjector(String typeName) {
    return injectors.getOrCreate(typeName, s->{
      Class cls = loadClass(typeName);
      MagicMethodInjector instance = (MagicMethodInjector) createInstance(cls);
      instance.init(service);
      return instance;
    });
  }

  private Object createInstance(Class cls) {
    return X_Inject.instance(cls);
  }

  private Class loadClass(String typeName) {
    try {
      return Class.forName(typeName);
    } catch (ClassNotFoundException e) {
      throw X_Debug.rethrow(e);
    }
  }

  private boolean nameAndParametersMatch(IsNamedType exe, String signature) {

    if (exe.getSimpleName().contentEquals(signature)) {

    }
    return false;
  }

  private boolean nameMatch(IsNamedType exe, String signature) {
    return exe.getQualifiedMemberName().equals(signature);
  }


}
