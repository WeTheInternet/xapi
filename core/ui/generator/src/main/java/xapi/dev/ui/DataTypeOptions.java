package xapi.dev.ui;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.JsonPairExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.source.api.IsType;
import xapi.source.impl.ImmutableType;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/22/16.
 */
public class DataTypeOptions {

  private String collection;
  private String factory;
  private String type;
  private String adder;
  private StringTo<IsType> fieldTypes;

  public DataTypeOptions() {
    fieldTypes = X_Collect.newStringMap(IsType.class);
  }

  public DataTypeOptions fromAnnotation(
      ClassBuffer cb,
      JsonContainerExpr json,
      AnnotationExpr anno,
      boolean searchTypes
  ) {
    if (anno != null) {
      for (MemberValuePair member : anno.getMembers()) {
        switch (member.getName()) {
          case "collection":
          case "value":
            assert collection == null : "Do not supply both `collection` and `value` to @Type annotation " + anno;
            collection = ASTHelper.extractAnnoValue(member);
            break;
          case "type":
            type = ASTHelper.extractAnnoValue(member);
            break;
          case "adder":
            adder = ASTHelper.extractAnnoValue(member);
            break;
          case "factory":
            factory = ASTHelper.extractAnnoValue(member);
            break;
          case "auto":
            searchTypes = true;
            break;
        }
      }
    }
    // attempt to determine generic type, using an extremely lazy peek at values.
    // if more complex type mapping is required, we should defer to a more comprehensive
    // type resolving system than the hack below.
    Expression best = null;
    for (JsonPairExpr pair : json.getPairs()) {
      final Expression val = pair.getValueExpr();
      if (best == null) {
        best = val;
      } else if (val.getClass() != best.getClass() && !(val instanceof NullLiteralExpr)) {
        best = null;
        break;
      }
      fieldTypes.put(pair.getKeyString(), extractType(pair, json));
    }
    if (type == null && searchTypes) {
      if (best != null) {
        if (best instanceof IntegerLiteralExpr) {
          type = "Integer";
        } else if (best instanceof LongLiteralExpr) {
          type = "Long";
        } else if (best instanceof DoubleLiteralExpr) {
          type = "Double";
        } else if (best instanceof CharLiteralExpr) {
          type = "Character";
        } else if (best instanceof BooleanLiteralExpr) {
          type = "Boolean";
        } else if (best instanceof StringLiteralExpr) {
          type = "String";
        }
      }
    }
    if (type == null) {
      type = "Object";
    }
    if (collection == null) {
      collection = (defaultCollectionType(json)).getName();
    }
    if (factory == null) {
      if (collection.startsWith("xapi")) {
        String factoryCls = cb.addImport(X_Collect.class);
        factory = factoryCls + ".new" + (json.isArray() ? "List" : "StringMap") + "($type.class)";
      } else {
        factory = "new $type()";
      }
    }
    if (adder == null) {
      adder = json.isArray() ? "add" : "put";
    }
    return this;
  }

  protected IsType extractType(Expression val) {
    if (val instanceof LiteralExpr) {
      if (val instanceof IntegerLiteralExpr) {
        return new ImmutableType("", "int");
      } else if (val instanceof LongLiteralExpr) {
        return new ImmutableType("", "long");
      } else if (val instanceof DoubleLiteralExpr) {
        return new ImmutableType("", "double");
      } else if (val instanceof CharLiteralExpr) {
        return new ImmutableType("", "char");
      } else if (val instanceof BooleanLiteralExpr) {
        return new ImmutableType("", "boolean");
      } else if (val instanceof StringLiteralExpr) {
        return new ImmutableType("java.lang", "String");
      }
      // TODO: arrays, ui containers, annotated elements, constructors, etc...
    }
    return new ImmutableType("java.lang", "Object");
  }

  protected IsType extractType(JsonPairExpr pair, JsonContainerExpr json) {
    final Expression val = pair.getValueExpr();
    return extractType(val);
  }

  protected Class<?> defaultCollectionType(JsonContainerExpr json) {
    return json.isArray() ? IntTo.class : StringTo.class;
  }

  public String getCollection() {
    return collection;
  }

  public String getFactory() {
    return factory;
  }

  public String getType() {
    return type;
  }

  public String getAdder() {
    return adder;
  }

  public StringTo<IsType> getFieldTypes() {
    return fieldTypes;
  }
}
