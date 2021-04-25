package net.wti.gradle.schema.index;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.schema.api.*;
import net.wti.gradle.schema.spi.SchemaProperties;
import xapi.fu.Do;
import xapi.fu.Lazy;
import xapi.fu.data.SetLike;
import xapi.fu.java.X_Jdk;
import xapi.gradle.fu.LazyString;
import xapi.string.X_String;

import java.io.File;

/**
 * IndexBackedSchemaMap:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 20/04/2021 @ 1:47 a.m..
 */
public class IndexBackedSchemaMap implements HasAllProjects  {
    private final SchemaCallbacks callbacks;
    private final SetLike<SchemaProject> allProjects;
    private final SchemaProperties properties;
    private final MinimalProjectView view;
    private final SchemaIndexReader reader;
    private volatile String group;
    private volatile String version;
    private Lazy<SchemaIndex> indexProvider;
    private volatile Do onResolved;

    public IndexBackedSchemaMap(final MinimalProjectView view, final SchemaCallbacks callbacks, final SchemaProperties properties, final Lazy<SchemaIndex> indexProvider) {
        this.callbacks = callbacks;
        this.properties = properties;
        this.view = view;
        this.allProjects = X_Jdk.setLinkedSynchronized();
        this.reader = properties.createReader(view, new LazyString(this::getVersion));
        this.indexProvider = indexProvider;
        onResolved = Do.NOTHING;
    }

    @Override
    public SchemaCallbacks getCallbacks() {
        return callbacks;
    }

    @Override
    public SetLike<SchemaProject> getAllProjects() {
        return allProjects;
    }

    @Override
    public String getGroup() {
        return X_String.isEmpty(group) ? QualifiedModule.UNKNOWN_VALUE : group;
    }

    @Override
    public void setGroup(final String group) {
        this.group = group;
    }

    @Override
    public void setVersion(final String version) {
        this.version = version;
    }

    @Override
    public String getVersion() {
        return X_String.isEmpty(version) ? QualifiedModule.UNKNOWN_VALUE : version;
    }

    @Override
    public Lazy<SchemaIndex> getIndexProvider() {
        return indexProvider;
    }

    @Override
    public MinimalProjectView getView() {
        return view;
    }

    @Override
    public File getRootSchemaFile() {
        return properties.getRootSchemaFile(getView());
    }

    @Override
    public synchronized void whenResolved(final Do job) {
        if (indexProvider.isResolved()) {
            job.done();
        } else {
            onResolved = onResolved.doAfter(job);
        }
    }

    @Override
    public void resolve() {
        getIndexProvider().out1();
    }
}
