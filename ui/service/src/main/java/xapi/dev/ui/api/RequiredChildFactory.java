package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.Expression;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 5/29/17.
 */
public class RequiredChildFactory {
    private final GeneratedUiDefinition definition;
    private final Expression sourceNode;


    public RequiredChildFactory(GeneratedUiDefinition definition, Expression sourceNode) {
        this.definition = definition;
        this.sourceNode = sourceNode;
    }

    public GeneratedUiDefinition getDefinition() {
        return definition;
    }

    public Expression getSourceNode() {
        return sourceNode;
    }
}
