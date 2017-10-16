package com.google.gwt.dev.codeserver;

import xapi.fu.Do;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.IsRecompiler;

import java.net.URL;

/**
 * The ForeignRecompiler exists for use by a calling thread;
 * it will proxy all types across classloaders,
 * so we can talk safely to an isolated gwt classloader.
 *
 * An isolated classloader is needed to run different Gwt compilations in the same JVM,
 * with potentially different classpaths or gwt.xml module inherits.
 *
 * Due to some static maps and other classloader-bound issues,
 * it is not safe to let multiple Gwt compilations share parent classloaders.
 *
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/10/17.
 */
public class ForeignRecompiler implements IsRecompiler {

    private final Object foreign;

    public ForeignRecompiler(Object foreign) {
        this.foreign = foreign;
    }

    @Override
    public URL getResource(String name) {
        return null;
    }

    @Override
    public CompiledDirectory recompile() {
        return null;
    }

    @Override
    public CompiledDirectory getOrCompile() {
        return null;
    }

    @Override
    public void checkFreshness(Do ifFresh, Do ifStale) {

    }
}
