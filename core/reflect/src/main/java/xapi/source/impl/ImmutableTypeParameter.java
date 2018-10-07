package xapi.source.impl;

import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.EmptyIterator;
import xapi.fu.itr.SizedIterable;
import xapi.source.api.HasQualifiedName;
import xapi.source.api.IsAnnotation;
import xapi.source.api.IsParameterizedType;
import xapi.source.api.IsTypeParameter;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 3:33 AM.
 */
public class ImmutableTypeParameter implements IsTypeParameter {
    private final String name;
    private final SizedIterable<IsAnnotation> annotations;
    private final SizedIterable<IsParameterizedType> bounds;

    public ImmutableTypeParameter(
        String name,
        IsParameterizedType... bounds
    ) {
        this(name, EmptyIterator.none(), bounds);
    }

    public ImmutableTypeParameter(
        String name,
        SizedIterable<IsAnnotation> annotations,
        IsParameterizedType... bounds
    ) {
        this.name = name;
        this.annotations = annotations;
        this.bounds = ArrayIterable.iterate(bounds);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SizedIterable<IsParameterizedType> getBounds() {
        return bounds;
    }

    @Override
    public SizedIterable<IsAnnotation> getAnnotations() {
        return annotations;
    }

    @Override
    public IsAnnotation getAnnotation(String name) {
        return annotations
            .filterMapped(
                name.contains(".") ? HasQualifiedName::getQualifiedName : HasQualifiedName::getSimpleName,
                name::equals)
            .firstOrNull();
    }
}
