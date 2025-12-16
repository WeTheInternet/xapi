package net.wti.dsl.parser;

import net.wti.lang.parser.JavaParser;
import net.wti.lang.parser.ParseException;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import xapi.fu.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;

///  DslParser:
///
///  A small facade to parse & validate <xapi-dsl> definitions into a UiContainerExpr tree
///  and a minimal DslModel wrapper. This is intentionally shallow for now:
///  we validate the root element name and hand back the raw UI AST,
///  so higher-level tooling can decide how to interpret it.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 09/12/2025 @ 04:33
public class DslParser {

    public static final String ROOT_ELEMENT = "xapi-dsl";

    ///
    /// Parse a DSL definition from a classpath resource.
    ///
    /// @param resourcePath path visible to this classloader (e.g. "META-INF/xapi/test-dsl.xapi")
    /// @return a DslModel wrapping the parsed root UiContainerExpr
    ///
    public DslModel parseResource(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath must not be null");
        final ClassLoader cl = getClassLoader();
        final URL url = cl.getResource(resourcePath);
        if (url == null) {
            throw new IllegalArgumentException("DSL resource not found on classpath: " + resourcePath);
        }
        return parseUrl(url);
    }

    ///
    /// Parse a DSL definition from a URL.
    ///
    /// @param url url to a .xapi file describing an xapi-dsl
    /// @return a DslModel wrapping the parsed root UiContainerExpr
    ///
    public DslModel parseUrl(URL url) {
        Objects.requireNonNull(url, "url must not be null");
        try (InputStream in = url.openStream()) {
            final UiContainerExpr root = parseStream(url.toExternalForm(), in);
            return new DslModel(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading DSL from " + url, e);
        }
    }

    ///
    /// Parse a DSL definition from a generic InputStream.
    ///
    /// @param sourceName A name for diagnostics (file path / URL / logical name)
    /// @param in         The stream with .xapi content
    /// @return The root UiContainerExpr (must be an <xapi-dsl> element)
    ///
    public UiContainerExpr parseStream(String sourceName, InputStream in) {
        Objects.requireNonNull(sourceName, "sourceName must not be null");
        Objects.requireNonNull(in, "input stream must not be null");
        try {
            final UiContainerExpr expr =
                    JavaParser.parseUiContainerMergeMany(sourceName, in, Log.LogLevel.INFO);
            validateRoot(expr, sourceName);
            return expr;
        } catch (ParseException e) {
            throw new RuntimeException("Invalid xapi DSL source in " + sourceName, e);
        }
    }

    protected void validateRoot(UiContainerExpr root, String sourceName) {
        if (!ROOT_ELEMENT.equals(root.getName())) {
            Log.tryLog(DslParser.class, this, Log.LogLevel.ERROR,
                    "Bad DSL file, does not start with <" + ROOT_ELEMENT + ">; started with <" +
                            root.getName() + "> from " + sourceName);
            throw new IllegalArgumentException(
                    "Bad DSL file " + sourceName +
                            "; root element must be <" + ROOT_ELEMENT + ">, found <" + root.getName() + ">");
        }
    }

    protected ClassLoader getClassLoader() {
        final ClassLoader ctx = Thread.currentThread().getContextClassLoader();
        return ctx == null ? getClass().getClassLoader() : ctx;
    }

}
