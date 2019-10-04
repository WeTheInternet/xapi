package xapi.gradle.paths;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.data.ListLike;
import xapi.fu.data.SetLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;
import xapi.fu.java.X_Jdk;
import xapi.fu.log.Log.LogLevel;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static xapi.fu.java.X_Jdk.set;

/**
 * A container for "all relevant paths" that are known about a single archive type w/in a module.
 *
 * Think of this like an internal copy of a {@link org.gradle.api.tasks.SourceSet}.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/27/18 @ 2:35 AM.
 */
public class AllPaths {

    private final SetLike<File> sources, resources, outputs, generated;
    private final SetLike<AllPaths> inherited;
    private final AtomicInteger mutated;
    // A root AllPaths will not increase mutation count upon absorption,
    // but its children will.  This ensures we see all child mutations...
    private boolean mutateOnAbsorb;

    public AllPaths() {
        mutated = new AtomicInteger(0);
        sources = X_Jdk.setLinked();
        resources = X_Jdk.setLinked();
        outputs = X_Jdk.setLinked();
        generated = X_Jdk.setLinked();
        inherited = X_Jdk.setLinked();
    }

    public SetLike<File> getSources() {
        resolve();
        return sources;
    }

    public SetLike<File> _sources() {
        return sources;
    }

    public SetLike<File> getResources() {
        resolve();
        return resources;
    }

    public SetLike<File> _resources() {
        return resources;
    }

    public SetLike<File> getOutputs() {
        resolve();
        return outputs;
    }

    public SetLike<File> _outputs() {
        return outputs;
    }

    public SetLike<File> getGenerated() {
        resolve();
        return generated;
    }

    public SetLike<File> _generated() {
        return generated;
    }

    private void resolve() {
        final Integer result = inherited
            .filter(this::isMutated)
            .map(this::setFrom)
            .reduce(Math::max, mutated.get());
        mutated.updateAndGet(is -> result > is ? result : is);
    }

    private int setFrom(AllPaths o) {
        this.sources.addNow(o.getSources());
        this.resources.addNow(o.getResources());
        this.outputs.addNow(o.getOutputs());
        this.generated.addNow(o.getGenerated());
        return o.mutated.get();
    }

    private boolean isMutated(AllPaths o) {
        return o.mutated.get() > this.mutated.get();
    }

    public void absorb(AllPaths ap) {
        if (ap == this) {
            return;
        }
        if (mutateOnAbsorb) {
            mutate();
        } else {
            ap.mutateOnAbsorb = true;
        }
        inherited.add(ap);
    }

    public String summary() {
        ListLike<String> values = X_Jdk.list();

        values.add(pathify(sources));
        values.add(pathify(resources));
        values.add(pathify(outputs));
        values.add(pathify(generated));
        values.add(inherited.map(AllPaths::summary)
            .map23(String::replace, ':', ';').join(":"));

        return values.join("::");
    }

    private String pathify(SetLike<File> sources) {
        return sources
            .filter(File::exists)
            .map(File::getAbsolutePath).join(":");
    }

    public static AllPaths deserialize(String path, InputStream from) throws ParseException {
        final UiContainerExpr el = JavaParser.parseUiContainer(
            path,
            from,
            LogLevel.INFO
        );
        final AllPaths paths = new AllPaths();
        final ComposableXapiVisitor<AllPaths> visitor = ComposableXapiVisitor.onMissingLog(
            AllPaths.class,
            false
        );
        String[] modName = {null};
        String[] type = {"main"};
        SetLike<String> provides = set();
        SetLike<String> includes = set();
        SetLike<String> inherits = set();
        SetLike<String>[] current = new SetLike[]{null};
        SetLike<File>[] into = new SetLike[]{null};
        In1<String> addItem = item -> {
            if (into[0] == null) {
                if (current[0] == null) {
                    assert false : "Somehow added item without target set";
                } else {
                    current[0].add(item);
                }
            } else {
                into[0].add(new File(item));
            }
        };
        visitor.withUiContainerTerminal(In2.ignoreAll())
            .withUiAttrTerminal((attr, arg)->{
                switch(attr.getNameString().toLowerCase()) {
                    case "sources":
                        into[0] = paths.getSources();
                        break;
                    case "resources":
                        into[0] = paths.getResources();
                        break;
                    case "outputs":
                        into[0] = paths.getOutputs();
                        break;
                    case "generated":
                        into[0] = paths.getGenerated();
                        break;
                    case "provides":
                        // calculate "provides" exclusions
                        current[0] = provides;
                        attr.getExpression().accept(visitor, arg);
                        break;
                    case "includes":
                        // automatically include these based on current location?
                        current[0] = includes;
                        attr.getExpression().accept(visitor, arg);
                        break;
                    case "inherits":
                        // load foreign inherits from our a given classpath?
                        // perhaps we can have "rootProject.configurations.xapiMeta" with all local paths in it
                        current[0] = inherits;
                        attr.getExpression().accept(visitor, arg);
                        break;
                    case "type":
                        type[0] = attr.getStringExpression(false);
                        break;
                    case "module":
                        modName[0] = attr.getStringExpression(false);
                        break;
                }
            })
            .withTemplateLiteralTerminal((str, arg)->
                addItem.in(str.getValueWithoutTicks())
            )
            .withStringLiteralTerminal((str, arg)->
                addItem.in(str.getValue())
            )
            .withQualifiedNameTerminal((name, arg)->
                addItem.in(name.getQualifiedName())
            )
            .withNameTerminal((name, arg)->
                addItem.in(name.getQualifiedName())
            )
        ;
        el.accept(visitor, paths);

        return paths;
    }

    public MappedIterable<File> getOwnFiles(boolean withSource) {
        SizedIterable<File> files = getGenerated();
        if (withSource) {
            files = files.plus(getSources());
        }
        files = files.plus(getOutputs());
        if (withSource) {
            // We want output resources to come before input ones...
            files = files.plus(getResources());
        }
        return files.filter(File::exists);
    }

    public MappedIterable<File> getAllFiles(boolean withSource) {
        MappedIterable<File> files = getOwnFiles(withSource);
        for (AllPaths children : inherited) {
            files = files.plus(children.getAllFiles(withSource));
        }
        return files;
    }

    public boolean isMutateOnAbsorb() {
        return mutateOnAbsorb;
    }

    public int mutate() {
        return mutated.incrementAndGet();
    }
}
