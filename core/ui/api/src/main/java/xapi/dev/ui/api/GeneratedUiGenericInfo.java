package xapi.dev.ui.api;

import com.github.javaparser.ast.TypeParameter;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.ui.api.GeneratedUiLayer.ImplLayer;
import xapi.fu.MappedIterable;
import xapi.fu.Maybe;

import java.util.EnumMap;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/29/17.
 */
public class GeneratedUiGenericInfo {

    public GeneratedTypeParameter setLayerName(String sysName, ImplLayer layer, String nameElement) {
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

    public boolean hasGenerics(ImplLayer layer) {
        return inputTypes.forEachValue()
                    .map(GeneratedTypeParameter::getImplNames)
                    .anyMatch(EnumMap::containsKey, layer);
    }

    public MappedIterable<String> getTypeParameterNames(ImplLayer layer) {
        return inputTypes.forEachValue()
            .filter(p -> p.getImplNames().containsKey(layer))
            .map(GeneratedTypeParameter::getTypeName)
            ;
    }

    public MappedIterable<GeneratedTypeParameter> getTypeParameters(ImplLayer layer) {
        return inputTypes.forEachValue()
            .filter(p -> p.getImplNames().containsKey(layer));
    }

    public MappedIterable<String> getSystemNames(ImplLayer layer) {
        return inputTypes.forEachValue()
                         .filter(p->p.getImplNames().containsKey(layer))
                         .map(GeneratedTypeParameter::getSystemName)
            ;
    }

    public boolean hasTypeParameter(ImplLayer layer, String systemName) {
        return inputTypes.getMaybe(systemName)
                   .mapNullSafe(GeneratedTypeParameter::getImplNames)
                   .mapNullSafe(EnumMap::containsKey, layer)
                   .isPresent();
    }

    public String getLayerName(ImplLayer base, String name) {
        return inputTypes.getMaybe(name)
                         .mapNullSafe(GeneratedTypeParameter::getImplNames)
                         .mapNullSafe(EnumMap::get, base)
                         .ifAbsentReturn(null);
    }
}
