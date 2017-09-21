package xapi.dev.lang;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.plugin.Transformer;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.api.ApiGeneratorTools;
import xapi.fu.Printable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/2/17.
 */
public class GeneratorAwareTransformer <Ctx extends ApiGeneratorContext<Ctx>> extends Transformer {

    private final Ctx ctx;
    private final ApiGeneratorTools<Ctx> tools;

    public GeneratorAwareTransformer(ApiGeneratorTools<Ctx> tools, Ctx ctx) {
        this.ctx = ctx;
        this.tools = tools;
    }

    @Override
    public String resolveName(Printable printer, NameExpr name) {
        String resolved = super.resolveName(printer, name);
        if (ctx.hasNode(resolved)) {
            final Node node = ctx.getNode(resolved);
            assert node instanceof Expression : "Non-expression found for [" + name + "]: " + tools.debugNode(node);
            return tools.resolveString(ctx, (Expression) node);
        }
        return resolved;
    }
}
