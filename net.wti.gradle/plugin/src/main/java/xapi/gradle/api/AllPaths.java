package xapi.gradle.api;

import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import xapi.fu.In1Out1;
import xapi.fu.data.ListLike;
import xapi.fu.data.SetLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.java.X_Jdk;

import java.io.File;

import static xapi.fu.java.X_Jdk.list;

/**
 * A container for "all relevant paths" that are known about a single archive type w/in a module.
 *
 * Think of this like an internal copy of a {@link SourceSet}.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/27/18 @ 2:35 AM.
 */
public class AllPaths {

    private final SetLike<File> sources, resources, outputs, generated;
    private final SetLike<AllPaths> inherited;
    private volatile int mutated;
    // A root AllPaths will not increase mutation count upon absorption,
    // but its children will.  This ensures we see all child mutations...
    private boolean mutateOnAbsorb;

    public AllPaths() {
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


    public SetLike<File> getResources() {
        resolve();
        return resources;
    }

    public SetLike<File> getOutputs() {
        resolve();
        return outputs;
    }

    public SetLike<File> getGenerated() {
        resolve();
        return generated;
    }

    private void resolve() {
        final Integer result = inherited
            .filter(this::isMutated)
            .map(this::setFrom)
            .reduce(Math::max, mutated);
        if (result > mutated) {
            mutated = result;
        }

    }

    private int setFrom(AllPaths o) {
        this.sources.addNow(o.getSources());
        this.resources.addNow(o.getResources());
        this.outputs.addNow(o.getOutputs());
        this.generated.addNow(o.getGenerated());
        return o.mutated;
    }

    private boolean isMutated(AllPaths o) {
        return o.mutated > this.mutated;
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public void addSources(SourceConfig config, In1Out1<File, Boolean> genDirCheck) {
        if (!mutateOnAbsorb) {
            // We may undo this if we see fit, but it's appropriate for now.
            throw new IllegalStateException("Only a child AllPaths may add sources; call parent.absorb(child) on " + this);
        }
        mutated++;
        for (File srcDir : config.getSources().getJava().getSrcDirs()) {
            sources.add(srcDir);
            if (genDirCheck.io(srcDir)) {
                generated.add(srcDir);
            }
        }
        for (File srcDir : config.getSources().getResources().getSrcDirs()) {
            resources.add(srcDir);
            if (genDirCheck.io(srcDir)) {
                generated.add(srcDir);
            }
        }
        final SourceSetOutput outs = config.getSources().getOutput();
        for (File dir : MappedIterable.mapped(outs.getDirs())
            .plus(outs.getClassesDirs())
            .plus(outs.getResourcesDir())
        ) {
            outputs.add(dir);
            if (genDirCheck.io(dir)) {
                generated.add(dir);
            }
        }
    }

    public void absorb(AllPaths ap) {
        if (ap == this) {
            return;
        }
        if (mutateOnAbsorb) {
            mutated++;
        } else {
            ap.mutateOnAbsorb = true;
        }
        inherited.add(ap);
    }

    public String summary() {
        ListLike<String> values = list();

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
}
