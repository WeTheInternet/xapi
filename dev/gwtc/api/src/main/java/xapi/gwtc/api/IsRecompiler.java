package xapi.gwtc.api;

import java.net.URL;

public interface IsRecompiler {

  URL getResource(String name);

  CompiledDirectory recompile();

}
