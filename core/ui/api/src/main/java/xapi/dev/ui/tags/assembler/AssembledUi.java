package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ast.expr.*;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.UiTagGenerator;
import xapi.source.X_Source;
import xapi.util.X_String;

/**
 * A container for information about a ui that is being assembled by a given component.
 *
 * This contains information about shadow and light DOM, names (and, later, types) of referenced elements.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/24/18.
 */
public class AssembledUi {

    private final UiGeneratorTools tools;
    private final UiNamespace namespace;
    private final ComponentBuffer source;
    private final UiTagGenerator generator;
    private final StringTo<AssembledElement> refs;
    private AssemblyRoot layoutRoot, logicalRoot;

    public AssembledUi(
        ComponentBuffer ui,
        UiGeneratorTools tools,
        UiNamespace namespace,
        UiTagGenerator generator
    ) {
        this.source = ui;
        this.tools = tools;
        this.namespace = namespace;
        this.generator = generator;
        refs = X_Collect.newStringMapInsertionOrdered(AssembledElement.class);
    }

    public AssemblyRoot layoutRoot() {
        return layoutRoot == null ? logicalRoot : layoutRoot;
    }

    public AssemblyRoot logicalRoot() {
        return logicalRoot == null ? layoutRoot : logicalRoot;
    }

    public AssemblyRoot getLayoutRoot() {
        return layoutRoot;
    }

    public AssemblyRoot getLogicalRoot() {
        return logicalRoot;
    }

    public AssemblyRoot requireLayoutRoot() {
        if (layoutRoot == null) {
            layoutRoot = new AssemblyRoot(this);
        }
        return layoutRoot;
    }

    public AssemblyRoot requireLogicalRoot() {
        if (logicalRoot == null) {
            logicalRoot = new AssemblyRoot(this);
        }
        return logicalRoot;
    }

    public GeneratedUiComponent getUi() {
        return source.getGeneratedComponent();
    }

    public UiGeneratorTools getTools() {
        return tools;
    }

    public ApiGeneratorContext getContext() {
        return source.getContext();
    }

    public UiNamespace getNamespace() {
        return namespace;
    }

    public UiAssemblerResult addAssembly(Expression uiExpr, boolean shadowUi, boolean hidden) {

        // TODO Look for template bindings, to figure out if we need to bind to any model fields
        // create a ui assembler for this root.
        UiAssembler assembler = generator.startAssembly(this, uiExpr, shadowUi, hidden);

//        final UiTagGeneratorVisitor visitor = new UiTagGeneratorVisitor(
//            this,
//            toDom,
//            rootRefs);
//        final String newBuilder = component.getElementBuilderConstructor(namespace);

        UiAssemblerResult result = new UiAssemblerResult();

        if (uiExpr instanceof JsonContainerExpr) {
            // A ui w/ a json container must be an array,
            final JsonContainerExpr json = (JsonContainerExpr) uiExpr;
            if (!json.isArray()) {
                throw new IllegalArgumentException("Children of a ui=feature must be either an array or an element, " +
                    "you sent: " + tools.debugNode(json));
            }
            // our element should act like / be a document fragment.
            for (JsonPairExpr pair : json.getPairs()) {
                if (!(pair.getValueExpr() instanceof UiContainerExpr)) {
                    throw new IllegalArgumentException("Array children of a ui=feature must be an element, " +
                        "you sent: " + tools.debugNode(pair));
                }
                final UiAssemblerResult root = assembler.addRoot(this, (UiContainerExpr) pair.getValueExpr());
                result.absorb(root);
            }
        } else  if (uiExpr instanceof UiContainerExpr) {
            // Now, visit any elements, storing variables to any refs.
            final UiAssemblerResult root = assembler.addRoot(this, (UiContainerExpr) uiExpr);
            result.absorb(root);
        } else {
            throw new IllegalArgumentException(
                "<define-tag/> only supports ui=<dom /> nodes;" +
                    "\nYou sent " + (shadowUi ?"shadow":"ui") + "=" + tools.debugNode(uiExpr)
            );
        }

        return result.finish();
    }

    public final String newBuilder() {
        return newBuilder(getNamespace());
    }

    public final String newBuilder(boolean searchable) {
        return newBuilder(getNamespace()).replace("()", "(" + searchable + ")");
    }

    public String newBuilder(UiNamespace ns) {
        return getUi().getElementBuilderConstructor(ns);
    }

    public final String getTypeBuilder() {
        return getTypeBuilder(getNamespace());
    }

    public String getTypeBuilder(UiNamespace ns) {
        return getUi().getBase().getElementBuilderType(ns);
    }

    public final String getTypeInjector() {
        return getTypeInjector(getNamespace());
    }

    public String getTypeInjector(UiNamespace ns) {
        return getUi().getMethods().getBaseTypeInjector(ns);
    }

    public final String newInjector() {
        return newInjector(getNamespace());
    }

    public String newInjector(UiNamespace ns) {
        return getUi().getElementInjectorConstructor(ns);
    }

    public boolean requireCompact(UiContainerExpr n) {
        return getGenerator().requireCompact(n);
    }

    public UiTagGenerator getGenerator() {
        return generator;
    }

    public final String getTypeElement() {
        return getTypeElement(getNamespace());
    }

    public String getTypeElement(UiNamespace namespace) {
        return getUi().getBase().getElementType(namespace);
    }

    public boolean hasRef(AssembledElement el) {
        return newRef(el, false) != null;
    }
    public String newRef(AssembledElement el, boolean create) {
        final Expression expr = el.getAst();
        String refName = null;

        if (expr instanceof UiContainerExpr) {
            UiContainerExpr ast = (UiContainerExpr) expr;
            final GeneratedUiBase base = getUi().getBase();
            refName = ast.getAttribute(UiNamespace.ATTR_REF)
                .map(attr-> {
                    // there is a ref attribute.  use it.
                    final Expression resolved = getGenerator().resolveReference(
                        getTools(),
                        getContext(),
                        getUi(),
                        getUi().getBase(),
                        el.maybeRequireRefRoot(),
                        el.maybeRequireRef(),
                        attr
                    );
                    String refString = getTools().resolveString(getContext(), resolved);
                    // Lets also create a public method exposing the raw element:
                    String elType = base.getElementType(namespace);
                    String nicified = X_String.toTitleCase(refString);
                    getBaseClass().createMethod("public " + elType + " el" + nicified + "()")
                        .returnValue(refString + ".out1().getElement()");
                    return refString;
                }, ()-> {
                    if (!create) {
                        return null;
                    }
                    // no ref attribute... generate one.
                    String refString = base.newFieldName((el.isParentRoot() ? "root" : "el") + X_Source.toCamelCase(ast.getName()));
                    ast.addAttribute(UiNamespace.ATTR_REF, StringLiteralExpr.stringLiteral(refString))
                        // mark our created ref attr as synthetic (not a user-requested ref)
                        .getExpression().addExtra(UiConstants.EXTRA_SYNTHENTIC, true);
                    return refString;
                })
                .get();

        } else if (expr instanceof JsonContainerExpr) {
            // arrays are okay
            JsonContainerExpr json = (JsonContainerExpr) expr;

        } else {
            // nothing else is currently supported.

        }

        if (refName == null) {
            return null;
        }
        final AssembledElement was = refs.put(refName, el);
        if (was != null && was != el) {
            throw new IllegalStateException("Attempt to overwrite " + refName + " (" + was + ") with " + el);
        }
        return refName;
    }

    public String newVarName(String s) {
        return source.getRoot().newVarName(s);
    }

    public AssembledElement attachChild(AssembledElement root, UiContainerExpr el) {
        return getGenerator().attachChild(this, root, el);
    }

    public ClassBuffer getBaseClass() {
        return getUi().getBaseClass();
    }
}
