package xapi.gwtc.api;

import xapi.fu.Do;

import java.net.URL;

public interface IsRecompiler {

  URL getResource(String name);

  CompiledDirectory recompile();

  CompiledDirectory getOrCompile();

  void checkFreshness(Do ifFresh, Do ifStale);
}
