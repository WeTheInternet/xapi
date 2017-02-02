package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.TypeExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.Do;
import xapi.fu.MappedIterable;
import xapi.fu.Maybe;
import xapi.fu.Out2;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/29/17.
 */
public class GeneratedUiGenericInfo {

    /**
     * Keyed by system $Name to the TypeExpr of the generic.
     *
     * This allows us to correctly override super types
     * (by asking the super type which system $Names it expected,
     * then supplying those in the implements / extends declaration).
     */
    private final StringTo<TypeExpr> inputTypes;

    public GeneratedUiGenericInfo() {
        inputTypes = X_Collect.newStringMapInsertionOrdered(TypeExpr.class);
    }

    public Maybe<TypeExpr> getGeneric(String systemName) {
        final String key = cleanKey(systemName);
        final TypeExpr val = inputTypes.get(key);
        return Maybe.nullable(val);
    }

    public MappedIterable<Out2<String, TypeExpr>> allGenerics() {
        return inputTypes.forEachItem();
    }

    public Do addGeneric(String systemName, TypeExpr type) {
        final String key = cleanKey(systemName) ;
        final TypeExpr was = inputTypes.put(key, type);
        return ()-> {
            if (type == inputTypes.get(key)) {
                if (was == null) {
                    inputTypes.remove(key);
                } else {
                    inputTypes.put(key, was);
                }
            }
        };
    }

    private String cleanKey(String systemName) {
        return systemName.startsWith("$") ? systemName.substring(1) : systemName;
    }
}
