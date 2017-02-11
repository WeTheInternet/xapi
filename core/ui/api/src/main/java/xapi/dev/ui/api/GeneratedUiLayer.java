package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.type.Type;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.CanAddImports;
import xapi.dev.source.SourceBuilder;
import xapi.fu.In1;
import xapi.fu.In2Out1;
import xapi.fu.Lazy;
import xapi.fu.MappedIterable;
import xapi.fu.Maybe;
import xapi.source.read.JavaModel.IsTypeDefinition;
import xapi.ui.api.NodeBuilder;

import static xapi.fu.Lazy.deferAll;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public abstract class GeneratedUiLayer extends GeneratedJavaFile {
    protected Lazy<GeneratedUiModel> model = deferAll(
        GeneratedUiModel::new,
        this::getPackageName,
        this::getNameForModel
    );

    public Maybe<GeneratedUiLayer> getSuperType() {
        return Maybe.nullable((GeneratedUiLayer) superType);
    }

    protected String getNameForModel() {
        return getTypeName(); // you may want to suffix your models.
    }

    private String nameElement, nameElementBuilder, nameStyleService, nameStyleElement;
    private final StringTo<In2Out1<UiNamespace, CanAddImports, String>> generics;
    private final GeneratedUiGenericInfo genericInfo;
    private final StringTo<In1<GeneratedUiImplementation>> abstractMethods;

    public GeneratedUiLayer(String pkg, String cls) {
        this(null, pkg, cls);
    }

    @SuppressWarnings("unchecked")
    public GeneratedUiLayer(GeneratedUiLayer superType, String pkg, String cls) {
        super(superType, pkg, cls);
        generics = X_Collect.newStringMapInsertionOrdered(In2Out1.class);
        genericInfo = new GeneratedUiGenericInfo();
        abstractMethods = X_Collect.newStringMap(In1.class);
    }

    public String getModelName() {
        return model.out1().getWrappedName();
    }

    public GeneratedUiModel getModel() {
        return model.out1();
    }

    protected abstract IsTypeDefinition definition();

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

    public String getElementType(UiNamespace namespace) {
        if (nameElement == null) {
            if (this instanceof GeneratedUiApi || this instanceof GeneratedUiBase) {
                // When in api/base layer, we need to use generics instead of concrete types.
                nameElement = getSource().getImports().reserveSimpleName("Element", "E", "El", "Ele");
                generics.put(nameElement, UiNamespace::getElementType);
            } else {
                nameElement = namespace.getElementType(getSource());
            }
            Type type = null;
            final TypeExpr expr = new TypeExpr(type);
            genericInfo.addGeneric(nameElement, expr);
        }
        // Use the concrete type
        return nameElement;
    }

    public String getElementBuilderType(UiNamespace namespace) {
        if (nameElementBuilder == null) {
            if (this instanceof GeneratedUiApi || this instanceof GeneratedUiBase) {
                // When in api/base layer, we need to use generics instead of concrete types.
                nameElementBuilder = getSource().getImports().reserveSimpleName(
                    "ElementBuilder",
                    "Builder",
                    "EB",
                    "ElBuilder"
                );
                String nodeBuilder = getSource().addImport(NodeBuilder.class);
                final String elementType = getElementType(namespace);
                generics.put(elementType, UiNamespace::getElementType);
                generics.put(
                    nameElementBuilder + " extends " + nodeBuilder + "<" + elementType + ">",
                    UiNamespace::getElementBuilderType
                );
            } else {
                nameElementBuilder = namespace.getElementBuilderType(getSource());
            }
        }
        // Use the concrete type
        return nameElementBuilder;
    }

    public boolean hasGenerics() {
        return !generics.isEmpty();
    }

    public MappedIterable<String> getGenerics() {
        return generics.mappedKeys();
    }

    public StringTo<In2Out1<UiNamespace, CanAddImports, String>> getGenericsMap() {
        return generics;
    }
}
