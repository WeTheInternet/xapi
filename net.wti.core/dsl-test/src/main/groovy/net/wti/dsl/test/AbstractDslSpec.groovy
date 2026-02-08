package net.wti.dsl.test

import net.wti.lang.parser.JavaParser
import net.wti.lang.parser.ast.expr.Expression
import net.wti.lang.parser.ast.expr.UiContainerExpr
import spock.lang.Specification
import xapi.fu.itr.MappedIterable

///
/// AbstractDslSpec:
///
/// Shared test utilities for DSL-related specs.
/// Provides a stable way to load and parse `.xapi` resources into `sourceAst` suitable for
/// constructing schema (`DslType*`) and runtime (`DslNode*` / `DslValue*`) objects.
///
abstract class AbstractDslSpec extends Specification {

    ///
    /// Loads and parses an `.xapi` file from the test classpath.
    ///
    /// Resource paths must be absolute (start with `/`) and point at real files under
    /// `src/test/resources`.
    ///
    protected final MappedIterable<Expression> astFromResource(final String resourcePath) {
        final InputStream inputStream = AbstractDslSpec.class.getResourceAsStream(resourcePath)
        assert inputStream != null : "Missing test resource on classpath: " + resourcePath

        final UiContainerExpr root = JavaParser.parseXapi(inputStream)

        final List<Expression> singleton = Collections.<Expression>singletonList(root)
        return MappedIterable.mapped(singleton)
    }
}
