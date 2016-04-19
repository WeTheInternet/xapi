package com.github.javaparser.ast.plugin;

import com.github.javaparser.ast.expr.UiExpr;
import com.github.javaparser.ast.visitor.DumpVisitor.SourcePrinter;

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

  String transformUi(SourcePrinter printer, UiExpr ui);

}
