package xapi.dev.api;

import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.fu.Maybe;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;
import xapi.source.X_Source;
import xapi.util.X_String;

/**
 * Represents a logical entity that might own any given generated file.
 *
 * For now, this will either be a ui component, or a generated api;
 * ({@link GeneratedUiComponent} or {@link GeneratedApi}, respectively).
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/2/18 @ 1:23 AM.
 */
@SuppressWarnings("JavadocReference")
public interface GeneratedTypeOwner {

    String getPackageName();

    String getTypeName();

    UiContainerExpr getAst();

    default boolean isUiComponent() {
        return getAst().getName().startsWith("define-tag");
    }

    GeneratedTypeWithModel getApi();

    GeneratedTypeWithModel getBase();

    GeneratedJavaFile addExtraLayer(String id, GeneratedJavaFile file);

    MappedIterable<? extends GeneratedTypeWithModel> getImpls();

    SizedIterable<String> getRecommendedImports();

    GeneratedUiGenericInfo getGenericInfo();

    default String getQualifiedName() {
        return X_Source.qualifiedName(getPackageName(), getTypeName());
    }

    default GeneratedTypeParameter addGeneric(String genericName, TypeParameter type) {
        final GeneratedTypeParameter param = getGenericInfo().setLayerName(genericName, SourceLayer.Api, type.getName());
        param.absorb(type);
        return param;
    }

    GeneratedJavaFile getOrCreateExtraLayer(String id, String pkg, String cls);

    default Maybe<GeneratedUiMember> getModelField(String name) {
        if (hasPublicModel()) {
            return Maybe.nullable(getPublicModel().getField(name));
        }
        return Maybe.not();
    }

    default GeneratedUiMember getPublicField(String name) {
        name = name.replaceFirst("(is|get|set|has|add|remove|clear)([A-Z][a-zA-Z0-9]*)", "$2");
        name = X_String.firstCharToLowercase(name);
        return getPublicModel().getField(name);
    }

    default boolean hasPublicModel() {
        return getApi().hasModel();
    }

    default boolean hasPrivateModel() {
        return getBase().isModelResolved();
    }

    default GeneratedUiModel getPublicModel() {
        return getApi().getModel();
    }

    default GeneratedUiModel getPrivateModel() {
        return getBase().getModel();
    }


}
