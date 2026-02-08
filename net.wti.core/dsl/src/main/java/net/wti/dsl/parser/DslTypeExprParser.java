package net.wti.dsl.parser;

import net.wti.dsl.type.DslType;
import net.wti.dsl.type.DslTypeBoolean;
import net.wti.dsl.type.DslTypeElement;
import net.wti.dsl.type.DslTypeEnum;
import net.wti.dsl.type.DslTypeInteger;
import net.wti.dsl.type.DslTypeJson;
import net.wti.dsl.type.DslTypeListOrMap;
import net.wti.dsl.type.DslTypeMany;
import net.wti.dsl.type.DslTypeMap;
import net.wti.dsl.type.DslTypeName;
import net.wti.dsl.type.DslTypeNamePair;
import net.wti.dsl.type.DslTypeNamedElement;
import net.wti.dsl.type.DslTypeOne;
import net.wti.dsl.type.DslTypeQualifiedName;
import net.wti.dsl.type.DslTypeRef;
import net.wti.dsl.type.DslTypeString;
import net.wti.dsl.type.DslTypeTypedMap;
import net.wti.lang.parser.ASTHelper;
import net.wti.lang.parser.ast.expr.*;
import xapi.fu.itr.MappedIterable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

///
/// DslTypeExprParser:
///
/// Converts xapi AST expressions representing schema type declarations into DslType objects.
///
/// Supports both:
///  - type constructors from the roadmap (one/many/map/typedMap/typeRef/element/namedElement)
///  - shape tags used by existing xapi-dsl schema files (list/map/element-ref/list-or-map)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 00:42
public final class DslTypeExprParser {

    private DslTypeExprParser() {}

    public static DslType parse(final Expression expr) {
        if (expr == null) {
            throw new NullPointerException("expr must not be null");
        }
        return parse(expr, singletonSource(expr));
    }

    private static MappedIterable<Expression> singletonSource(final Expression expr) {
        final List<Expression> one = Collections.<Expression>singletonList(expr);
        return MappedIterable.mapped(one);
    }

    private static DslType parse(final Expression expr, final MappedIterable<Expression> sourceAst) {
        if (expr instanceof UiContainerExpr) {
            return parseUiContainer((UiContainerExpr) expr, sourceAst);
        }
        if (expr instanceof MethodCallExpr) {
            return parseMethodCall((MethodCallExpr) expr, sourceAst);
        }
        if (expr instanceof NameExpr) {
            return parseName(((NameExpr) expr).getName(), sourceAst, expr);
        }
        throw new IllegalArgumentException("Unsupported type expression AST: " + expr.getClass().getName() + " :: " + expr.toSource());
    }

    private static DslType parseName(final String name, final MappedIterable<Expression> sourceAst, final Expression original) {
        if (name == null) {
            throw new IllegalArgumentException("Null type name for expr: " + original);
        }
        switch (name) {
            case "string":
                return new DslTypeString(sourceAst);
            case "name":
                return new DslTypeName(sourceAst);
            case "qualifiedName":
                return new DslTypeQualifiedName(sourceAst);
            case "bool":
            case "boolean":
                return new DslTypeBoolean(sourceAst);
            case "int":
            case "integer":
                return new DslTypeInteger(sourceAst);
            case "namePair":
                return new DslTypeNamePair(sourceAst);
            case "json":
                return new DslTypeJson(sourceAst);
            default:
                throw new IllegalArgumentException("Unknown type name: " + name + " from " + original.toSource());
        }
    }

    private static DslType parseUiContainer(final UiContainerExpr ui, final MappedIterable<Expression> sourceAst) {
        final String tag = ui.getName();

        if ("string".equals(tag) ||
                "name".equals(tag) ||
                "qualifiedName".equals(tag) ||
                "bool".equals(tag) ||
                "boolean".equals(tag) ||
                "int".equals(tag) ||
                "integer".equals(tag) ||
                "namePair".equals(tag) ||
                "json".equals(tag)) {
            return parseName(tag, sourceAst, ui);
        }

        if ("one".equals(tag)) {
            return new DslTypeOne(sourceAst, parseChildrenTypes(ui.getBody().getChildren()));
        }
        if ("many".equals(tag)) {
            return new DslTypeMany(sourceAst, parseChildrenTypes(ui.getBody().getChildren()));
        }
        if ("typedMap".equals(tag)) {
            final List<Expression> kids = ui.getBody().getChildren();
            if (kids.size() != 1 || !(kids.get(0) instanceof JsonContainerExpr) || ((JsonContainerExpr) kids.get(0)).isArray()) {
                throw new IllegalArgumentException("typedMap expects a single JSON object argument; got: " + kids);
            }
            final JsonContainerExpr obj = (JsonContainerExpr) kids.get(0);
            final LinkedHashMap<String, DslType> fields = new LinkedHashMap<>();
            for (final JsonPairExpr pair : obj.getPairs()) {
                final String key = pair.getKeyString();
                final DslType valueType = parse(pair.getValueExpr());
                fields.put(key, valueType);
            }
            return new DslTypeTypedMap(sourceAst, fields);
        }

        if ("element".equals(tag)) {
            final String name = requireAttrString(ui, "name");
            return new DslTypeElement(sourceAst, name);
        }

        if ("namedElement".equals(tag)) {
            final String name = requireAttrString(ui, "name");
            return new DslTypeNamedElement(sourceAst, name);
        }

        if ("typeRef".equals(tag)) {
            final String name = requireAttrString(ui, "name");
            return new DslTypeRef(sourceAst, name);
        }

        if ("element-ref".equals(tag)) {
            final String name = requireAttrString(ui, "name");
            return new DslTypeElement(sourceAst, name);
        }

        if ("list".equals(tag)) {
            final Expression elementExpr = requireAttrExpr(ui, "element");
            final DslType itemType = parse(coerceAttrExpr(elementExpr));
            return new DslTypeMany(sourceAst, Collections.<DslType>singletonList(itemType));
        }

        if ("map".equals(tag)) {
            final String keyTypeName = requireAttrString(ui, "keyType");
            final String valueTypeName = requireAttrString(ui, "valueType");
            final DslType keyType = parseName(keyTypeName, sourceAst, ui);
            final DslType valueType = parseName(valueTypeName, sourceAst, ui);
            return new DslTypeMap(sourceAst, keyType, Collections.<DslType>singletonList(valueType));
        }

        if ("list-or-map".equals(tag)) {
            final Expression listElementExpr = requireAttrExpr(ui, "listElement");
            final Expression mapValueExpr = requireAttrExpr(ui, "mapValue");
            final DslType listElementType = parse(coerceAttrExpr(listElementExpr));
            final DslType mapValueType = parse(coerceAttrExpr(mapValueExpr));
            return new DslTypeListOrMap(sourceAst, listElementType, mapValueType);
        }

        if ("enum".equals(tag)) {
            final Expression valuesExpr = requireAttrExpr(ui, "values");
            final Expression coerced = coerceAttrExpr(valuesExpr);
            if (!(coerced instanceof JsonContainerExpr) || !((JsonContainerExpr) coerced).isArray()) {
                throw new IllegalArgumentException("<enum> expects values=[\"a\", \"b\", ...]; got: " + coerced.toSource());
            }
            final JsonContainerExpr arr = (JsonContainerExpr) coerced;
            final ArrayList<String> values = new ArrayList<>();
            for (final Expression item : arr.getValues()) {
                final String str = ASTHelper.extractStringValue(item);
                if (str == null || str.isEmpty()) {
                    throw new IllegalArgumentException("<enum> values must be non-empty strings; got: " + item.toSource());
                }
                values.add(str);
            }
            return new DslTypeEnum(sourceAst, values);
        }

        throw new IllegalArgumentException("Unknown type tag <" + tag + "> from " + ui.toSource());
    }

    private static DslType parseMethodCall(final MethodCallExpr call, final MappedIterable<Expression> sourceAst) {
        final String name = call.getName();

        if ("one".equals(name)) {
            return new DslTypeOne(sourceAst, parseChildrenTypes(call.getArgs()));
        }
        if ("many".equals(name)) {
            return new DslTypeMany(sourceAst, parseChildrenTypes(call.getArgs()));
        }
        if ("map".equals(name)) {
            final List<DslType> args = parseChildrenTypes(call.getArgs());
            if (args.size() < 2) {
                throw new IllegalArgumentException("map(keyType, valueType...) expects at least 2 arguments");
            }
            final DslType keyType = args.get(0);
            final List<DslType> valueChoices = args.subList(1, args.size());
            return new DslTypeMap(sourceAst, keyType, valueChoices);
        }
        if ("typeRef".equals(name)) {
            if (call.getArgs().size() != 1) {
                throw new IllegalArgumentException("typeRef(...) expects exactly 1 argument; got " + call.getArgs().size());
            }
            final String alias = ASTHelper.extractStringValue(call.getArg(0));
            return new DslTypeRef(sourceAst, alias);
        }

        if (call.getArgs().isEmpty()) {
            return parseName(name, sourceAst, call);
        }

        throw new IllegalArgumentException("Unsupported type call " + name + "(...) from " + call.toSource());
    }

    private static List<DslType> parseChildrenTypes(final List<Expression> argsOrChildren) {
        final ArrayList<DslType> out = new ArrayList<>();
        for (final Expression e : argsOrChildren) {
            out.add(parse(e));
        }
        return out;
    }

    private static String requireAttrString(final UiContainerExpr ui, final String attrName) {
        if (!ui.hasAttribute(attrName)) {
            throw new IllegalArgumentException("Expected <" + ui.getName() + "> to have attribute " + attrName);
        }
        final String val = ui.getAttributeRequiredString(attrName);
        if (val == null || val.isEmpty()) {
            throw new IllegalArgumentException("Attribute " + attrName + " must not be empty on <" + ui.getName() + ">");
        }
        return val;
    }

    private static Expression requireAttrExpr(final UiContainerExpr ui, final String attrName) {
        if (!ui.hasAttribute(attrName)) {
            throw new IllegalArgumentException("Expected <" + ui.getName() + "> to have attribute " + attrName);
        }
        return ui.getAttribute(attrName).get().getExpression();
    }

    private static Expression coerceAttrExpr(final Expression expr) {
        if (expr instanceof UiAttrExpr) {
            return ((UiAttrExpr) expr).getExpression();
        }
        return expr;
    }
}
