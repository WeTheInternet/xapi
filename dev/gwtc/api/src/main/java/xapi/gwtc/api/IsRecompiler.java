package xapi.gwtc.api;

import xapi.dev.gwtc.api.GwtcJobState;
import xapi.fu.In1;
import xapi.fu.In2;

import java.net.URL;

public interface IsRecompiler {

  void onCompileReady(In2<GwtcJobState, Throwable> callback);

  URL getResource(String name);

  CompiledDirectory recompile();

  CompiledDirectory getOrCompile();

}
