package xapi.jre.ui.impl.feature;

import net.wti.lang.parser.ASTHelper;
import net.wti.lang.parser.ast.expr.*;
import net.wti.lang.parser.ast.expr.BinaryExpr.Operator;
import net.wti.lang.parser.ast.plugin.NodeTransformer;
import net.wti.lang.parser.ast.plugin.Transformer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope;
import xapi.fu.Maybe;
import xapi.jre.ui.runtime.DoubleSupplierBinding;

import java.util.List;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/1/16.
 */
public class JavaFxSizeFeatureGenerator extends UiFeatureGenerator {

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata container,
        UiAttrExpr attr
    ) {

        String panel = container.peekPanelName();
        final UiContainerExpr parent = container.getUi();
        final MethodBuffer mb = container.getMethod(panel);

        final UiAttrExpr size = parent.getAttributeNotNull("size");
        Expression expr = size.getExpression();
        while (expr instanceof EnclosedExpr) {
            expr = ((EnclosedExpr)expr).getInner();
        }
        final Expression width, height;
        if (ASTHelper.isNumericLiteral(expr)) {
            // a single numeric literal means "use this number for both width and height".
            width = expr;
            height = expr;
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) expr;
            if (binary.getOperator() == Operator.times) {
                width = binary.getLeft();
                height = binary.getRight();
            } else {
                width = binary;
                height = binary;
            }
        } else {
            width = expr;
            height = expr;
        }
        final Transformer transformer = generator.getTransformer(service, source.getRoot().getContext());
        String widthStr = toLambdaOrLiteral(width, transformer);
        String heightStr = toLambdaOrLiteral(height, transformer);
        if (width != null) {
            if (ASTHelper.isNumericLiteral(width)) {
                mb.println(panel+".setPrefWidth(" + widthStr + ");");
            } else {
                final Maybe<UiAttrExpr> fill = parent.getAttribute("fill");
                if (fill.isPresent()) {
                    if (!"height".equals(ASTHelper.extractAttrValue(fill.get()))) {
                        throw new IllegalArgumentException(
                              "Cannot use a width size expression in a container which uses a fill that modifies width. " +
                                    "Bad container: " + parent);
                    }
                }
                final String binder = mb.addImport(DoubleSupplierBinding.class);
                mb.println(panel + ".prefWidthProperty().bind(" + binder + ".valueOf(");
                mb.indentln(widthStr);
                mb.print(")");
                final List<String> notifiers = NodeTransformer.findTransformers(width);
                if (!notifiers.isEmpty()) {
                    mb.println();
                    notifiers.forEach(n->
                        mb.indentln(".bindNotifier(" + n + ".out1())")
                    );
                }
                mb.println(");");
            }
        }

        if (height != null) {
            if (ASTHelper.isNumericLiteral(height)) {
                mb.println(panel+".setPrefHeight(" + heightStr + ");");
            } else {
                final Maybe<UiAttrExpr> fill = parent.getAttribute("fill");
                if (fill.isPresent()) {
                    if (!"width".equals(ASTHelper.extractAttrValue(fill.get()))) {
                        throw new IllegalArgumentException(
                              "Cannot use a height size expression in a container which uses a fill that modifies height. " +
                                    "Bad container: " + parent);
                    }
                }
                final String binder = mb.addImport(DoubleSupplierBinding.class);
                mb.println(panel + ".prefHeightProperty().bind(" + binder + ".valueOf(");
                mb.indentln(heightStr);
                mb.print(")");
                final List<String> notifiers = NodeTransformer.findTransformers(width);
                if (!notifiers.isEmpty()) {
                    mb.println();
                    notifiers.forEach(n->
                          mb.indentln(".bindNotifier(" + n + ".out1())")
                    );
                }
                mb.println(");");
            }
        }

        return super.startVisit(service, generator, source, container, attr);
    }

    protected String toLambdaOrLiteral(Expression expr, Transformer transformer) {
        while (expr instanceof EnclosedExpr) {
            expr = ((EnclosedExpr)expr).getInner();
        }
        if (
              expr instanceof LambdaExpr ||
              expr instanceof MethodReferenceExpr ||
              expr instanceof DoubleLiteralExpr) {
            return expr.toSource(transformer);
        }
        if (expr instanceof IntegerLiteralExpr) {
            return expr.toSource(transformer) + "."; // declare as double
        }
        if (expr instanceof LongLiteralExpr) {
            return expr.toSource(transformer).replace("L", "."); // declare as double
        }

        return "()->" + expr.toSource(transformer);
    }
}
