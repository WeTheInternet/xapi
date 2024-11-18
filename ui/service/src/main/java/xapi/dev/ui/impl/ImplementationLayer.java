package xapi.dev.ui.impl;

import xapi.dev.lang.gen.GeneratedJavaFile;
import xapi.dev.lang.gen.GeneratedTypeOwner;
import xapi.dev.lang.gen.SourceLayer;
import xapi.dev.ui.api.GeneratedUiDefinition;
import xapi.dev.ui.api.GeneratedUiLayer;
import xapi.dev.ui.api.ImplementationGenerator;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/2/18 @ 2:54 AM.
 */
public abstract class ImplementationLayer extends GeneratedUiLayer {

    private ImplementationGenerator<?, ?> generator;

    public ImplementationLayer(String pkg, String cls, SourceLayer layer, GeneratedTypeOwner owner) {
        super(pkg, cls, layer, owner);
    }

    public ImplementationLayer(
        GeneratedJavaFile superType,
        String pkg,
        String cls,
        SourceLayer layer,
        GeneratedTypeOwner owner
    ) {
        super(superType, pkg, cls, layer, owner);
    }

    public void setGenerator(ImplementationGenerator<?, ?> generator) {
        this.generator = generator;
    }

    public ImplementationGenerator<?, ?> getGenerator() {
        return generator;
    }

    public String mangleName(GeneratedUiDefinition def) {
        return generator.getImplName(def.getPackageName(), def.getApiName());
    }

    public String mangleName(String pkgName, String typeName) {
        return generator.getImplName(pkgName, typeName);
    }

}
