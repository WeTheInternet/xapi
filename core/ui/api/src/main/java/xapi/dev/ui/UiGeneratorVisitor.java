package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiBodyExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.dev.ui.UiVisitScope.ScopeType;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.util.api.RemovalHandler;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/4/16.
 */
public class UiGeneratorVisitor extends VoidVisitorAdapter<UiGeneratorTools> {

    interface ScopeHandler extends In1Out1<UiVisitScope, RemovalHandler> { }

    final Lazy<ContainerMetadata> root;
    private ScopeHandler onScope;
    ContainerMetadata parent;
    private UiComponentGenerator generator;
    private UiFeatureGenerator feature;

    public UiGeneratorVisitor(ScopeHandler onScope) {
        this.onScope = onScope;
        root = Lazy.deferred1(this.createRoot());
    }

    public UiGeneratorVisitor(ScopeHandler onScope, Out1<ContainerMetadata> source) {
        this.onScope = onScope;
        root = Lazy.deferred1(source);
    }

    public UiGeneratorVisitor(ScopeHandler onScope, ContainerMetadata source) {
        this.onScope = onScope;
        root = Lazy.immutable1(source);
    }

    protected Out1<ContainerMetadata> createRoot() {
        return ContainerMetadata::new;
    }

    public void wrapScope(In1Out1<ScopeHandler, ScopeHandler> mapper) {
        onScope = mapper.io(onScope);
    }

    @Override
    public void visit(UiContainerExpr n, UiGeneratorTools service) {
        boolean isRoot = parent == null;
        final ContainerMetadata myParent = getParent();

        final ContainerMetadata me;
        if (isRoot) {
            me = parent = myParent;
        } else {
            me = parent = myParent.createChild(n, service);
        }
        final UiComponentGenerator oldGenerator = generator;
        try {
            final UiComponentGenerator myGenerator = generator = service.getComponentGenerator(n, me);
            if (myGenerator != null) {
                final UiVisitScope scope = generator.startVisit(service, me, n);
                assert scope.getType() == ScopeType.CONTAINER : "Expected container scope " + scope;
                final RemovalHandler undo = onScope.io(scope);
                if (scope.isVisitChildren()) {

                    super.visit(n, service);
                }
                undo.remove();
                myGenerator.endVisit(service, me, n, scope);
            }

        } finally {
            parent = myParent;
            generator = oldGenerator;
        }

    }

    @Override
    public void visit(
          UiAttrExpr n, UiGeneratorTools service
    ) {
        final UiComponentGenerator myGenerator = generator;
        final UiFeatureGenerator oldFeature = feature;
        try {

            final UiFeatureGenerator myFeature = feature = service.getFeatureGenerator(n, generator);
            if (myFeature != null) {
                final UiVisitScope scope = myFeature.startVisit(service, myGenerator, parent, n);
                assert scope.getType() == ScopeType.FEATURE : "Expected feature scope " + scope;
                final RemovalHandler undo = onScope.io(scope);
                if (scope.isVisitChildren()) {
                    super.visit(n, service);
                }
                undo.remove();
                myFeature.finishVisit(service, myGenerator, parent, n, scope);
                if (myFeature.hasSideEffects()) {
                    myGenerator.getMetadata().recordSideEffects(service, myFeature);
                }
            }
        } finally {
            feature = oldFeature;
        }
    }

    @Override
    public void visit(
          UiBodyExpr n, UiGeneratorTools arg
    ) {
        if (n.isNotEmpty()) {

        }
        super.visit(n, arg);
    }

    private ContainerMetadata getParent() {
        if (parent == null) {
            parent = root.out1();
        }
        return parent;
    }
}
