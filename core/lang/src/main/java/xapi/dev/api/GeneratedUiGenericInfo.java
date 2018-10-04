package xapi.dev.api;

import com.github.javaparser.ast.TypeParameter;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.Maybe;
import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.MappedIterable;

import java.util.EnumMap;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/29/17.
 */
public class GeneratedUiGenericInfo {

    public GeneratedTypeParameter setLayerName(String sysName, SourceLayer layer, String nameElement) {
        return getOrCreateGeneric(sysName)
            .setLayerName(layer, nameElement);
    }

    /**
     * Keyed by system $Name to the TypeExpr of the generic.
     *
     * This allows us to correctly override super types
     * (by asking the super type which system $Names it expected,
     * then supplying those in the implements / extends declaration).
     */
    private final StringTo<GeneratedTypeParameter> inputTypes;

    public GeneratedUiGenericInfo() {
        inputTypes = X_Collect.newStringMapInsertionOrdered(GeneratedTypeParameter.class);
    }

    public GeneratedTypeParameter getOrCreateGeneric(String systemName) {
        final String key = cleanKey(systemName);
        return inputTypes.getOrCreate(key, ignored->
            // TODO: consider using the cleaned key here as well?
            new GeneratedTypeParameter(systemName, null)
        );
    }
    public Maybe<GeneratedTypeParameter> getGeneric(String systemName) {
        final String key = cleanKey(systemName);
        final GeneratedTypeParameter val = inputTypes.get(key);
        return Maybe.nullable(val);
    }

    public MappedIterable<GeneratedTypeParameter> allGenerics() {
        return inputTypes.forEachValue();
    }

    public GeneratedTypeParameter addGeneric(String systemName, TypeParameter type) {
        final String key = cleanKey(systemName) ;
        GeneratedTypeParameter param = inputTypes.get(key);
        if (param == null) {
            param = new GeneratedTypeParameter(systemName, type);
            inputTypes.put(key, param);
        } else {
            param.absorb(type);
        }

        return param;
    }

    private String cleanKey(String systemName) {
        return systemName.startsWith("$") ? systemName.substring(1) : systemName;
    }

    public boolean isEmpty() {
        return inputTypes.isEmpty();
    }

    public MappedIterable<GeneratedTypeParameter> forEachValue() {
        return inputTypes.forEachValue();
    }

    public boolean hasGenerics(SourceLayer layer) {
        return inputTypes.forEachValue()
                    .map(GeneratedTypeParameter::getImplNames)
                    .anyMatch(EnumMap::containsKey, layer);
    }

    public MappedIterable<String> getTypeParameterNames(SourceLayer layer) {
        return inputTypes.forEachValue()
            .filter(p -> p.getImplNames().containsKey(layer))
            .map(GeneratedTypeParameter::getTypeName)
            ;
    }

    public MappedIterable<GeneratedTypeParameter> getTypeParameters(SourceLayer layer) {
        return inputTypes.forEachValue()
            .filter(p -> p.getImplNames().containsKey(layer));
    }

    public MappedIterable<String> getSystemNames(SourceLayer layer) {
        return inputTypes.forEachValue()
                         .filter(p->p.getImplNames().containsKey(layer))
                         .map(GeneratedTypeParameter::getSystemName)
            ;
    }

    public boolean hasTypeParameter(SourceLayer layer, String systemName) {
        return inputTypes.getMaybe(systemName)
                   .mapNullSafe(GeneratedTypeParameter::getImplNames)
                   .mapNullSafe(EnumMap::containsKey, layer)
                   .isPresent();
    }

    public String getLayerName(SourceLayer base, String name) {
        return inputTypes.getMaybe(name)
                         .mapNullSafe(GeneratedTypeParameter::getImplNames)
                         .mapNullSafe(EnumMap::get, base)
                         .ifAbsentReturn(null);
    }

    public String findUnused(SourceLayer layer, boolean exposed, String ... opts) {
        nextOp:
        for (String opt : opts) {
            for (GeneratedTypeParameter param : getTypeParameters(layer)) {
                if (!exposed && !param.isExposed()) {
                    continue;
                }
                if (opt.equals(param.getTypeName())) {
                    continue nextOp;
                }
            }
            // We won!  TODO: consider reserving this name somehow that is not generated?
            return opt;
        }
        throw new IllegalArgumentException("All options taken " + ArrayIterable.iterate(opts) +
            " in " + getTypeParameters(layer).join(", "));
    }
}
