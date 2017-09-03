package xapi.dev.gwtc.impl;

import xapi.dev.source.XmlBuffer;
import xapi.fu.MappedIterable;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;

import java.util.Collection;
import java.util.SortedSet;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.Shardable;

import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.dev.CompilePropertiesArtifact;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigurationProperty;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/6/17.
 */
@LinkerOrder(Order.POST)
@Shardable
public class MetaLinker extends AbstractLinker {
    @Override
    public String getDescription() {
        return "Emit a xapi.settings file containing information about the compilation (directories)";
    }

    @Override
    public ArtifactSet link(TreeLogger logger, LinkerContext context, ArtifactSet artifacts, boolean onePermutation)
    throws UnableToCompleteException {
        if (!onePermutation) {
            SortedSet<CompilePropertiesArtifact> compProps = artifacts.find(CompilePropertiesArtifact.class);
            if (compProps.isEmpty()) {
                assert false : "Cannot export compiler settings as gwt compiler has not exported a CompilePropertiesArtifact";
            } else {
                assert compProps.size() == 1 : "Only supporting a single global CompilePropertiesArtifact at this time";
                final CompilePropertiesArtifact props = compProps.first();
                XmlBuffer output = new XmlBuffer("gwt-props");

                output
                    .setAttributeIfNotNull("genDir", props.getGenDir())
                    .setAttributeIfNotNull("warDir", props.getWarDir())
                    .setAttributeIfNotNull("workDir", props.getWorkDir())
                    .setAttributeIfNotNull("deployDir", props.getDeployDir())
                    .setAttributeIfNotNull("extraDir", props.getExtraDir())
                    .setAttributeIfNotNull("sourceMapPrefix", props.getSourceMapPrefix())
                    // TODO: add module shortnames...
                    .setAttribute("modules", toJsonArray(props.getModuleNames()), false)
                    .setAttributeIfNotNull("jsNamespace", props.getJsNamespace().name())
                    ;
                if (props.getBindingProps() != null) {
                    StringBuilder values = new StringBuilder("{\n  ");
                    boolean first = true;
                    for (BindingProperty prop : props.getBindingProps()) {
                        ChainBuilder<String> opts = Chain.startChain();
                        for (SortedSet<String> strings : prop.getCollapsedValuesSets()) {
                            opts.add(toJsonArray(strings));
                        }
                        if (first) {
                            first = false;
                        } else {
                            values.append(",\n    ");
                        }
                        values.append(prop.getName()).append(": ")
                            .append(opts.join("[", ",", "]"));

                    }
                    values.append("\n  }");
                    output.setAttribute("bindingProps", values.toString(), false);
                }
                if (props.getConfigProps() != null) {
                    StringBuilder values = new StringBuilder("{\n  ");
                    for (ConfigurationProperty prop : props.getConfigProps()) {
                        if (prop.isMultiValued()) {
                            values.append(prop.getName()).append(": ")
                                .append(toJsonArray(prop.getValues()));
                        } else {
                            values.append(prop.getName()).append(": ")
                                .append(prop.getValue());
                        }
                    }
                    values.append("\n  }");
                    output.setAttribute("configProps", values.toString(), false);
                }

                final SyntheticArtifact result = emitString(
                    logger,
                    output.toSource(),
                    "props.xapi"
                );
                artifacts.add(result);
            }

        }
        return artifacts;
    }

    private String toJsonArray(Collection<String> moduleNames) {
        return MappedIterable.mapped(moduleNames).join("[\"", "\", \"", "\"]");
    }

}
