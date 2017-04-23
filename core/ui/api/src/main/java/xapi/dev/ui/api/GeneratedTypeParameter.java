package xapi.dev.ui.api;

import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import xapi.dev.source.CanAddImports;
import xapi.dev.ui.api.GeneratedUiLayer.ImplLayer;
import xapi.fu.MappedIterable;
import xapi.util.X_String;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 4/13/17.
 */
public class GeneratedTypeParameter {
    private final String systemName;
    private final TypeParameter type;
    private final EnumMap<ImplLayer, String> implNames;

    private boolean exposed;
    private boolean addOwnTypeNames;

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
        implNames = new EnumMap<>(ImplLayer.class);
    }

    protected EnumMap<ImplLayer, String> getImplNames() {
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

    public String computeDeclaration(
        GeneratedUiLayer layer,
        ImplLayer forLayer,
        UiGeneratorService generator,
        UiNamespace namespace,
        CanAddImports out
    ) {
        final List<ClassOrInterfaceType> bounds;
        if (forLayer == ImplLayer.Impl) {
            bounds = Collections.emptyList();
        } else {
            bounds = new ArrayList<>(type.getTypeBound());
        }
        String name;
        if (forLayer == ImplLayer.Impl) {
            name = namespace.loadFromNamespace(systemName, out)
                     .ifAbsentSupply(type::getName);
        } else {
            name = type.getName();
        }
        if (UiNamespace.TYPE_SELF.equals(name)) {
            // When requesting a self type, we want to add the known bounds of the given layer.
            final MappedIterable<String> names = layer.getGenericNames();
            if (!names.isEmpty()) {
                names
                    .map(ClassOrInterfaceType::new)
                    .forAll(bounds::add);
            }
        }
        if (bounds.isEmpty()) {
            return name;
        }
        final String baseName = namespace.loadFromNamespace(systemName, out)
            .ifAbsentReturn(name);
        StringBuilder b = new StringBuilder(baseName);
        String spacer = bounds.size() > 2 ? ",\n" : ", ";
        String toAdd = "<";
        for (ClassOrInterfaceType param : bounds) {
            b.append(toAdd);
            toAdd = spacer;
            // TODO: require UiGeneratorTools so we can normalize any magic strings in our name
            String source = param.toSource();
            if (generator != null) {
//                generator.tools().resolveString()
            }
            b.append(source);
        }
        if (!"<".equals(toAdd)) {
            b.append(">");
        }

        return b.toString();
    }

    public GeneratedTypeParameter setLayerName(ImplLayer layer, String nameElement) {
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

    public GeneratedTypeParameter setAddOwnTypeNames() {
        addOwnTypeNames = true;
        return this;
    }
}
