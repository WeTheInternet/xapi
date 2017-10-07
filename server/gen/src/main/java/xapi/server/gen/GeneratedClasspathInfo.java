package xapi.server.gen;

import com.github.javaparser.ast.expr.Expression;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.api.AbstractClasspathProvider;
import xapi.dev.api.Classpath;
import xapi.dev.source.CanAddImports;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.ImportSection;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.GeneratedJavaFile;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.Immutable;
import xapi.fu.Lazy;
import xapi.fu.MappedIterable;
import xapi.fu.Out1;
import xapi.fu.iterate.SingletonIterator;
import xapi.mvn.api.MvnCoords;
import xapi.scope.api.Scope;
import xapi.source.X_Modifier;
import xapi.source.X_Source;
import xapi.source.api.HasImports;
import xapi.source.read.JavaModel.IsTypeDefinition;
import xapi.util.X_String;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/8/17.
 */
public class GeneratedClasspathInfo {

    public static class ClasspathItem {

        private final GeneratedClasspathInfo owner;
        private final Expression expr;
        private final Lazy<MethodBuffer> source;
        private final Lazy<String> name;

        private MvnCoords coords;
        private String path;
        private boolean isMvn;

        public ClasspathItem(GeneratedClasspathInfo owner, Expression expr) {
            this.owner = owner;
            this.expr = expr;
            name = Lazy.deferred1(this::guessName);
            source = Lazy.deferred1(()->{
                final ClassBuffer src = owner.getSource();
                String retType = owner.getStringListProvider();
                final MethodBuffer method = src.createMethod("private " + retType + " " + name.out1());
                return method;
                }
            );
        }

        private String guessName() {
            if (expr.hasExtra("name")) {
                return expr.getExtra("name");
            }
            if (coords != null) {
                return "mvn" + X_Source.toCamelCase(X_Source.javaSafeName(coords.getArtifactId()));
            }
            // TODO actually look in the expression...
            return "item" + System.identityHashCode(this);
        }

        public GeneratedClasspathInfo getOwner() {
            return owner;
        }

        public MethodBuffer getSource() {
            return source.out1();
        }

        public String getName() {
            return name.out1();
        }

        public void setMavenCoordinates(MvnCoords coords) {
            this.coords = coords;
            writeCoords(coords);
        }

        protected void writeCoords(MvnCoords coords) {
            isMvn = owner.hasMavenCoords = true;
            final MethodBuffer out = source.out1().println("return fromMaven(")
                .indent()
                .print(string(coords.getGroupId())).println(",")
                .print(string(coords.getArtifactId())).println(",");
            if (coords.getClassifier() == null) {
                if (coords.getPackaging() != null) {
                    out.print(string(coords.getPackaging())).println(",");
                }
            } else {
                out.print(string(X_String.firstNotEmpty(coords.getPackaging(), "jar"))).println(",");
                out.print(string(coords.getClassifier())).println(",");
            }
            out.println(string(coords.getVersion()))
               .outdent()
               .println(");");

            printInitLine();
        }

        private void printInitLine() {
            boolean first = owner.init.isUnresolved();
            MethodBuffer init = owner.init.out1();
            if (!first) {
                init.println(",");
            }
            init.print(source.out1().getName()).print("()");
        }

        private String string(String s) {
            return s.startsWith("\"") ? s : "\"" + X_Source.escape(s) + "\"";
        }

        public void setPath(String path) {
            this.path = path;
            writePath(path);
        }

        protected void writePath(String path) {
            // TODO: check if path has variables...
            source.out1().print("return ")
                         .print(owner.getSingleStringProvider())
                         .print(X_Source.escape(path))
                         .print("\"));");
            printInitLine();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final ClasspathItem that = (ClasspathItem) o;

            if (coords != null ? !coords.equals(that.coords) : that.coords != null)
                return false;
            return path != null ? path.equals(that.path) : that.path == null;
        }

        @Override
        public int hashCode() {
            int result = coords != null ? coords.hashCode() : 0;
            result = 31 * result + (path != null ? path.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ClasspathItem{" +
                "expr=" + expr +
                ", source=" + (source.isResolved()?source.out1():"<unresolved>") +
                ", name=" + name.out1() +
                ", coords=" + coords +
                ", path='" + path + '\'' +
                '}';
        }

        public boolean isMvn() {
            return isMvn;
        }
    }

    private String getStringListProvider() {
        return stringListProvider.out1();
    }

    private String getSingleStringProvider() {
        return singleStringProvider.out1();
    }

    private final String name;
    private final ComponentBuffer buffer;
    private final UiGeneratorTools service;
    private final Lazy<GeneratedJavaFile> source;
    private final Lazy<MethodBuffer> init;
    private final StringTo<ClasspathItem> items;
    /**
     * provides imported name: Out1<MappedIterable<String>>
      */
    private final Lazy<String> stringListProvider;
    private final Lazy<String> singleStringProvider;
    private boolean hasMavenCoords;

    public GeneratedClasspathInfo(UiGeneratorTools service, ComponentBuffer buffer, String name) {
        this.service = service;
        this.name = name;
        this.buffer = buffer;
        source = Lazy.deferred1(()->{
            final ComponentBuffer source = getBuffer();
            final GeneratedUiComponent component = source.getGeneratedComponent();
            String ident = X_Source.javaSafeName(name);
            GeneratedJavaFile layer = component.getOrCreateExtraLayer(ident, component.getClassName());
            // TODO: check expr for annotation to influence naming
            layer.setPrefix("Dep");
            if (!layer.getWrappedName().toLowerCase().contains(ident.toLowerCase())) {
                layer.setSuffix(ident);
            }
            layer.setType(IsTypeDefinition.newClass(layer.getPackageName(), layer.getWrappedName()));

            // Lets setup some boilerplate:
            final ClassBuffer out = layer.getSource().getClassBuffer();
            String lazy = out.addImport(Lazy.class);
            String self = layer.getWrappedName();
            out.createConstructor(X_Modifier.PRIVATE);
            out.createField(lazy + "<" + self + ">", "INSTANCE", X_Modifier.PRIVATE_STATIC_FINAL)
                .setInitializer(lazy + ".deferred1(" + self + "::new);");
            out.createMethod(X_Modifier.PUBLIC_STATIC, self, "inherit")
                .returnValue("INSTANCE.out1();");

            return layer;
        });
        stringListProvider = Lazy.deferred1(()->{
            final ClassBuffer src = getSource();
            return src.addImport(Out1.class) + "<" + src.addImport(MappedIterable.class) + "<String>>";
        });
        singleStringProvider = Lazy.deferred1(()->{
            final ClassBuffer src = getSource();
            return src.addImportStatic(Immutable.class, "immutable1")
                 + "(" + src.addImportStatic(SingletonIterator.class, "singleItem")
                 + "(\"";
        });
        items = X_Collect.newStringMap(ClasspathItem.class);
        init = Lazy.deferred1(()->{
            final GeneratedJavaFile layer = source.out1();
            final ClassBuffer out = layer.getSource().getClassBuffer();
            final MethodBuffer init = out.createMethod(X_Modifier.PROTECTED, void.class, "initClasspath")
                .addParameter(Scope.class, "scope")
                .addParameter(Classpath.class, "cp")
                .println("cp.getPaths().addAll(loadDependencies(")
                .indent();
            return init;
        });
    }

    public ClassBuffer getSource() {
        return source.out1().getSource().getClassBuffer();
    }

    public void absorb(GeneratedClasspathInfo other) {
        other.items.forBoth((k, v)->{
            final ClasspathItem collision = items.get(k);
            if (collision == null) {
                items.put(k, v);
            } else {
                if (!collision.equals(v)) {
                    throw new IllegalStateException("Classpath Key " + k + " reused for inequal entities: \n" + v +
                        "\nvs\n" + collision);
                }
            }
        });
        if (other.hasMavenCoords) {
            hasMavenCoords = true;
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final GeneratedClasspathInfo that = (GeneratedClasspathInfo) o;

        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public void addClasspathItem(ClasspathItem item) {
        String name = item.getName();
        final ClasspathItem was = items.get(name);
        if (was == null) {
            items.put(item.getName(), item);
        } else if (!was.equals(item)){
            throw new IllegalStateException("Classpath Key " + name + " reused for inequal entities: \n" + item +
                "\nvs\n" + was);
        }
    }

    public ComponentBuffer getBuffer() {
        return buffer;
    }

    public UiGeneratorTools getService() {
        return service;
    }

    public void finish() {
        // finish off the init method
        init.out1()
            .outdent()
            .println("));");
        // apply correct supertype...
        final ClassBuffer out = getSource();
        if (hasMavenCoords) {
            String mvnType = out.addImport("xapi.mvn.impl.MvnClasspathProvider");
            out.setSuperClass(mvnType + "<" + out.getSimpleName() + ">");
        } else {
            assert items.forEachValue().noneMatch(ClasspathItem::isMvn);
            String abstractType = out.addImport(AbstractClasspathProvider.class);
            out.setSuperClass(abstractType + "<" + out.getSimpleName() + ">");
        }
    }

    public String inherit(CanAddImports mb) {
        return mb.addImport(getSource().getQualifiedName()) +".inherit()";
    }
}
