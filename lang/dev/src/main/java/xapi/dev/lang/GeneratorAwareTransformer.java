package xapi.dev.lang;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.TemplateLiteralExpr;
import com.github.javaparser.ast.plugin.Transformer;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.lang.gen.ApiGeneratorTools;
import xapi.fu.Printable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/2/17.
 */
public class GeneratorAwareTransformer <Ctx extends ApiGeneratorContext<Ctx>> extends Transformer {

    private final Ctx ctx;
    private final ApiGeneratorTools<Ctx> tools;
    private final Transformer delegate;

    public GeneratorAwareTransformer(
        ApiGeneratorTools<Ctx> tools,
        Ctx ctx,
        Transformer transformer
    ) {
        this.ctx = ctx;
        this.tools = tools;
        this.delegate = transformer == null || transformer.getClass() == Transformer.class ? null : transformer;
    }

    @Override
    public String resolveName(Printable printer, NameExpr name) {
        String resolved = delegate == null ? super.resolveName(printer, name) : delegate.resolveName(printer, name);
        if (ctx.hasNode(resolved)) {
            final Node node = ctx.getNode(resolved);
            assert node instanceof Expression : "Non-expression found for [" + name + "]: " + tools.debugNode(node);
            return tools.resolveString(ctx, (Expression) node);
        }
        return resolved;
    }

    @Override
    public String resolveType(ClassOrInterfaceType type) {
        return delegate == null ? super.resolveType(type) : delegate.resolveType(type);
    }

    @Override
    public String resolveTypeParamName(TypeParameter param) {
        return delegate == null ? super.resolveTypeParamName(param) : delegate.resolveTypeParamName(param);
    }

    @Override
    public String onTemplateStart(Printable printer, TemplateLiteralExpr template) {
        return delegate == null ? super.onTemplateStart(printer, template) : delegate.onTemplateStart(printer, template);
    }

    @Override
    public void onTemplateEnd(Printable printer) {
        if (delegate == null) {
            super.onTemplateEnd(printer);
        } else {
            delegate.onTemplateEnd(printer);
        }
    }

    @Override
    public void normalizeToString(Printable printer, String template) {
        if (delegate == null) {
            super.normalizeToString(printer, template);
        } else {
            delegate.normalizeToString(printer, template);
        }
    }
}
