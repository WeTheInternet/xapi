package net.wti.dsl.schema;

import net.wti.dsl.parser.DslModel;
import net.wti.dsl.parser.DslTypeExprParser;
import net.wti.dsl.type.DslType;
import net.wti.lang.parser.ast.expr.Expression;
import net.wti.lang.parser.ast.expr.JsonContainerExpr;
import net.wti.lang.parser.ast.expr.JsonPairExpr;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import xapi.fu.itr.MappedIterable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

///
/// DslSchemaCompiler:
///
/// Compiles a parsed DSL definition (a {@link DslModel} rooted at <xapi-dsl>)
/// into an immutable {@link DslSchema} graph suitable for:
///  - code generation (DSL-specific analyzers/builders/validators),
///  - schema-time validation of the DSL definition,
///  - optional generic validation in tests/tools.
///
/// This compiler focuses on producing an immutable, easy-to-consume object model.
/// It does not attempt to perform “full” typeRef resolution or advanced templating yet.
///
/// ## Supported definition features (first pass)
/// - root tag: read from the DSL model
/// - reusable attributes: read from <xapi-dsl attributes={...}/> if present
/// - per-element attributes: read from <element-def attributes={...}/>
/// - per-element children: read from <element-def elements={...}/>
///
/// ## Attribute merging semantics (first pass)
/// - Top-level attributes are canonical; elements will reuse them by name when compatible.
/// - If an element defines an attribute name that already exists top-level, the definition must
///   be compatible (same required/default/type “shape”), otherwise an exception is thrown.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 03:12
public final class DslSchemaCompiler {

    public DslSchema compile(final DslModel model) {
        Objects.requireNonNull(model, "model must not be null");
        final UiContainerExpr root = model.getRoot();

        final Map<String, DslSchemaAttribute> schemaAttrs = new LinkedHashMap<>();
        final Map<String, DslSchemaElement> schemaElements = new LinkedHashMap<>();

        // 1) Top-level reusable attributes (optional)
        final UiAttrExpr topAttrs = root.getAttribute("attributes").ifAbsentReturn(null);
        if (topAttrs != null) {
            final Expression expr = topAttrs.getExpression();
            if (!(expr instanceof JsonContainerExpr) || ((JsonContainerExpr) expr).isArray()) {
                throw new IllegalArgumentException("xapi-dsl attributes= must be a json object/map; got: " + expr.toSource());
            }
            final JsonContainerExpr obj = (JsonContainerExpr) expr;
            for (final JsonPairExpr pair : obj.getPairs()) {
                final String attrName = pair.getKeyString();
                final DslSchemaAttribute attr = compileAttribute(attrName, pair.getValueExpr());
                putSchemaAttribute(schemaAttrs, attr);
            }
        }

        // 2) Elements: compile element-def entries
        final UiAttrExpr elementsAttr = root.getAttribute("elements")
                .ifAbsentThrow(() -> new IllegalArgumentException("xapi-dsl root is missing elements= attribute"))
                .get();

        final Expression elementsExpr = elementsAttr.getExpression();
        if (!(elementsExpr instanceof JsonContainerExpr) || !((JsonContainerExpr) elementsExpr).isArray()) {
            throw new IllegalArgumentException("xapi-dsl elements= must be a json array; got: " + elementsExpr.toSource());
        }

        final JsonContainerExpr elementsArr = (JsonContainerExpr) elementsExpr;
        elementsArr.getValues()
                .filterInstanceOf(UiContainerExpr.class)
                .forAll(def -> {
                    if (!"element-def".equals(def.getName())) {
                        return;
                    }
                    final String tagName = def.getAttribute("name")
                            .ifAbsentThrow(() -> new IllegalArgumentException("element-def missing name= attribute: " + def.toSource()))
                            .get().getString(false, true);

                    final Map<String, DslSchemaAttribute> elementAttrs = compileElementAttributes(def, schemaAttrs, tagName);
                    final Map<String, DslType> elementKids = compileElementChildren(def, tagName);

                    final DslSchemaElement element = new DslSchemaElement(
                            singletonSource(def),
                            tagName,
                            elementAttrs,
                            elementKids
                    );
                    if (schemaElements.containsKey(tagName)) {
                        throw new IllegalArgumentException("Duplicate element-def for '" + tagName + "'");
                    }
                    schemaElements.put(tagName, element);
                });

        // 3) Root tag sanity
        final String rootTag = model.getRoot().getAttribute("rootTag")
                .mapNullSafe(attr -> attr.getString(false, true))
                .ifAbsentReturn("root");

        if (!schemaElements.containsKey(rootTag)) {
            throw new IllegalArgumentException("rootTag='" + rootTag + "' does not match any element-def; defined: " + schemaElements.keySet());
        }

        return new DslSchema(
                singletonSource(root),
                model.getName(),
                model.getPackageName(),
                rootTag,
                schemaAttrs,
                schemaElements
        );
    }

    private static Map<String, DslSchemaAttribute> compileElementAttributes(
            final UiContainerExpr elementDef,
            final Map<String, DslSchemaAttribute> schemaAttrs,
            final String elementName
    ) {
        final UiAttrExpr attrsAttr = elementDef.getAttribute("attributes").ifAbsentReturn(null);
        if (attrsAttr == null) {
            return Collections.emptyMap();
        }

        final Expression attrsExpr = attrsAttr.getExpression();
        if (!(attrsExpr instanceof JsonContainerExpr) || ((JsonContainerExpr) attrsExpr).isArray()) {
            throw new IllegalArgumentException("attributes= for element-def '" + elementName + "' must be a json object/map; got: " + attrsExpr.toSource());
        }

        final JsonContainerExpr attrsObj = (JsonContainerExpr) attrsExpr;
        final LinkedHashMap<String, DslSchemaAttribute> out = new LinkedHashMap<>();

        for (final JsonPairExpr pair : attrsObj.getPairs()) {
            final String attrName = pair.getKeyString();
            final DslSchemaAttribute compiled = compileAttribute(attrName, pair.getValueExpr());

            // ensure schema-level registry has a compatible attribute for reuse-by-name
            if (schemaAttrs.containsKey(attrName)) {
                requireCompatible(schemaAttrs.get(attrName), compiled, "attribute '" + attrName + "' on element '" + elementName + "'");
                out.put(attrName, schemaAttrs.get(attrName));
            } else {
                // promote to schema-level reusable attribute by default
                schemaAttrs.put(attrName, compiled);
                out.put(attrName, compiled);
            }
        }

        return Collections.unmodifiableMap(out);
    }

    private static Map<String, DslType> compileElementChildren(final UiContainerExpr elementDef, final String elementName) {
        final UiAttrExpr kidsAttr = elementDef.getAttribute("elements").ifAbsentReturn(null);
        if (kidsAttr == null) {
            return Collections.emptyMap();
        }

        final Expression kidsExpr = kidsAttr.getExpression();
        if (!(kidsExpr instanceof JsonContainerExpr) || ((JsonContainerExpr) kidsExpr).isArray()) {
            throw new IllegalArgumentException("elements= for element-def '" + elementName + "' must be a json object/map; got: " + kidsExpr.toSource());
        }

        final JsonContainerExpr kidsObj = (JsonContainerExpr) kidsExpr;
        final LinkedHashMap<String, DslType> out = new LinkedHashMap<>();
        for (final JsonPairExpr pair : kidsObj.getPairs()) {
            final String childTag = pair.getKeyString();
            final DslType childType = DslTypeExprParser.parse(pair.getValueExpr());
            if (out.containsKey(childTag)) {
                throw new IllegalArgumentException("Duplicate child tag '" + childTag + "' in element-def '" + elementName + "'");
            }
            out.put(childTag, childType);
        }
        return Collections.unmodifiableMap(out);
    }

    private static void putSchemaAttribute(final Map<String, DslSchemaAttribute> schemaAttrs, final DslSchemaAttribute attr) {
        final DslSchemaAttribute existing = schemaAttrs.get(attr.getName());
        if (existing == null) {
            schemaAttrs.put(attr.getName(), attr);
            return;
        }
        requireCompatible(existing, attr, "schema attribute '" + attr.getName() + "'");
        // keep the original canonical instance
    }

    private static void requireCompatible(final DslSchemaAttribute existing, final DslSchemaAttribute incoming, final String context) {
        if (existing.isRequired() != incoming.isRequired()) {
            throw new IllegalArgumentException("Conflicting required= for " + context);
        }
        // Default value expressions are AST nodes; we only enforce “both present/both absent” here.
        // If you want stricter behavior, we can compare serialized source later.
        final boolean existingHasDefault = existing.getDefaultValueExpr() != null;
        final boolean incomingHasDefault = incoming.getDefaultValueExpr() != null;
        if (existingHasDefault != incomingHasDefault) {
            throw new IllegalArgumentException("Conflicting default= presence for " + context);
        }
        // Type compatibility: first pass uses class equality (structural deep-compare can come later).
        if (!existing.getType().getClass().equals(incoming.getType().getClass())) {
            throw new IllegalArgumentException("Conflicting type for " + context + ": " +
                    existing.getType().getClass().getSimpleName() + " vs " +
                    incoming.getType().getClass().getSimpleName());
        }
    }

    private static DslSchemaAttribute compileAttribute(final String attrName, final Expression typeExprWithMetadata) {
        // Parse the actual type shape
        final DslType type = DslTypeExprParser.parse(typeExprWithMetadata);

        // Extract metadata if the value expression is a UiContainerExpr like <string required=true default=... />
        boolean required = false;
        Expression defaultExpr = null;

        if (typeExprWithMetadata instanceof UiContainerExpr) {
            final UiContainerExpr ui = (UiContainerExpr) typeExprWithMetadata;

            if (ui.hasAttribute("required")) {
                final String req = ui.getAttributeRequiredString("required");
                required = "true".equalsIgnoreCase(req);
            }
            if (ui.hasAttribute("default")) {
                defaultExpr = ui.getAttribute("default").get().getExpression();
            }
        }

        return new DslSchemaAttribute(
                singletonSource(typeExprWithMetadata),
                attrName,
                type,
                required,
                defaultExpr
        );
    }

    private static MappedIterable<Expression> singletonSource(final Expression expr) {
        return xapi.fu.itr.MappedIterable.mapped(Collections.<Expression>singletonList(expr));
    }
}
