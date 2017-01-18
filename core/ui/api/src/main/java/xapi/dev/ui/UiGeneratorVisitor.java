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

    public interface ScopeHandler extends In1Out1<UiVisitScope, RemovalHandler> { }

    final Lazy<ContainerMetadata> root;
    private final ComponentBuffer source;
    private ScopeHandler onScope;
    ContainerMetadata parent;
    private UiComponentGenerator generator;
    private UiFeatureGenerator feature;

    public UiGeneratorVisitor(ScopeHandler onScope, ComponentBuffer source) {
        this.onScope = onScope;
        root = Lazy.deferred1(this.createRoot());
        this.source = source;
    }

    public UiGeneratorVisitor(ScopeHandler onScope, Out1<ContainerMetadata> metadata, ComponentBuffer source) {
        this.onScope = onScope;
        root = Lazy.deferred1(metadata);
        this.source = source;
    }

    public UiGeneratorVisitor(ScopeHandler onScope, ContainerMetadata metadata, ComponentBuffer source) {
        this.onScope = onScope;
        root = Lazy.immutable1(metadata);
        this.source = source;
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
        me.setImplementation(myParent.getImplementation());
        final UiComponentGenerator oldGenerator = generator;
        try {
            final UiComponentGenerator myGenerator = generator = service.getComponentGenerator(n, me);
            if (myGenerator != null) {
                final UiVisitScope scope = generator.startVisit(service, source, me, n);
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
                final UiVisitScope scope = myFeature.startVisit(service, myGenerator, source, parent, n);
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
            /*
            TODO: allow parser selectors like so:
            <some-ui>
              {json/
                {
                  uses: "json parser",
                  is : "wrapped in ForeignNode"
                }
              /json}
              {java/
                @Ui(`
                  <this is = \` Some java type wrapped in a ForeignNode \`/>
                `)
                package de.mocra.cy;
              /java}
              <raw-xapi
                is="embedded child node"
                processedAt=sameTimeAsParentNode()
                deferredProcessing =
                  {xapi/
                      <defer-parsing
                        is = \`
                          also wrapped in ForeignNode,
                          to ensure contents are a "black box",
                          which are not parsed until you open it.

                          Think of it like the blood-brain barrier for code :-)
                          Or Schrodinger's Scope :-/
                        \`/>
                  /xapi}
                  // note that embedded languages which parse // as comments
                  //can-embed-close tags} in comments,
                  // since the parent parser slices off the /EOF} style symbol.

              /raw-xapi>
              // note that <self-closing /> tags are allowed to be written as:
              // <self-closing value=<some thing=Huge /> /self-closing>
              // to ensure that /> is not erroneously mapped to an unexpected element.
              // This ensures that a parse breaks closer to the source of the error.

              {wti/
                Allow for custom syntax of any kind,
                by registering a lang name,
                which must map to a (findable parser plugin)->{
                  The plugin will be used to stream into the source,
                  until the end token is seen,
                  with the end token in the form /lang}
                  where lang is used in the opening {lang/ tag,
                  (which is safe to use inside this block,
                  as we let the foreign parser (process everything)->{
                    Because we know what the closing tag is going to be,
                    we can, by default, make all ForeignNode lang tags lazy,
                    thus deferring the scope (and failure) of embedded syntax.
                  }
                  between the opening and closing lang tags.

                  Beware a hack like adding "/xapi}" in other languages;
                  this should never happen anywhere; this lang tag should
                  be special cased such that it is only allowed when
                  the immediate parent lang tag is {xapi/
                  (if found when searching for closing lang tag, throw exception)
                }.
              /wti}
            </some-ui>
            */
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
