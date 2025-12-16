package xapi.dev.api;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.model.api.Model;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/4/16.
 */
public interface Classpath extends Model {

    IntTo<String> getPaths();

    default IntTo<String> getOrCreatePaths() {
        return getOrCreate(this::getPaths, X_Collect::newStringList, this::setPaths);
    }

    Classpath setPaths(IntTo<String> paths);

    Expression getSource();

    Classpath setSource(Expression source);

}
