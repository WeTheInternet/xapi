package xapi.dev.source;

import xapi.annotation.process.OnThreadDeath;
import xapi.inject.X_Inject;
import xapi.source.service.SourceService;

/**
 *
 * In order to avoid wasted cpu cycles, we keep a threadlocal cache of
 * known Immutable Types; this allows us to use identity semantics instead
 * of performing deep equals checks.
 *
 * All we have to do is call SourceContext.gc() when we know we are done.
 *
 */

public class SourceContext {

  protected SourceContext() {// injectable, but not public.
    //TODO register this class for thread-local cleanup...
  }

  private static final class LocalContext extends ThreadLocal<SourceContext> {
    @Override
    protected SourceContext initialValue() {
      return X_Inject.instance(SourceContext.class);
    }
  }

  private static final LocalContext context = new LocalContext();

  public static SourceContext getContext() {
    return context.get();
  }



  @OnThreadDeath
  public static void gc() {
    context.remove();
  }



  public SourceService getService() {
    // Default action is to use an application-wide source service.
    return X_Inject.singleton(SourceService.class);
  }


}
