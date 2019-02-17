package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.require.api.ArchiveGraph;
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency;
import org.gradle.api.internal.file.FileCollectionInternal;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2/17/19 @ 5:06 AM.
 */
public class XapiModuleDependency extends DefaultSelfResolvingDependency {

    private final ArchiveGraph module;

    public XapiModuleDependency(ArchiveGraph module, String name, FileCollectionInternal lazyFiles) {
        super(module.getComponentId(name), lazyFiles);
        this.module = module;
    }

    public String getPath() {
        return module.getView().getPath();
    }

    public ArchiveGraph getModule() {
        return module;
    }
}
