package net.wti.dsl.parser;

import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;

import java.util.Objects;

///
/// Minimal high-level model wrapper around a parsed xapi-dsl definition.
/// For now this just holds the root UiContainerExpr and exposes a few
/// convenience accessors (name, packageName), but it is intended to evolve
/// into a richer meta-model as we firm up the xapi-dsl format.
///
public class DslModel {

    private final UiContainerExpr root;

    public DslModel(UiContainerExpr root) {
        this.root = Objects.requireNonNull(root, "root must not be null");
    }

    public UiContainerExpr getRoot() {
        return root;
    }

    ///
    /// @return The logical name of this DSL, taken from the root xapi-dsl
    ///         element's name= attribute when present, otherwise the
    ///         DslParser.ROOT_ELEMENT fallback.
    ///
    public String getName() {
        return root.getAttribute("name")
                .mapIfPresent(UiAttrExpr::getStringExpression)
                .ifAbsentReturn(DslParser.ROOT_ELEMENT);
    }

    ///
    /// @return The Java package name to use for generated artifacts,
    ///         taken from the root xapi-dsl element's package= or
    ///         packageName= attribute when present. When absent, this
    ///         returns the empty string, which callers may interpret as
    ///         "default package" or apply their own default.
    ///
    public String getPackageName() {
        // Prefer explicit package=, then packageName=
        UiAttrExpr attr = root.getAttribute("package")
                .ifAbsent(root::getAttribute, "packageName")
                .getOrNull();
        if (attr == null) {
            return "";
        }
        String value = attr.getString(false, true);
        return value == null ? "" : value.trim();
    }
}
