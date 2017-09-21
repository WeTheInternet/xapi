package xapi.server.gen;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.JsonPairExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope;
import xapi.dev.ui.api.UiVisitScope.ScopeType;
import xapi.except.NotYetImplemented;
import xapi.fu.In2Out1;
import xapi.fu.In3Out1;
import xapi.fu.MappedIterable;
import xapi.fu.Maybe;
import xapi.javac.dev.util.NameUtil;
import xapi.model.X_Model;
import xapi.mvn.api.MvnCoords;
import xapi.mvn.api.MvnDependency;
import xapi.server.gen.GeneratedClasspathInfo.ClasspathItem;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public class ClasspathFeatureGenerator extends UiFeatureGenerator {

    private final WebAppComponentGenerator owner;

    public ClasspathFeatureGenerator(WebAppComponentGenerator owner) {
        this.owner = owner;
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata container,
        UiAttrExpr attr
    ) {
        final UiVisitScope scope = new UiVisitScope(ScopeType.FEATURE);

        Expression expr = attr.getExpression();
        if (!(expr instanceof JsonContainerExpr)) {
            throw new IllegalArgumentException("classpaths feature only accepts json nodes as children (your classpaths must have name keys); you sent " + attr);
        }

        JsonContainerExpr elements = (JsonContainerExpr) expr;

        for (JsonPairExpr classpath : elements.getPairs()) {
            final ApiGeneratorContext ctx = source.getRoot().getContext();
            String name = service.resolveString(ctx, classpath.getKeyExpr());
            final Expression val = service.resolveVar(ctx, classpath.getValueExpr());
            JsonContainerExpr entries = val instanceof JsonContainerExpr ?
                (JsonContainerExpr) val : JsonContainerExpr.jsonArray(val);
            final GeneratedClasspathInfo info = toInfo(service, source, name, entries);
            owner.getGenerator().addClasspath(name, info);
            service.getGenerator().onFinish(WebAppGenerator.PRIORITY_RESOLVE_CLASSPATHS, info::finish);
        }

        scope.setVisitChildren(false);
        return scope;
    }

    private GeneratedClasspathInfo toInfo(
        UiGeneratorTools service,
        ComponentBuffer source,
        String name,
        JsonContainerExpr entries
    ) {
        final GeneratedClasspathInfo info = new GeneratedClasspathInfo(service, source, name);
        final ApiGeneratorContext ctx = source.getRoot().getContext();
        final MappedIterable<Expression> exprs = entries.getValues()
            .<ApiGeneratorContext, Expression>map1(service::resolveVar, ctx);
        for (Expression expr : exprs) {
            if (expr instanceof JsonContainerExpr) {
                final JsonContainerExpr json = (JsonContainerExpr) expr;
                if (json.isArray()) {
                    for (JsonPairExpr pair : json.getPairs()) {
                        ClasspathItem item = toItem(service, ctx, source, info, pair.getValueExpr());
                        info.addClasspathItem(item);
                    }
                } else {
                    ClasspathItem item = toItem(service, ctx, source, info, expr);
                    info.addClasspathItem(item);
                }
            } else {
                ClasspathItem item = toItem(service, ctx, source, info, expr);
                info.addClasspathItem(item);
            }
        }
        return info;
    }

    private ClasspathItem toItem(
        UiGeneratorTools service,
        ApiGeneratorContext ctx,
        ComponentBuffer source,
        GeneratedClasspathInfo info,
        Expression expr
    ) {
        final ClasspathItem item = new ClasspathItem(info, expr);
        if (expr instanceof MethodCallExpr) {
            final MethodCallExpr call = (MethodCallExpr) expr;
            switch (call.getName()) {
                case "$maven":
                    // alright! we should expect our value to either be a coordinate string, or a json map {groupId:...}
                    // TODO consider accepting method invocations to do key:value pairing:
                    // $maven(groupId("net.wti"), artifactId("my-module"), ...))
                    MvnCoords dependency = extractMavenDependency(service, source, item, call);
                    item.setMavenCoordinates(dependency);
                    break;
                default:
                    throw new IllegalArgumentException("Unable to process " + call.toSource());
            }
        } else {
            String dep = service.resolveString(ctx, expr);
            // assume either a relative or static path...
            // TODO: check for runtime variables, i.e., the character $
            item.setPath(dep);
        }
        return item;
    }

    protected MvnCoords extractMavenDependency(
        UiGeneratorTools service,
        ComponentBuffer source,
        ClasspathItem item,
        MethodCallExpr call
    ) {
        if (call.getArgs().size() == 1) {
            final Expression arg = call.getArgs().get(0);
            if (arg instanceof JsonContainerExpr) {
                final MvnCoords coords = X_Model.create(MvnCoords.class);
                // when it's a json container, it will need to have groupId, artifactId and version;
                // groupId and version will default to net.wetheinter:
                JsonContainerExpr json = (JsonContainerExpr) arg;
                final In2Out1<Expression, ApiGeneratorContext, String> mapper = service::resolveStringReverse;

                final ApiGeneratorContext ctx = source.getContext();
                String groupId = json.getNodeMaybe("groupId")
                    .mapIfPresent(mapper, ctx)
                    .ifAbsentSupply(owner.getGenerator()::getDefaultGroupId);

                String version = json.getNodeMaybe("groupId")
                    .mapIfPresent(mapper, ctx)
                    .ifAbsentSupply(owner.getGenerator()::getDefaultVersion);

                String artifactId = service.resolveString(ctx, json.getNode("artifactId"));

                final Maybe<String> classifier = json.getNodeMaybe("classifier")
                    .mapIfPresent(mapper, ctx);

                // When there is a classifier but no packaging, we will default to using jar type
                final Maybe<String> packaging = json.getNodeMaybe("packaging")
                    .mapIfAbsent(() -> json.getNodeMaybe("type").get())
                    .mapIfPresent(mapper, ctx)
                    .mapIfAbsent(() -> classifier.mapIfPresent(s -> "jar").get());

                // Hokay! Lets generate a ClasspathProvider instance
                // which will pre-load the maven dependency graph as early as possible,
                // and block on actually downloading missing dependencies until as late as possible
                // (we will also make the dev instance prime local filesystem while building,
                // so that if you are building on the machine you are running [local dev], then
                // your maven repo will always be primed before runtime even starts).

                coords.setGroupId(groupId);
                coords.setArtifactId(artifactId);
                coords.setVersion(version);
                coords.setClassifier(classifier.ifAbsentReturn(null));
                coords.setPackaging(packaging.ifAbsentReturn(null));
                return coords;
            } else {
                String val = service.resolveString(source.getContext(), arg);
                // must be maven coordinate...
                final MvnCoords<?> coords = MvnCoords.fromString(val);
                return coords;
            }
        } else {
            // only acceptable multi-arg format is named vars using methods...
            throw new NotYetImplemented("Haven't yet implemented classpath generator for " + call);
        }
    }
}
