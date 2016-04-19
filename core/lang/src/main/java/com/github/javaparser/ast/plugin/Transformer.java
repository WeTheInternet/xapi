package com.github.javaparser.ast.plugin;

import com.github.javaparser.ast.expr.TemplateLiteralExpr;
import com.github.javaparser.ast.visitor.DumpVisitor.SourcePrinter;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/11/16.
 */
public class Transformer {
  public static final String DO_NOT_PRINT = "\0\0\0";

  public Transformer(){}

  public String onTemplateStart(SourcePrinter printer, TemplateLiteralExpr template) {
    return template.getValue();
  }

  public void onTemplateEnd(SourcePrinter printer) {
  }

}
