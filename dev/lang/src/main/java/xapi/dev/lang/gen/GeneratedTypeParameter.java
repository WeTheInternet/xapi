package xapi.dev.lang.gen;

import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import xapi.string.X_String;

import java.util.EnumMap;
import java.util.Objects;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 4/13/17.
 */
public class GeneratedTypeParameter {
    private final String systemName;
    private final TypeParameter type;
    private final EnumMap<SourceLayer, String> implNames;

    private boolean exposed;

    public GeneratedTypeParameter(String systemName, TypeParameter type) {
        this.systemName = systemName;
        if (type == null) {
            this.type = new TypeParameter();
            this.type.setName(systemName);
        } else {
            this.type = type;
            if (X_String.isEmpty(type.getName())) {
                type.setName(systemName);
            }
        }
        implNames = new EnumMap<>(SourceLayer.class);
    }

    protected EnumMap<SourceLayer, String> getImplNames() {
        return implNames;
    }

    public boolean isExposed() {
        return exposed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof GeneratedTypeParameter))
            return false;
        final GeneratedTypeParameter that = (GeneratedTypeParameter) o;
        // We purposely only perform equality on system name.
        // This is so you can find the correct key to replace with amended children
        return Objects.equals(systemName, that.systemName);
    }

    @Override
    public int hashCode() {
        return systemName != null ? systemName.hashCode() : 0;
    }

    public String absorb(TypeParameter type) {
        // TODO Only ever allow greater specificity in updates,
        // so it is legal to make bounds tighter, but never looser
        // Legal: Type<T> -> Type<T extends List>
        // Legal: Type<T> -> Type<T, V>
        // Illegal: Type<T extends List> -> Type<T>
        // TODO: consider whether it should be legal to remove types.  Probably not,
        // since isolated modules would want to add only names they know about,
        // so, absorbing a type parameter should never accidentally erase a type requested elsewhere.
        this.type.setTypeBound(type.getTypeBound());
        this.type.setName(type.getName());
        return type.getName();
    }

    public void append(ClassOrInterfaceType type) {
        this.type.getTypeBound().add(type);
    }

    public GeneratedTypeParameter setExposed(boolean exposed) {
        this.exposed = exposed;
        return this;
    }

    public GeneratedTypeParameter setLayerName(SourceLayer layer, String nameElement) {
        implNames.put(layer, nameElement);
        return this;
    }

    public String getTypeName() {
        return type.getName();
    }

    public String getSystemName() {
        return systemName;
    }

    public TypeParameter getType() {
        return type;
    }

    @Override
    public String toString() {
        return "GeneratedTypeParameter{" +
            "systemName='" + systemName + '\'' +
            ", type=" + type +
            ", implNames=" + implNames +
            ", exposed=" + exposed +
            '}';
    }

    public ReferenceType getReferenceType() {
        return new ReferenceType(type.getName());
    }
}
