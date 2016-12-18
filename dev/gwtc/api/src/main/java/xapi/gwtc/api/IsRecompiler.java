package xapi.gwtc.api;

import com.google.gwt.dev.cfg.ResourceLoader;

public interface IsRecompiler {

  ResourceLoader getResourceLoader();

  CompiledDirectory recompile();

}
