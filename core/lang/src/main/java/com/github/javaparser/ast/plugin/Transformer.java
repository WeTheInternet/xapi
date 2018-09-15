package com.github.javaparser.ast.plugin;

import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.TemplateLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.TransformVisitor;
import xapi.fu.Printable;
import xapi.source.X_Source;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/11/16.
 */
public class Transformer {
  public static final String DO_NOT_PRINT = "\0\0\0";
  private boolean shouldQuote;

  public Transformer(){}

  public String onTemplateStart(Printable printer, TemplateLiteralExpr template) {
    return template.getValue();
  }

  public String resolveName(Printable printer, NameExpr name) {
    return name.getName();
  }

  public void onTemplateEnd(Printable printer) {
  }

  public String resolveType(ClassOrInterfaceType type) {
    return type.getName();
  }

  public String resolveTypeParamName(TypeParameter param) {
    return param.getName();
  }

  public void normalizeToString(Printable printer, String template) {
    boolean isQuote = isShouldQuote();
    if (isQuote) {
      printer.print("\"");
    }
    if (template.isEmpty()) {
      if (isQuote) {
        printer.print("\"");
      }
    } else {
      String[] lines = TransformVisitor.normalizeLines(template);
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        printer.print(X_Source.escape(line));
        if (i < lines.length - 1) {
          printer.println("\\n\" +");
        }
        if (isQuote) {
          printer.print("\"");
        }
      }
    }
  }

  public boolean isShouldQuote() {
    return shouldQuote;
  }

  public void setShouldQuote(boolean shouldQuote) {
    this.shouldQuote = shouldQuote;
  }
}
