package net.wti.lang.parser.ast.plugin;

import net.wti.lang.parser.ast.expr.UiExpr;
import xapi.fu.Printable;

/**
 * Used to allow custom transforms of ui elements,
 * so you can use any element type you want;
 * really, since you are generating code, you may target any of the many
 * gui platforms which run on java.
 *
 * Note: Generating valid Play-N code would take care of web, desktop, flash, android and ios:
 * https://github.com/playn/playn
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/11/16.
 */
public interface UiTranslatorPlugin {

  String transformUi(Printable printer, UiExpr ui);

}
