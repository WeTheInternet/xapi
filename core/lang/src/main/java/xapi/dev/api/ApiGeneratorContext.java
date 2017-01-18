package xapi.dev.api;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.SourceBuilder;
import xapi.fu.Do;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.ReturnSelf;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.source.write.StringerMatcher;
import xapi.source.write.Template;

import java.util.function.Predicate;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/21/16.
 */
public class ApiGeneratorContext<Ctx extends ApiGeneratorContext<Ctx>>
    implements ReturnSelf<Ctx> {

    public ApiGeneratorContext() { }

    private StringTo<Node> vars = X_Collect.newStringMap(Node.class);
    private StringTo<SourceBuilder<Ctx>> sources = X_Collect.newStringMap(SourceBuilder.class);
    private boolean firstOfRange;
    private boolean inRange;

    public Do addToContext(String id, Node node) {
        String key = id.startsWith("$") ? id.substring(1) : id;
        final Node was = vars.get(key);
        vars.put(key, node);
        if (was != null && was != node) {
            X_Log.warn(getClass(), "Overriding key ", id, "was", was, "is", node);
        }
        return was == null ? () -> vars.remove(key) : () -> vars.put(key, was);
    }

    public String resolveValues(String nameString, In1Out1<Node, Object> mapper) {
        final String[] keys = vars.keyArray();
        final Out1<?>[] values = new Out1[keys.length];
        for (
            int i = keys.length;
            i-- > 0;
            ) {
            final String key = keys[i];
            keys[i] = key.startsWith("$") ? key : "$" + key;
            values[i] = Lazy.deferred1(
                () -> mapper.io(vars.get(key))
            );
        }
        Template t = new Template(nameString, new StringerMatcher() {
            @Override
            public String toString(Object o) {
                if (o instanceof Out1) {
                    o = ((Out1) o).out1();
                }
                return o.toString();
            }

            @Override
            public Predicate<String> matcherFor(String value) {
                return value::equals;
            }
        }, keys);
        return t.apply((Object[]) values);
    }

    public boolean hasNode(String key) {
        return vars.containsKey(key.startsWith("$") ? key.substring(1) : key);
    }

    public Node getNode(String key) {
        return vars.get(key.startsWith("$") ? key.substring(1) : key);
    }

    public String getString(String key) {
        final Node node = getNode(key);
        return node == null ? null : ASTHelper.extractStringValue((Expression) node);
    }

    public SourceBuilder<Ctx> getOrMakeClass(String pkg, String cls, boolean isInterface) {
        final String fqcn = X_Source.qualifiedName(pkg, cls);
        return sources.getOrCreate(fqcn, create -> new SourceBuilder<>(self())
            .setClassDefinition("public " + (isInterface ? "interface" : "class") + " " + cls, false)
            .setPackage(pkg)
        );
    }

    public void setFirstOfRange(boolean firstOfRange) {
        this.firstOfRange = firstOfRange;
    }

    public boolean isFirstOfRange() {
        if (!inRange) {
            throw new IllegalStateException("Cannot use $first() outside of a $range expression");
        }
        return firstOfRange;
    }

    public void setInRange(boolean inRange) {
        this.inRange = inRange;
    }

    public boolean isInRange() {
        return inRange;
    }

    public Iterable<SourceBuilder<Ctx>> getSourceFiles() {
        return sources.values();
    }
}
