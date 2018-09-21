package xapi.dev.api;

import xapi.fu.Lazy;
import xapi.fu.itr.MappedIterable;
import xapi.fu.Out1;
import xapi.fu.itr.ArrayIterable;
import xapi.model.X_Model;
import xapi.scope.api.Scope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/11/17.
 */
public abstract class AbstractClasspathProvider <Self extends AbstractClasspathProvider<Self>> implements ClasspathProvider<Self> {

    protected volatile Scope scope;

    private final Lazy<Classpath> classpath = Lazy.deferred1(()->{
        Classpath cp = X_Model.create(Classpath.class);
        initClasspath(scope, cp);
        scope = null;
        return cp;
    });

    protected MappedIterable<String> loadDependencies(Out1<? extends Iterable<String>> ... deps) {
        return ArrayIterable.iterate(deps)
            .map(Out1::out1)
            .flatten(MappedIterable::mapped);
    }

    protected abstract void initClasspath(Scope scope, Classpath cp);

    @Override
    public Out1<Classpath> loadClasspath(Scope s) {
        // TODO ensure WeakReference is emulated in gwt then use it...
        this.scope = s;
        return classpath;
    }


}
