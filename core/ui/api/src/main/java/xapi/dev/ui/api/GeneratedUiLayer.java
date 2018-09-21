package xapi.dev.ui.api;

import com.github.javaparser.ast.TypeArguments;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.CanAddImports;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.*;
import xapi.fu.itr.MappedIterable;
import xapi.source.X_Source;
import xapi.source.read.JavaModel.IsTypeDefinition;
import xapi.ui.api.ElementBuilder;
import xapi.util.X_String;

import java.util.Collections;

import static xapi.fu.Lazy.deferAll;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public abstract class GeneratedUiLayer extends GeneratedJavaFile {

    public enum ImplLayer {
        Api, Super, Base, Impl, Mixin
    }

    private final ImplLayer layer;
    private final StringTo<String> coercions;

    protected Lazy<GeneratedUiModel> model = deferAll(
        GeneratedUiModel::new,
        this::getOwner,
        this::getPackageName,
        this::getNameForModel
    );

    public Maybe<GeneratedUiLayer> getSuperType() {
        return Maybe.nullable((GeneratedUiLayer) superType);
    }

    protected String getNameForModel() {
        return getTypeName(); // you may want to suffix your models.
    }

    private String nameNode, nameElement, nameElementBuilder, nameElementInjector, nameModel, nameBase, nameApi;
    private final StringTo<In1<GeneratedUiImplementation>> abstractMethods;
    private final StringTo<ReferenceType> localDefinitions;

    public GeneratedUiLayer(String pkg, String cls, ImplLayer layer, GeneratedUiComponent owner) {
        this(null, pkg, cls, layer, owner);
    }

    @SuppressWarnings("unchecked")
    public GeneratedUiLayer(
        GeneratedUiLayer superType,
        String pkg,
        String cls,
        ImplLayer layer,
        GeneratedUiComponent owner
    ) {
        super(owner, superType, pkg, cls);
        this.layer = layer;
        abstractMethods = X_Collect.newStringMap(In1.class);
        localDefinitions = X_Collect.newStringMap(ReferenceType.class);
        coercions = X_Collect.newStringMap(String.class);
    }

    public String getModelName() {
        return model.out1().getWrappedName();
    }
    public String getModelFieldName() {
        return X_String.firstCharToLowercase(getTypeName());
    }

    public String getModelNameQualified() {
        final String modelName = model.out1().getWrappedName();
        if (modelName.indexOf('.') == 0) {
            return X_Source.qualifiedName(getPackageName(), modelName);
        }
        return modelName;
    }

    public GeneratedUiModel getModel() {
        return model.out1();
    }

    protected abstract IsTypeDefinition definition();

    public void addLocalDefinition(String sysName, ReferenceType param) {
        localDefinitions.put(sysName, param);
    }

    public boolean hasLocalDefinition(String sysName) {
        return localDefinitions.containsKey(sysName);
    }

    @Override
    protected SourceBuilder<GeneratedJavaFile> createSource() {
        final SourceBuilder<GeneratedJavaFile> source = super.createSource();
        final IsTypeDefinition definition = definition();
        if (definition != null) {
            source.setClassDefinition(definition.toDefinition(), false);
            source.setPackage(definition.getPackage());
        }
        return source;
    }

    public boolean hasModel() {
        return model.isResolved();
    }

    public String getNodeType(UiNamespace namespace) {
        if (nameNode == null) {
            nameNode = reserveName(UiNamespace.TYPE_NODE,
                "N", "_Node", "_N"
            );
        }
        return nameNode;
    }

    public String getElementType(UiNamespace namespace) {
        if (nameElement == null) {
            nameElement = reserveName(UiNamespace.TYPE_ELEMENT,
                // Some backups to use in source...  we will reserve these if you imported a type named `El`
                "E", "Ele", "Element"
            );
        }
        return nameElement;
    }

    public void setElementType(String type) {
        this.nameElement = type;
    }

    public String getModelType(UiNamespace namespace) {
        if (nameModel == null) {
            String modelName = getModelNameQualified();
            if (modelName == null) {
                Maybe<GeneratedUiLayer> search = getSuperType();
                while (modelName == null && search.isPresent()) {
                    modelName = search.get().getModelNameQualified();
                    search = search.get().getSuperType();
                }
            }
            assert modelName != null : "Did not have a model name present...";
            nameModel = getSource().addImport(modelName);
        }
        return nameModel;
    }

    public String getBaseType(UiNamespace namespace) {
        if (nameBase == null) {
            nameBase = reserveName(UiNamespace.TYPE_BASE,
                // Some backups to use in source...  we will reserve these if you imported a type named `El`
                "B", "BaseType"
            );
        }
        return nameBase;
    }

    public String getApiType(UiNamespace namespace) {
        if (nameApi == null) {
            nameApi = reserveName(UiNamespace.TYPE_API,
                // Some backups to use in source...  we will reserve these if you imported a type named `El`
                "A", "ApiType"
            );
        }
        return nameApi;
    }

    protected String reserveName(String sysName, String ... backups) {
        final String name = getSource().getImports().reserveSimpleName(
            X_Fu.concat(new String[]{sysName}, backups)
        );
        final GeneratedTypeParameter generic = getGenericInfo().getOrCreateGeneric(sysName);
        if (this.layer == ImplLayer.Api) {
            generic.setExposed(true);
        }
        generic.setLayerName(this.layer, name);
        return name;
    }

    public String getElementBuilderType(UiNamespace namespace) {
        if (nameElementBuilder == null) {
            if (this instanceof GeneratedUiApi || this instanceof GeneratedUiBase) {
                // When in api/base layer, we need to use generics instead of concrete types.
                nameElementBuilder = getSource().getImports().reserveSimpleName(
                    UiNamespace.TYPE_ELEMENT_BUILDER,
                    "ElementBuilder",
                    "Builder",
                    "EB"
                );
                String nodeBuilder = getSource().addImport(ElementBuilder.class);
                final String elementType = getElementType(namespace);
                final GeneratedUiGenericInfo generics = getOwner().getGenericInfo();
                final GeneratedTypeParameter generic = generics
                    .setLayerName(UiNamespace.TYPE_ELEMENT_BUILDER, layer, nameElementBuilder);

                final ClassOrInterfaceType builderType = new ClassOrInterfaceType(nodeBuilder);
                builderType.setTypeArguments(TypeArguments.withArguments(
                    Collections.singletonList(
                        new ClassOrInterfaceType(elementType)
                    )
                ));

                generic.absorb(new TypeParameter(nameElementBuilder, Collections.singletonList(builderType)));
            } else {
                nameElementBuilder = namespace.getElementBuilderType(getSource());
            }
        }
        // Use the concrete type
        return nameElementBuilder;
    }

    public boolean hasGenerics() {
        return getOwner().getGenericInfo().hasGenerics(layer);
    }

    public MappedIterable<String> getGenericNames() {
        return getGenericInfo().getTypeParameterNames(layer);
    }

    public MappedIterable<GeneratedTypeParameter> getTypeParameters() {
        return getGenericInfo().getTypeParameters(layer);
    }

    public boolean hasGeneric(String self) {
        return getGenericInfo().hasTypeParameter(layer, self);
    }

    protected GeneratedUiGenericInfo getGenericInfo() {
        return getOwner().getGenericInfo();
    }

    public String getTypeWithGenerics(ImplLayer forLayer, UiGeneratorService generator, UiNamespace namespace, CanAddImports out) {
        final MappedIterable<GeneratedTypeParameter> myParams = getGenericInfo().getTypeParameters(layer);
        return getWrappedName() + (
            myParams.isEmpty() ? "" :
                    myParams
                        .map(s->s.computeDeclaration(this, forLayer, generator, namespace, out))
                        .join("<", ", ", ">")
        );
    }

    public ImplLayer getLayer() {
        return layer;
    }

    public String getGenericValue(GeneratedTypeParameter param) {
        // Check if this layer has a type provider for the requested type param.
        final GeneratedTypeParameter newParam = getOwner().addGeneric(param.getSystemName(), param.getType());
        if (param.isExposed()) {
            newParam.setExposed(true);
        }
        return newParam.absorb(param.getType());
    }

    public ReferenceType getLocalDefinition(String systemName) {
        return localDefinitions.get(systemName);
    }

    public boolean hasCoercion(GeneratedUiMember field) {
        return coercions.has(field.getMemberType().toSource());
    }

    public void addCoercion(GeneratedUiMember field) {
        if (!hasCoercion(field)) {
            // create a coercion method.
            final ClassBuffer out = getSource().getClassBuffer();
            final String type = field.importType(out);
            final String rawType;
            int rawInd = type.indexOf('<');
            if (rawInd == -1) {
                rawType = type;
            } else {
                rawType = type.substring(0, rawInd);
            }
            // Check with the oracle to see if we can guess a type...
            // if it is a component, or a model created for a component,
            // then we will want to append (return) a new node builder,
            // whereas other items will either be .toString()d,
            // or unfolded if they are a container type.

        }
    }

    protected void prepareToSave(UiGeneratorTools<?> tools) {
    }

    public String getLayerName(String systemName) {
        for (GeneratedTypeParameter param : getGenericInfo().getTypeParameters(getLayer())) {
            if (systemName.equals(param.getSystemName())) {
                return param.getTypeName();
            }
        }
        throw new UnsupportedOperationException("No layer for " + systemName + " found in " + getLayer() + " : " + getGenericInfo().getTypeParameters(getLayer()));
    }
}
