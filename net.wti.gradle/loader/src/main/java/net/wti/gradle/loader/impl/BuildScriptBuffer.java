package net.wti.gradle.loader.impl;

import xapi.dev.source.CharBuffer;
import xapi.fu.Lazy;
import xapi.fu.data.MapLike;
import xapi.fu.data.SetLike;
import xapi.fu.java.X_Jdk;

/**
 * BuildScriptBuffer:
 * <p>
 * <p>
 * <p> A special case of {@link GradleBuffer} which exposes buildscript{} and plugins{} blocks.
 * <p>
 * <p> TODO: special class for plugins {}, since it only allows `id 'blah'` children.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 21/03/2021 @ 3:27 a.m..
 */
public class BuildScriptBuffer extends GradleBuffer {

    private final Lazy<ClosureBuffer> buildscript;
    private final Lazy<ClosureBuffer> plugins;
    private final SetLike<String> pluginList;

    public BuildScriptBuffer() {
        final CharBuffer top = new CharBuffer();
        final CharBuffer bottom = new CharBuffer();
        buffer.addToBeginning(bottom);
        buffer.addToBeginning(top);

        plugins = Lazy.deferred1(()-> startClosure(bottom).setName("plugins"));
        buildscript = Lazy.deferred1(()-> startClosure(top).setName("buildscript"));
        pluginList = X_Jdk.set();
    }

    public ClosureBuffer getBuildscript() {
        return buildscript.out1();
    }

    public ClosureBuffer getPlugins() {
        return plugins.out1();
    }

    public boolean hasBuildscript() {
        return buildscript.isFull1() && buildscript.out1().buffer.isNotEmpty();
    }

    public boolean hasPlugins() {
        return plugins.isFull1() && plugins.out1().buffer.isNotEmpty();
    }

    public BuildScriptBuffer addPlugin(String pluginId) {
        String key = pluginId.trim();

        boolean hasId = false, hasQuote = false;
        if (key.startsWith("id ") || key.startsWith("id\t")) {
            hasId = true;
            key = key.replaceFirst("id\\s*", "");
        }
        if (key.startsWith("'") || key.startsWith("\"")) {
            hasQuote = true;
            key = key.replaceAll("[\"']", "");
        }
        if (pluginList.addIfMissing(key)) {
            final ClosureBuffer out = getPlugins();
            out.println().indent();
            if (!hasId) {
                out.append("id\t");
            }
            if (!hasQuote) {
                out.append("\"");
            }
            out.append(pluginId);
            if (!hasQuote) {
                out.append("\"");
            }
            out.outdent();
        }
        return this;
    }
}
