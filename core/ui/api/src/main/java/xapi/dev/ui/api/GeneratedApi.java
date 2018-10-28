package xapi.dev.ui.api;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.api.GeneratedUiGenericInfo;
import xapi.dev.api.GeneratedUiModel;
import xapi.dev.gen.SourceHelper;
import xapi.dev.ui.impl.AbstractUiGeneratorService;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.UiTagModelGenerator;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.Out1;
import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.source.read.JavaModel.IsQualified;
import xapi.util.X_String;

import java.util.Arrays;
import java.util.List;

import static com.github.javaparser.ast.visitor.ComposableXapiVisitor.slurpStrings;
import static xapi.fu.itr.ArrayIterable.iterate;
import static xapi.fu.itr.EmptyIterator.none;

/**
 * Represents a set of app-wide generated classes (models, for now),
 * plus metadata to influence the generation of components (like priming import resolutions,
 * so you don't have to @Import({}) a billion times for a well-named type, like MappedIterable.
 *
 * You may specify entire packages of types to prime code generators with,
 * but beware that this could add some latency to your generator
 * (this is generally negligible, but you have been warned)
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/2/18 @ 12:45 AM.
 */
public class GeneratedApi extends GeneratedTypeOwnerBase <GeneratedUiApi, GeneratedUiBase, GeneratedUiImplementation> {

    private final AbstractUiGeneratorService<?, ?> generator;
    private final SourceHelper<?> sources;

    public GeneratedApi(
        AbstractUiGeneratorService<?, ?> generator,
        SourceHelper<?> sources,
        IsQualified type,
        UiContainerExpr expr
    ) {
        super(type.getPackage(), type.getSimpleName(), Out1.immutable(expr), new GeneratedUiGenericInfo());
        this.generator = generator;
        this.sources = sources;
    }

    @Override
    public boolean isUiComponent() {
        return false;
    }

    // for now, we're just going to borrow these ui types;
    // when we move this layer into xapi-gen project, we'll separate out the minimal non-ui logic,
    // and put that into a new class with no / minimal cruft
    @Override
    protected GeneratedUiApi makeApi() {
        return new GeneratedUiApi(this, getPackageName(), getTypeName())
            .setSuffix("Api");
    }

    @Override
    protected GeneratedUiBase makeBase() {
        return new GeneratedUiBase(this, getApi());
    }

    public void addImports(UiAttrExpr attr) {
        for (String item : slurpStrings(GeneratedApi.class, attr.getExpression())) {
            String[] bits = item.split(",");
            addRecommendedImports(iterate(bits).map(String::trim));
        }
    }

    public void addModels(UiGeneratorTools tools, ApiGeneratorContext ctx, UiAttrExpr attr) {
        final List<AnnotationExpr> defaultAnnotations = attr.getAnnotations();
        ComposableXapiVisitor<?> visitor = ComposableXapiVisitor.whenMissingFail(GeneratedApi.class);
        visitor
            .withJsonContainerRecurse(In2.ignoreAll())
            .withJsonPairTerminal((json, arg)->{

                SizedIterable<AnnotationExpr> annos = computeAnnotations(json.getAnnotations()::iterator, defaultAnnotations::iterator);
                UiAttrExpr jsonAsAttr = new UiAttrExpr(json.getKeyString(), json.getValueExpr());
                jsonAsAttr.setAnnotations(Arrays.asList(annos.toArray(AnnotationExpr.class)));
                final String modName = X_String.toTitleCase(json.getKeyString());
                final GeneratedUiModel model = new GeneratedUiModel(this, getPackageName(), modName);
                UiTagModelGenerator.generateModel(tools, ctx, this, model, jsonAsAttr, true);
                X_Log.info("Generated Api model: ", X_Source.pathToLogLink(getPackageName(), model.getWrappedName()));
            });
        attr.getExpression().accept(visitor, null);

    }

    private SizedIterable<AnnotationExpr> computeAnnotations(MappedIterable<AnnotationExpr> annotations, MappedIterable<AnnotationExpr> defaultAnnotations) {
        StringTo<AnnotationExpr> annos = X_Collect.newStringMapInsertionOrdered(AnnotationExpr.class);

        boolean nullable = false, notNullable = false;
        for (AnnotationExpr anno : defaultAnnotations.plus(annotations)) {
            final String key = anno.getNameString().toLowerCase();
            annos.put(key, anno);
            switch (key) {
                // last one wins...
                case "null": // why would you even?  ...but, @NotNull makes sense, so we'll accept @Null too.
                case "nullable":
                    nullable = true;
                    notNullable = false;
                    break;
                case "notnull":
                case "notnullable":
                    notNullable = true;
                    nullable = false;
                    break;
            }
        }
        if (nullable) {
            annos.remove("notnull");
            annos.remove("notnullable");
        } else if (notNullable) {
            annos.remove("nullable");
            annos.remove("null");
        }

        return annos.forEachValue();
    }

    public void addMigrations(UiAttrExpr attr) {
    }

    /**
     * Allow each generated api to see each generated component.
     *
     * This is a good time to add per-type convenience methods to master api.
     *
     * These convenience methods should each be a mixin that we inherit
     * (so you can also implement them differently, and pass around w/out
     * having to needlessly reference / require the master api in method parameters)
     *
     */
    public void spyComponent(GeneratedUiComponent component) {
    }

    /**
     * Called after all apis and components have been generated.
     *
     * This would be a great place to save props.xapi files...
     */
    public void finish() {
        final UiGeneratorTools tools = generator.tools();
        if (isApiResolved()) {
            saveType(getApi(), generator, tools, none());
        }
        if (isBaseResolved()) {
            saveType(getBase(), generator, tools, none());
        }
        saveSource(generator);
    }

}
