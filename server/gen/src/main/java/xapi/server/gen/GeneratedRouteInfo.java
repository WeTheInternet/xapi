package xapi.server.gen;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.HtmlBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.Lazy;
import xapi.fu.MappedIterable;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.server.gen.WebAppComponentGenerator.WebAppGeneratorScope;

/**
 * A container to encapsulate configuration / state for a generated route, as it is parsed.
 *
 * Depending on what sorts of elements are seen in the route component source,
 * we will either emit a static file that we can serve directly,
 * or a dynamic handler which will replace template variables with information from a database / user's session.
 *
 * In some cases, we will be able to generate a set of DOM which includes a script element
 * where we can give instructions to the running client how to fill in certain values;
 * for example, by creating a script element with an id, we can tell the client
 * to find that script, and set it's src to a value that depends on client info (like current host / subdomain).
 *
 * This will be done primarily to aid in reducing as many requests as possible to the filesystem.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/3/17.
 */
public class GeneratedRouteInfo {

    private final Lazy<HtmlBuffer> dom;
    private UiContainerExpr source;
    private final String path;
    private final String method;
    private final ChainBuilder<In1<WebAppGeneratorScope>> callbacks;
    private DomBuffer domTarget;
    private final StringTo<UiContainerExpr> idToInput;
    private final StringTo<UiContainerExpr> refToInput;
    private final StringTo<DomBuffer> idToOutput;
    private final StringTo<DomBuffer> refToOutput;

    public GeneratedRouteInfo(String path, String method) {
        this.path = path;
        this.method = method;
        dom = Lazy.deferred1(HtmlBuffer::new);
        callbacks = Chain.startChain();
        idToInput = X_Collect.newStringMap(UiContainerExpr.class);
        refToInput = X_Collect.newStringMap(UiContainerExpr.class);
        idToOutput = X_Collect.newStringMap(DomBuffer.class);
        refToOutput = X_Collect.newStringMap(DomBuffer.class);
    }

    public HtmlBuffer getHtmlBuffer() {
        return dom.out1();
    }

    public DomBuffer getDomTarget() {
        if (domTarget == null) {
            synchronized (dom) {
                if (domTarget == null) {
                    domTarget = dom.out1().getBody();
                    domTarget.setIndentNeeded(false);
                    domTarget.setTrimWhitespace(true);
                }
            }
        }
        return domTarget;
    }

    public GeneratedRouteInfo addServerStartCallback(In1<WebAppGeneratorScope> callback) {
        callbacks.add(callback);
        return this;
    }

    public boolean hasDom() {
        return dom.isResolved();
    }

    public void setSource(UiContainerExpr source) {
        this.source = source;
    }

    public UiContainerExpr getSource() {
        return source;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public DomBuffer getScriptTarget() {
        final HtmlBuffer buf = dom.out1();
        if (buf.hasBody()) {
            return buf.getBody();
        }
        return buf.getHead().getBuffer();
    }

    public void setDomTarget(DomBuffer domTarget) {
        this.domTarget = domTarget;
    }

    public void recordId(String val, UiContainerExpr input, DomBuffer output) {
        final UiContainerExpr wasInput = idToInput.put(val, input);
        assert wasInput == null || wasInput == input : "Redefining id " + val + " from " + wasInput + " to " + input;
        final DomBuffer wasOutput = idToOutput.put(val, output);
        assert wasOutput == null || wasOutput == output : "Redefining ref " + val + " from " + wasOutput + " to " + output;
    }

    public void recordRef(String val, UiContainerExpr input, DomBuffer output) {
        final UiContainerExpr wasInput = refToInput.put(val, input);
        assert wasInput == null || wasInput == input : "Redefining ref " + val + " from " + wasInput + " to " + input;
        final DomBuffer wasOutput = refToOutput.put(val, output);
        assert wasOutput == null || wasOutput == output : "Redefining ref " + val + " from " + wasOutput + " to " + output;
    }

    public boolean needsCallback() {
        return callbacks.isNotEmpty();
    }
    public MappedIterable<In1<WebAppGeneratorScope>> callbackGenerators() {
        return callbacks;
    }
}
