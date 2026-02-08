package net.wti.dsl.parser;

import net.wti.dsl.shape.DslSchemaShape;
import net.wti.dsl.type.DslType;
import net.wti.lang.parser.ast.expr.Expression;
import net.wti.lang.parser.ast.expr.JsonContainerExpr;
import net.wti.lang.parser.ast.expr.JsonPairExpr;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import xapi.fu.Pointer;
import xapi.fu.log.Log;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

///
/// AbstractDslUtility:
///
/// A place to collect handy, reusable methods for inspecting ast / processing xapi dsl.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 16/12/2025 @ 01:59
public interface DslUtility {
    DslModel getDslModel();

    default JsonContainerExpr getDslElements() {
        final DslModel dslRoot = getDslModel();
        final UiAttrExpr elementsAttr = dslRoot.getRoot().getAttribute("elements")
                .ifAbsentThrow(() ->
                        new IllegalArgumentException("xapi-dsl root is missing elements= attribute"))
                .get();

        final Expression expr = elementsAttr.getExpression();
        if (!(expr instanceof JsonContainerExpr)) {
            throw new IllegalArgumentException(
                    "xapi-dsl elements= must be a json array of <element-def>; got: " +
                            expr.getClass().getSimpleName());
        }

        final JsonContainerExpr json = (JsonContainerExpr) expr;
        if (!json.isArray()) {
            throw new IllegalArgumentException("xapi-dsl elements= must be a json array");
        }
        return json;
    }

    ///
    /// Extracts a per-element mapping of attribute name -> compiled DslType.
    ///
    /// This reads each <element-def attributes={ ... }/> entry in the root `elements=[ ... ]` list
    /// and compiles attribute value expressions into `DslType` objects.
    ///
    default Map<String, Map<String, DslType>> extractAttributeTypes() {
        final JsonContainerExpr json = getDslElements();

        final Map<String, Map<String, DslType>> elementAttrTypes = new LinkedHashMap<>();

        json.getValues()
                .filterInstanceOf(UiContainerExpr.class)
                .forAll(def -> {
                    if (!"element-def".equals(def.getName())) {
                        return;
                    }

                    final UiAttrExpr nameAttr = def.getAttribute("name")
                            .ifAbsentThrow(() -> new IllegalArgumentException(
                                    "element-def missing name= attribute: " + def.toSource()))
                            .get();
                    final String elementName = nameAttr.getString(false, true);

                    final UiAttrExpr attrsAttr = def.getAttribute("attributes")
                            .ifAbsentThrow(() -> new IllegalArgumentException(
                                    "element-def " + elementName + " missing attributes= map"))
                            .get();

                    final Expression attrsExpr = attrsAttr.getExpression();
                    if (!(attrsExpr instanceof JsonContainerExpr)) {
                        throw new IllegalArgumentException(
                                "attributes= for element-def " + elementName + " must be json; got: " +
                                        attrsExpr.getClass().getSimpleName());
                    }

                    final JsonContainerExpr attrsJson = (JsonContainerExpr) attrsExpr;
                    if (attrsJson.isArray()) {
                        throw new IllegalArgumentException(
                                "attributes= for element-def " + elementName + " must be a json object/map, not an array");
                    }

                    final LinkedHashMap<String, DslType> types = new LinkedHashMap<>();
                    for (final JsonPairExpr pair : attrsJson.getPairs()) {
                        final String attrName = pair.getKeyString();
                        final Expression typeExpr = pair.getValueExpr();
                        final DslType type = DslTypeExprParser.parse(typeExpr);
                        types.put(attrName, type);
                    }

                    elementAttrTypes.put(elementName, types);
                });

        return elementAttrTypes;
    }

    default String getRootTagName() {
        return getDslModel().getRoot().getAttribute("rootTag")
                .mapNullSafe(attr -> attr.getString(false, true))
                .ifAbsentReturn("root");
    }

    ///
    /// Internal shape extracted from the <xapi-dsl> root:
    ///  - which element tag is considered "root"
    ///  - mapping element tag -> allowed attribute names.
    ///
    default DslSchemaShape extractRootShape() {
        String explicitRootTag = getRootTagName();

        JsonContainerExpr json = getDslElements();

        Map<String, Set<String>> elements = new LinkedHashMap<>();
        Pointer<String> rootTag = Pointer.pointerTo(null);

        json.getValues()
                .filterInstanceOf(UiContainerExpr.class)
                .forAll(def -> {
                    if (!"element-def".equals(def.getName())) {
                        Log.tryLog(DslGraphMetaModelBuilder.class, this, Log.LogLevel.INFO,
                                "Ignoring non element-def entry in elements= array:", def.toSource());
                        return;
                    }

                    UiAttrExpr nameAttr = def.getAttribute("name")
                            .ifAbsentThrow(() -> new IllegalArgumentException(
                                    "element-def missing name= attribute: " + def.toSource()))
                            .get();
                    String elementName = nameAttr.getString(false, true);

                    UiAttrExpr attrsAttr = def.getAttribute("attributes")
                            .ifAbsentThrow(() -> new IllegalArgumentException(
                                    "element-def " + elementName + " missing attributes= map"))
                            .get();

                    Set<String> allowedAttrs = new LinkedHashSet<>();
                    Expression attrsExpr = attrsAttr.getExpression();
                    if (attrsExpr instanceof JsonContainerExpr) {
                        JsonContainerExpr attrsJson = (JsonContainerExpr) attrsExpr;
                        if (!attrsJson.isArray()) {
                            for (JsonPairExpr pair : attrsJson.getPairs()) {
                                allowedAttrs.add(pair.getKeyString());
                            }
                        } else {
                            Log.tryLog(DslGraphMetaModelBuilder.class, this, Log.LogLevel.WARN,
                                    "Attributes for element-def", elementName,
                                    "are an array, expected map; treating as empty");
                        }
                    } else {
                        Log.tryLog(DslGraphMetaModelBuilder.class, this, Log.LogLevel.WARN,
                                "Attributes for element-def", elementName,
                                "are not json; got:", attrsExpr.getClass().getSimpleName());
                    }

                    elements.put(elementName, allowedAttrs);
                    Log.tryLog(DslGraphMetaModelBuilder.class, this, Log.LogLevel.DEBUG,
                            "Discovered element-def", elementName, "attrs:", allowedAttrs);

                    if (rootTag.out1() == null) {
                        if (explicitRootTag.equals(elementName)) {
                            rootTag.in(elementName);
                        }
                    }
                });

        if (rootTag.out1() == null) {
            throw new IllegalArgumentException(
                    "xapi-dsl rootTag=\"" + explicitRootTag +
                            "\" does not match any element-def; defined: " + elements.keySet());
        }

        return new DslSchemaShape(rootTag.out1(), elements);
    }


}
