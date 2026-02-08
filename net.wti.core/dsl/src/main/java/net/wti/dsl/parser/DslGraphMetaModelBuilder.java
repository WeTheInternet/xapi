package net.wti.dsl.parser;

import net.wti.dsl.shape.DslSchemaShape;
import net.wti.dsl.type.DslType;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import xapi.fu.log.Log;
import xapi.fu.log.Log.LogLevel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

///
/// DslGraphMetaModelBuilder:
///
/// Consumes a {@link DslModel} rooted at <xapi-dsl>
/// and produces an in-memory {@link DslGraphModel} describing
///  - the root tag,
///  - the declared element-def entries, and
///  - the attributes available on each element.
///
/// This meta model is used by the graph code generator to emit:
///  - <DslName>GraphFactory
///  - one immutable *El type per DSL element.
///
/// All raw Ui* AST types are kept internal to this builder.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 16/12/2025 @ 01:44
public final class DslGraphMetaModelBuilder implements DslUtility {

    private DslModel model;

    public DslGraphMetaModelBuilder() {
    }

    ///
    /// Build a DslGraphModel from the given DslModel.
    ///
    public DslGraphModel build(DslModel model) {
        this.model = model;
        Objects.requireNonNull(model, "model must not be null");
        UiContainerExpr root = model.getRoot();
        Log.tryLog(DslGraphMetaModelBuilder.class, this, LogLevel.INFO,
                "Building graph meta model for DSL", model.getName(), "root tag:", root.getName());

        DslSchemaShape shape = extractRootShape();
        final Map<String, Map<String, DslType>> attrTypesByElement = extractAttributeTypes();

        Map<String, DslGraphModel.ElementType> elements = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : shape.elements.entrySet()) {
            final String tagName = entry.getKey();
            final Set<String> attrs = entry.getValue();
            final String elTypeName = toElTypeName(tagName);

            final Map<String, DslType> attrTypes = attrTypesByElement.get(tagName);
            if (attrTypes == null) {
                elements.put(tagName, new DslGraphModel.ElementType(tagName, elTypeName, attrs));
            } else {
                elements.put(tagName, new DslGraphModel.ElementType(tagName, elTypeName, attrs, attrTypes));
            }
        }

        return new DslGraphModel(
                model.getName(),
                model.getPackageName(),
                shape.rootElement,
                elements
        );
    }

    /// Convert a raw tag name (e.g. "root", "entity-field", "my_widget")
    /// into a Java simple name for the generated element type,
    /// by turning it into TitleCase and appending "El".
    ///
    /// Examples:
    ///  - "root"        -> "RootEl"
    ///  - "entity"      -> "EntityEl"
    ///  - "entity-field"-> "EntityFieldEl"
    ///  - "my_widget"   -> "MyWidgetEl"
    ///
    public String toElTypeName(String tagName) {
        if (tagName == null || tagName.isEmpty()) {
            throw new IllegalArgumentException("tagName must be specified");
        }
        StringBuilder out = new StringBuilder();
        boolean up = true;
        for (int i = 0; i < tagName.length(); i++) {
            char c = tagName.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                up = true;
                continue;
            }
            if (out.length() == 0 || up) {
                out.append(Character.toUpperCase(c));
                up = false;
            } else {
                out.append(c);
            }
        }
        if (out.length() == 0) {
            out.append("Unnamed");
        }
        out.append("El");
        return out.toString();
    }

    @Override
    public DslModel getDslModel() {
        return model;
    }
}