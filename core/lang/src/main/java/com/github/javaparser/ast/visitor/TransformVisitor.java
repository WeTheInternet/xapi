/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2015 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

package com.github.javaparser.ast.visitor;

import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.TemplateLiteralExpr;
import com.github.javaparser.ast.plugin.Transformer;
import xapi.fu.Printable;
import xapi.source.X_Source;

import static com.github.javaparser.ast.plugin.Transformer.DO_NOT_PRINT;

import java.util.Arrays;

public class TransformVisitor extends DumpVisitor {

  private final Transformer transformer;

  public TransformVisitor() {
    this(new Transformer());
  }

  public TransformVisitor(Transformer transformer) {
    this.transformer = transformer;
  }

  @Override
  public void visit(TemplateLiteralExpr n, Object arg) {
    // Lets transform this template into valid java!
    final Comment comment = n.getComment();
    printJavaComment(comment, arg);

    String template = transformer.onTemplateStart(printer, n);
    if (DO_NOT_PRINT.equals(template)) {
      transformer.onTemplateEnd(printer);
      return;
    }
    normalizeToString(printer, template);
    transformer.onTemplateEnd(printer);
  }

  public static void normalizeToString(Printable printer, String template) {
    printer.print("\"");
    if (template.isEmpty()) {
      printer.print("\"");
    } else {
      String[] lines = normalizeLines(template);
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        printer.print(X_Source.escape(line));
        if (i < lines.length - 1) {
          printer.println("\\n\" +");
        }
        printer.print("\"");
      }
    }
  }

  private static String[] normalizeLines(String template) {
    // TODO: sourcemapping for changed indices
    template = X_Source.normalizeNewlines(template);
    if (template.charAt(0) == '\n') {
      // an opening tick that is immediately followed by a newline should be ignored.
      template = template.substring(1);
    }
    if (template.trim().isEmpty()) {
      return new String[0];
    }

    final String[] lines = template.split("\n");
    int numLeadingWhitespace = Integer.MAX_VALUE;
    int numTrailingWhitespace = Integer.MAX_VALUE;
    for (String line : lines) {
      if (line.isEmpty()) {
        continue;
      }
      int numFound = 0;
      while(line.length() > numFound && Character.isWhitespace(line.charAt(numFound))) {
        numFound ++;
      }
      numLeadingWhitespace = Math.min(numLeadingWhitespace, numFound);

      int index = line.length();
      numFound = 0;
      while (index --> 0 && Character.isWhitespace(line.charAt(index))) {
        numFound ++;
      }
      numTrailingWhitespace = Math.min(numTrailingWhitespace, numFound);

      if (numLeadingWhitespace == 0 && numTrailingWhitespace == 0) {
        break;
      }
    }
    final boolean trimEnd = numTrailingWhitespace > 0 && numTrailingWhitespace != Integer.MAX_VALUE;
    final boolean trimStart = numLeadingWhitespace > 0 && numLeadingWhitespace != Integer.MAX_VALUE;
      for (int i = lines.length; i-->0; ) {
        String line = lines[i];
        if (line.isEmpty()) {
          continue;
        }
        if (trimStart) {
          if (trimEnd) {
            lines[i] = line.substring(numLeadingWhitespace, line.length() - numTrailingWhitespace);
          } else {
            lines[i] = line.substring(numLeadingWhitespace);
          }
        } else if (trimEnd) {
            lines[i] = line.substring(0, line.length() - numTrailingWhitespace);
        }
    }
    if (lines[lines.length-1].isEmpty()) {
      return Arrays.copyOf(lines, lines.length-1);
    }
    return lines;
  }

}
