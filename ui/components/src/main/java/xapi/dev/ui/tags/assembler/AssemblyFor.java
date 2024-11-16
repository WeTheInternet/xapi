package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.UiTagGenerator;
import xapi.dev.ui.tags.factories.GeneratedFactory;
import xapi.fu.Do;
import xapi.fu.Maybe;

import static xapi.dev.ui.api.UiConstants.EXTRA_MODEL_INFO;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/26/18.
 */
public class AssemblyFor extends AssembledElement {

    /**
     * If you want a no-parent version of this constructor, see {@link AssemblyRoot} (or subclass yourself)
     *
     * @param source - The ui assembly which owns this element
     * @param parent - The parent element, never null (see {@link AssemblyRoot#SUPER_ROOT}.
     * @param ast    - The {@code <dom />} ast for the current element. never null.
     */
    public AssemblyFor(AssembledUi source, AssembledElement parent, UiContainerExpr ast) {
        super(source, parent, ast);
    }

    @Override
    public GeneratedFactory startElement(
        AssembledUi ui,
        UiAssembler assembler,
        AssembledElement parent,
        UiContainerExpr el
    ) {
        final UiTagGenerator generator = ui.getGenerator();
        final UiNamespace namespace = ui.getNamespace();
        final MethodBuffer toDom = generator.toDomMethod(namespace, ui.getUi().getBase());
        final UiGeneratorTools tools = ui.getTools();
        final GeneratedUiComponent component = ui.getUi();
        final ApiGeneratorContext ctx = ui.getContext();
        final GeneratedUiBase baseClass = component.getBase();

        toDom.print("for (");
        getAst().getAttribute("allOf")
            .readIfPresent(all->{
                final Expression startExpr = all.getExpression();
                final Expression endExpr = generator.resolveReference(tools, ctx, component, baseClass, getRoot().maybeRequireRef(), maybeRequireRef(), all);

                final Maybe<UiAttrExpr> index = getAst().getAttribute("index");

                // TODO an alternative to as which exposes the index as well/instead
                getAst().getAttribute("as")
                    .readIfPresent(as->{
                        final String asName = tools.resolveString(ctx, as.getExpression());
                        // Ok! We have allOf=someData, likely $model.fieldName,
                        // and an as=name to put into the context.

                        // Lets find out what type our elements are,
                        // open our for loop, register the model information,
                        // and then visit the children of the <for /> tag
                        if (endExpr instanceof ScopedExpression) {
                            // probably a $model/$data reference...
                            Expression allOfRoot = ((ScopedExpression) endExpr)
                                .getRootScope();
                            if (generator.isModelReference(allOfRoot)) {
                                GeneratedUiField modelInfo = (GeneratedUiField) endExpr.getExtras().get(
                                    EXTRA_MODEL_INFO);
                                if (modelInfo == null) {
                                    throw new IllegalArgumentException(
                                        "Complex model expressions not yet supported by <for /> tag."
                                            +"\nYou sent " + tools.debugNode(startExpr) + "; which was converted to " + tools.debugNode(endExpr));
                                }
                                String type = toDom.addImport(ASTHelper.extractGeneric(modelInfo.getMemberType()));
                                String call = tools.resolveString(ctx, endExpr);
                                final Type memberType = modelInfo.getMemberType();
                                if (memberType.hasRawType("IntTo")) {
                                    call = call + ".forEach()"; // ew.  But. well, it is what it is.
                                    // TODO: abstract this into something that understands the member type
                                }
                                toDom.append(type).append(" ").append(asName)
                                    .append(" : ").append(call).println(") {")
                                    .indent();

                                // Save our vars to ctx state.
                                // Since we are running inside a for loop,
                                // we want the var to point to the named instance we just used.
                                NameExpr var = new NameExpr(asName);
                                var.setExtras(endExpr.getExtras());
                                var.addExtra(UiConstants.EXTRA_FOR_LOOP_VAR, true);
                                Do undo = ctx.addToContext(asName, var);
//                                // Now, we can visit children
//                                if (index.isPresent()) {
//                                    String indexName = tools.resolveString(ctx, index.get().getExpression());
//                                    final UiBodyExpr body = getAst().getBody();
//                                    if (body != null) {
//                                        int cnt = 0;
//                                        for (Expression child : body.getChildren()) {
//                                            final Do indexUndo = ctx.addToContext(
//                                                indexName,
//                                                IntegerLiteralExpr.intLiteral(cnt++)
//                                            );
//                                            // child.addExtra(EXTRA_GENERATE_MODE, "");
//                                            child.accept(this, getAst());
//                                            indexUndo.done();
//                                        }
//
//                                    }
//                                } else {
//                                    // No index; just visit children.
//                                    final UiBodyExpr body = getAst().getBody();
//                                    if (body != null) {
//                                        super.visit(body, n);
//                                    }
//                                }

                                undo.done();

                                toDom.outdent();
                                toDom.println("}");
                                return;
                            }
                            throw new IllegalArgumentException("Non-model based for loop expressions not yet supported."
                                +"\nYou sent " + tools.debugNode(startExpr) + "; which was converted to " + tools.debugNode(endExpr));
                        }
                        throw new IllegalArgumentException("Non-ScopedExpression based for loop expressions not yet supported."
                            +"\nYou sent " + tools.debugNode(startExpr) + "; which was converted to " + tools.debugNode(endExpr));
                    })
                    .readIfAbsent(noAs->{
                        throw new IllegalArgumentException("For now, all <for /> tags *must* have an as=nameToReferenceItems attribute");
                    });
            })
            .readIfAbsentUnsafe(noAllOf->{
                throw new IllegalArgumentException("For now, all <for /> tags *must* have an allOf= attribute");
            });
        return null;
    }
}
