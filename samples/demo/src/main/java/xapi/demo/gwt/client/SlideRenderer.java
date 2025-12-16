package xapi.demo.gwt.client;

import net.wti.lang.parser.ASTHelper;
import net.wti.lang.parser.ast.expr.Expression;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import net.wti.lang.parser.ast.visitor.ComposableXapiVisitor;
import net.wti.lang.parser.ast.visitor.GenericVisitorAdapter;
import de.mocra.cy.shared.ast.*;
import xapi.demo.gwt.client.ui.BoxPosition;
import xapi.demo.gwt.client.ui.BoxSize;
import xapi.dev.source.DomBuffer;
import xapi.except.NotYetImplemented;
import xapi.log.X_Log;
import xapi.source.X_Source;

import java.io.StringReader;

import static xapi.fu.itr.ArrayIterable.iterate;

/**
 * Used to transformed parsed xapi AST into functional HTML.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/24/17.
 */
public class SlideRenderer {

    public void renderSlide(DomBuffer into, UiContainerExpr slide) {
        // We'll render the slide element ourselves, if it hasn't been rendered for us.
        final DomBuffer out;
        if ("xapi-slide".equals(into.getTagName())) {
            out = into;
        } else {
            out = into.makeTag("xapi-slide");
        }
        slide.getAttribute("id")
            .mapNullSafe(UiAttrExpr::getExpression)
            .mapNullSafe(ASTHelper::extractStringValue)
            .readIfPresent(out::setId);

        final ComposableXapiVisitor<DomBuffer> visitor = new ComposableXapiVisitor<>();
        visitor
            .withUiContainerExpr((tag, buffer)->{
                renderSlideChild(out, tag, visitor, slide);
                return false;
            })
            .visit(slide.getBody(), out);
    }

    protected void renderSlideChild(
        DomBuffer into,
        UiContainerExpr tag,
        ComposableXapiVisitor<DomBuffer> visitor,
        UiContainerExpr slide
    ) {
        switch (tag.getName()) {
            case "box":
                renderBox(into, tag, visitor, slide);
                break;
            case "item-list":
                renderItemList(into, tag, visitor);
                break;
            case "item":
                renderItem(into, tag, visitor);
                break;
            default:
                throw new NotYetImplemented("Tag " + tag.getName() + " not yet supported");
        }
    }

    private void renderBox(
        DomBuffer into,
        UiContainerExpr tag,
        ComposableXapiVisitor<DomBuffer> visitor,
        UiContainerExpr slide
    ) {
        final DomBuffer box = into.makeTag("xapi-box");
        for (UiAttrExpr attr : tag.getAttributes()) {
            switch (attr.getNameString()) {
                case "id":
                    String val = serializeValue(attr);
                    box.setId(fixupId(val));
                    break;
                case "text":
                    val = serializeValue(attr);
                    box.append(renderText(val, attr, slide));
                    break;
                case "position":
                    val = serializeValue(attr);
                    try {
                        assert !BoxPosition.valueOf(val).equals(1) :
                            "The only reason for this to fail is if BoxPosition.valueOf throws" ;
                    } catch (Throwable t) {
                        String location = slide.getExtra("location");
                        if (location != null) {
                            X_Log.error("Error at: " + X_Source.pathToLogLink(location, attr.getBeginLine()));
                        }
                        X_Log.error(SlideRenderer.class, "Invalid BoxPosition", val, "not in " + iterate(BoxPosition.values()).join("[", ",", "]"));
                        throw t;
                    }
                    box.setAttribute("position", val);
                    break;
                case "size":
                    val = serializeValue(attr);
                    switch (val) {
                        case "largest":
                        case "large":
                        case "normal":
                        case "small":
                        case "smallest":
                            // TODO: add class from stylesheet...
                            box.setAttribute("size", val);
                            break;
                        default:
                        String location = slide.getExtra("location");
                        if (location != null) {
                            X_Log.error("Error at: " + X_Source.pathToLogLink(location, attr.getBeginLine()));
                        }
                        X_Log.error(SlideRenderer.class, "Invalid BoxSize", val, "not in " + iterate(BoxSize.values()).join("[", ",", "]"));
                        throw new IllegalArgumentException("position=" + val + " not supported");
                    }
                    break;
                case "title":
                    val = serializeValue(attr);
                    final DomBuffer title = into.makeTagAtBeginning("xapi-title");
                    title.append(val);
                    break;
                case "image":
                case "onClick":
                    // hrm. lets not go here just yet...
                    break;
                default:
                    throw new NotYetImplemented("no box render support for attribute " + attr.getNameString());
            }
        }

    }

    /**
     * Serializes an attributes value using {@link ASTHelper#extractStringValue(Expression)}.
     *
     * protected so you may override it for something with more brains.
     */
    protected String serializeValue(UiAttrExpr attr) {
        return ASTHelper.extractStringValue(attr.getExpression());
    }

    private String renderText(String val, UiAttrExpr attr, UiContainerExpr slide) {
        if (val.trim().isEmpty()) {
            return val;
        }
        try {
            final WtiContentAst content = new WtiAst(new StringReader(val+"\n\n")).ContentAst();
            // alright, now we render this content as links...
            return renderContent(content);
        } catch (ParseException | TokenMgrError e) {
            String location = slide.getExtra("location");
            if (location != null) {
                // figure out delta of wti error within attribute's value
                int delta;
                if (e instanceof ParseException) {
                    delta = ((ParseException) e).currentToken.beginLine;
                } else {
                    // when it's a token manager error, we won't get the line number (for now)
                    delta = 1;
                }
                X_Log.error("Invalid WTI text at " + X_Source.pathToLogLink(location,
                    attr.getBeginLine() + delta - 1));
            }
            X_Log.error(SlideRenderer.class, "Invalid WTI text: ", val);
            throw new RuntimeException(e);
        }
    }

    private String renderContent(WtiContentAst content) {
        DomBuffer out = new DomBuffer();
        // TODO: apply formatting as well...
        content.jjtAccept(new WtiAstDefaultVisitor(){

            @Override
            public <T> VisitorContext<T> visitWtiContentAst(WtiContentAst node, VisitorContext<T> data) {
                if (node.getPermalink() != null) {
                    node.getPermalink().jjtAccept(this, data);
                }
                if (node.getTitle() != null) {
                    if (node.getPermalink() != null) {
                        out.append(" ");
                    }
                    out.makeAnchorInline()
                       .setHref("javascript:void(0)")
                       .setAttribute("onclick", "return XapiSlides.click(this)")
                       .append(node.getTitle());
                }
                return super.visitWtiContentAst(node, data);
            }

            @Override
            public <T> VisitorContext<T> visitWtiLinkAst(
                WtiLinkAst node, VisitorContext<T> data
            ) {
                out.makeAnchorInline()
                   .setHref("javascript:void(0)")
                   .setAttribute("onclick", "return XapiSlides.click(this)")
                   .append(node.getTitle());
                return super.visitWtiLinkAst(node, data);
            }

            @Override
            public <T> VisitorContext<T> visitWtiTextAst(
                WtiTextAst node, VisitorContext<T> data
            ) {
                out.append(node.getText());
                return super.visitWtiTextAst(node, data);
            }

        }, null);
        return out.toSource();
    }

    /**
     * Allow subclasses to do things like id prefixing.
     */
    protected String fixupId(String id) {
        return id.startsWith("\"") || id.startsWith("`") ? id.substring(1, id.length()-1) : id;
    }

    private void renderItemList(DomBuffer into, UiContainerExpr tag, ComposableXapiVisitor<DomBuffer> visitor) {
        final DomBuffer box = into.makeTag("xapi-items");
        String id = tag.getAttributeRequiredString("id");
        box.setId(fixupId(id));
    }

    private void renderItem(DomBuffer into, UiContainerExpr tag, ComposableXapiVisitor<DomBuffer> visitor) {
        final DomBuffer box = into.makeTag("xapi-item");
        String id = tag.getAttributeRequiredString("id");
        box.setId(fixupId(id));
    }

    private final class SlideRenderingVisitor extends GenericVisitorAdapter<DomBuffer, DomBuffer> {

    }

}
