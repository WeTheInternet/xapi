package xapi.lang.oracle;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.X_Fu;
import xapi.fu.itr.SizedIterable;
import xapi.lang.api.AstLayerType;
import xapi.source.X_Source;
import xapi.util.X_String;

import java.io.Serializable;

/**
 * An oracle for storing, retrieving and updating type information.
 *
 * This will allow for "whole world knowledge" during a compile.
 *
 * At this time, we are optimistically making this type Serializable,
 * though we will not depend on / require full serializability at this time.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 5/1/17.
 */
public class AstOracle<Extra> implements Serializable {

    private final StringTo<AstInfo<Extra>> allTypes;
    private final StringTo<AstInfo<Extra>> componentTypes;
    private final StringTo<AstInfo<Extra>> componentModelTypes;
    private final StringTo<AstInfo<Extra>> builderTypes;
    private final StringTo<AstInfo<Extra>> generatorTypes;
    private final StringTo<AstInfo<Extra>> modelTypes;

    public AstInfo<Extra> getOrCreate(Extra extra, String pkg, String ... enclosed) {
        String key = X_Source.qualifiedName(pkg, X_String.join(".", enclosed));
        return allTypes.getOrCreate(key, k->{
            if (enclosed.length < 2) {
                if (enclosed.length == 0) {
                    // just the pkg was used; likely a package-less TypeParam...
                    return new AstInfo<>(AstOracle.this, extra, "", pkg);
                } else {
                    return new AstInfo<>(AstOracle.this, extra, pkg, enclosed[0]);
                }
            }
            final String[] parent = X_Fu.slice(0, enclosed.length - 1, enclosed);
            final AstInfo<Extra> enclosing = getOrCreate(extra, pkg, parent);
            return new AstInfo<>(AstOracle.this, enclosing, extra, pkg, enclosed[enclosed.length-1]);
        });
    }

    @SuppressWarnings("unchecked")
    public AstOracle(){
        allTypes = X_Collect.newStringMap(AstInfo.class, X_Collect.MUTABLE_KEY_ORDERED);
        modelTypes = X_Collect.newStringMapInsertionOrdered(AstInfo.class);
        componentTypes = X_Collect.newStringMapInsertionOrdered(AstInfo.class);
        componentModelTypes = X_Collect.newStringMapInsertionOrdered(AstInfo.class);
        builderTypes = X_Collect.newStringMapInsertionOrdered(AstInfo.class);
        generatorTypes = X_Collect.newStringMapInsertionOrdered(AstInfo.class);
    }

    protected AstInfo<Extra> removeFromLayer(AstLayerType layer, AstInfo<Extra> info) {
        final AstInfo<Extra> removed;
        switch (layer) {
            case Component:
                removed = componentTypes.remove(info.getQualifiedName());
                break;
            case Model:
                removed = modelTypes.remove(info.getQualifiedName());
                break;
            case ComponentModel:
                removed = componentModelTypes.remove(info.getQualifiedName());
                break;
            case Builder:
                removed = builderTypes.remove(info.getQualifiedName());
                break;
            case Generator:
                removed = generatorTypes.remove(info.getQualifiedName());
                break;
            default:
                assert false : "Can't get here";
                throw new AssertionError();
        }
        if (removed != null) {
            assert removed == info : "Oracle appears corrupted; you are removing an object we don't own." +
                "\nwas: " + removed+"; you removed: " + info;
            return removed;
        }
        return info;
    }

    protected void addToLayer(AstLayerType layer, AstInfo<Extra> info) {
        switch (layer) {
            case Component:
                componentTypes.put(info.getQualifiedName(), info);
                return;
            case Model:
                modelTypes.put(info.getQualifiedName(), info);
                return;
            case ComponentModel:
                componentModelTypes.put(info.getQualifiedName(), info);
                return;
            case Builder:
                builderTypes.put(info.getQualifiedName(), info);
                break;
            case Generator:
                generatorTypes.put(info.getQualifiedName(), info);
                break;
            default:
                assert false : "Can't get here";
                throw new AssertionError();
        }
    }

    public SizedIterable<AstInfo<Extra>> getComponentTypes() {
        return componentTypes.forEachValue();
    }

    public SizedIterable<AstInfo<Extra>> getComponentModelTypes() {
        return componentModelTypes.forEachValue();
    }

    public SizedIterable<AstInfo<Extra>> getBuilderTypes() {
        return builderTypes.forEachValue();
    }

    public SizedIterable<AstInfo<Extra>> getGeneratorTypes() {
        return generatorTypes.forEachValue();
    }

    public SizedIterable<AstInfo<Extra>> getModelTypes() {
        return modelTypes.forEachValue();
    }
}
