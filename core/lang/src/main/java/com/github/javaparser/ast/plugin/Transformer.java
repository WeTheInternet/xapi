package com.github.javaparser.ast.plugin;

import com.github.javaparser.ast.expr.TemplateLiteralExpr;
import xapi.fu.Printable;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/11/16.
 */
public class Transformer {
  public static final String DO_NOT_PRINT = "\0\0\0";

  public Transformer(){}

  public String onTemplateStart(Printable printer, TemplateLiteralExpr template) {
    return template.getValue();
  }

  public void onTemplateEnd(Printable printer) {
  }

}
