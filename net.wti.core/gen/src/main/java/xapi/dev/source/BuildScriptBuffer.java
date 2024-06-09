package xapi.dev.source;

import xapi.dev.source.CharBuffer;
import xapi.fu.Lazy;
import xapi.fu.Printable;
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

    private final Lazy<Printable<?>> buildscript;
    private final Lazy<Printable<?>> plugins;
    private final SetLike<String> pluginList;

    private final boolean allowPluginsBlock;

    public BuildScriptBuffer() {
        this(true);
    }
    public BuildScriptBuffer(boolean allowPluginsBlock) {
        this.allowPluginsBlock = allowPluginsBlock;
        final CharBuffer top = new CharBuffer();
        final CharBuffer bottom = new CharBuffer();
        buffer.addToBeginning(bottom);
        buffer.addToBeginning(top);

        plugins = Lazy.deferred1(()-> {
            if (allowPluginsBlock) {
                final ClosureBuffer pluginsClosure = startClosure(bottom).setName("plugins");
                return pluginsClosure;
            }
            PrintBuffer buff = new PrintBuffer();
            bottom.addToEnd(buff);
            return buff;
        });
        buildscript = Lazy.deferred1(()-> {
            if (allowPluginsBlock) {
                return startClosure(top).setName("buildscript");
            }
            PrintBuffer buff = new PrintBuffer();
            top.addToEnd(buff);
            return buff;
        });
        pluginList = X_Jdk.set();
    }

    public Printable<?> getBuildscript() {
        return buildscript.out1();
    }

    public Printable<?> getPlugins() {
        return plugins.out1();
    }

    public boolean addPlugin(String pluginId) {
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
            final Printable<?> out = getPlugins();
            if (allowPluginsBlock) {
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
            } else {
                out.println()
                   .append("apply plugin: \"").append(pluginId).append("\"")
                   .println();
            }
            return true;
        }
        return false;
    }
}
