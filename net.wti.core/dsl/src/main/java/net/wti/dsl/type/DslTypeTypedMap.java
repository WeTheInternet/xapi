package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

///
/// DslTypeTypedMap:
///
/// Type descriptor for typedMap({ key: typeExpr, ... }).
///
/// Semantics:
///  - The *type definition* must not contain repeated keys.
///  - Field names must be valid `name` identifiers (Java identifiers),
///    since we intend to generate fields without extra normalization.
///  - Order of keys is preserved (LinkedHashMap).
///  - Runtime values for typedMap are an instruction-list and may repeat keys.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 15:52
public final class DslTypeTypedMap extends ImmutableDslType {

    private final Map<String, DslType> fieldTypes;

    public DslTypeTypedMap(
            final MappedIterable<Expression> sourceAst,
            final Map<String, ? extends DslType> fieldTypes
    ) {
        super(sourceAst);
        Objects.requireNonNull(fieldTypes, "fieldTypes must not be null");

        final LinkedHashMap<String, DslType> copy = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends DslType> entry : fieldTypes.entrySet()) {
            final String key = Objects.requireNonNull(entry.getKey(), "typedMap field name must not be null");
            requireName(key);
            final DslType value = Objects.requireNonNull(entry.getValue(), "typedMap field type must not be null: " + key);
            if (copy.containsKey(key)) {
                throw new IllegalArgumentException("typedMap field name is duplicated in schema: " + key);
            }
            copy.put(key, value);
        }
        this.fieldTypes = Collections.unmodifiableMap(copy);
    }

    public Map<String, DslType> getFieldTypes() {
        return fieldTypes;
    }

    public boolean hasField(String fieldName) {
        return fieldTypes.containsKey(fieldName);
    }

    public DslType getFieldType(String fieldName) {
        return fieldTypes.get(fieldName);
    }

    private static void requireName(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("typedMap field name must not be empty");
        }
        if (!Character.isJavaIdentifierStart(key.charAt(0))) {
            throw new IllegalArgumentException("typedMap field name is not a valid name (bad start char): " + key);
        }
        for (int i = 1; i < key.length(); i++) {
            if (!Character.isJavaIdentifierPart(key.charAt(i))) {
                throw new IllegalArgumentException("typedMap field name is not a valid name (bad char): " + key);
            }
        }
    }
}
