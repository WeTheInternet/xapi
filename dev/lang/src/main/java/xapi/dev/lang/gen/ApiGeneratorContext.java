package xapi.dev.lang.gen;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.SourceBuilder;
import xapi.fu.*;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.source.write.Template;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/21/16.
 */
public class ApiGeneratorContext<Ctx extends ApiGeneratorContext<Ctx>>
    implements ReturnSelf<Ctx> {

    private String generatorDirectory;
    private String outputDirectory;
    private StringTo<Node> vars;
    private StringTo<SourceBuilder<?>> sources;
    private boolean firstOfRange;
    private boolean inRange;
    private boolean ignoreChanges;

    @SuppressWarnings("unchecked")
    public ApiGeneratorContext() {
        vars = X_Collect.newStringMap(Node.class);
        sources = X_Collect.newStringMap(SourceBuilder.class);
    }

    public ApiGeneratorContext(ApiGeneratorContext from) {
        this.generatorDirectory = from.generatorDirectory;
        this.outputDirectory = from.outputDirectory;
        this.vars = from.vars;
        this.sources = from.sources;
        this.firstOfRange = from.firstOfRange;
        this.inRange = from.inRange;
    }

    public Do addToContext(String id, Node node) {
        String key = id.startsWith("$") ? id.substring(1) : id;
        final Node was = vars.get(key);
        vars.put(key, node);
        if (!ignoreChanges && was != null && !was.equals(node)) {
            X_Log.warn(ApiGeneratorContext.class, "Overriding key ", id, "was", was, "is", node);
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
        Template t = new Template(nameString, keys);
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

    public SourceBuilder<?> getOrMakeClass(String pkg, String cls, boolean isInterface) {
        final String fqcn = X_Source.qualifiedName(pkg, cls);
        return sources.getOrCreate(fqcn, create -> new SourceBuilder<>(this)
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

    public Iterable<SourceBuilder<?>> getSourceFiles() {
        return sources.values();
    }

    public void setGeneratorDirectory(String generatorDirectory) {
        this.generatorDirectory = generatorDirectory;
    }

    public String getGeneratorDirectory() {
        return generatorDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public boolean setIgnoreChanges(boolean ignoreChanges) {
        final boolean was = this.ignoreChanges;
        this.ignoreChanges = ignoreChanges;
        return was;
    }
}
