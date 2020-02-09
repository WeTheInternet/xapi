package net.wti.gradle.schema.map;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.fu.In2;
import xapi.fu.data.ListLike;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.java.X_Jdk;

import static com.github.javaparser.ast.visitor.ComposableXapiVisitor.whenMissingFail;

/**
 * An object-oriented facade over a {@code <preload />} xapi element:
 * <code><pre>
 *        <preload
 *             name = "gwt"
 *             url = "${System.getProperty('xapi.mvn.repo', 'https://wti.net/repo')}"
 *             version = "${System.getProperty('xapi.version', XAPI_VERSION)}"
 *             // limits these artifacts to gwt platform, where they will be auto-available as versionless dependencies
 *             // this inheritance is also given to any platform replacing gwt platform.
 *             platforms = [ "gwt" ]
 *             modules = [ main ]
 *             artifacts = {
 *                 "com.google.gwt" : [
 *                     "gwt-user",
 *                     "gwt-dev",
 *                     "gwt-codeserver",
 *                 ]
 *             }
 *         /preload>
 * </pre></code>
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-08 @ 4:56 a.m..
 */
public class SchemaPreload {

    private final String name, url, version;
    private final ListLike<String> platforms, modules;
    private final ListLike<SchemaArtifact> artifacts;

    public SchemaPreload(String name, String url, String version) {
        this.name = name;
        this.url = url;
        this.version = version;
        platforms = X_Jdk.list();
        modules = X_Jdk.list();
        artifacts = X_Jdk.list();
    }

    public static SchemaPreload fromAst(UiContainerExpr ast) {
        String name = ast.getAttributeRequiredString("name");
        String url = ast.getAttributeRequiredString("url");
        String version = ast.getAttributeRequiredString("version");
        final SchemaPreload preload = new SchemaPreload(name, url, version);

        ast.getAttribute("modules").readIfPresent(attr->{
            attr.getExpression().accept(
                whenMissingFail(SchemaPreload.class)
                    .extractNames(preload.modules::add), null)
                ;
        });

        ast.getAttribute("platforms").readIfPresent(attr->{
            attr.getExpression().accept(
                whenMissingFail(SchemaPreload.class)
                    .extractNames(preload.modules::add), null)
                ;
        });

        ast.getAttribute("artifacts").readIfPresent(attr->{
            attr.getExpression().accept(
                whenMissingFail(SchemaPreload.class)
                    .withJsonContainerRecurse(In2.ignoreAll())
                    .withJsonPairTerminal((artifactEntry, ignored)->{
                        String groupId = artifactEntry.getKeyString();
                        ChainBuilder<String> artifactIds = Chain.startChain();
                        artifactEntry.getValueExpr().accept(
                        whenMissingFail(SchemaPreload.class)
                            .extractNames(artifactId -> {
                                if (artifactId.contains(":")) {
                                    String[] idVers = artifactId.split(":");
                                    assert idVers.length == 2 : "Invalid artifactId " + artifactId + " contains too many : (max one allowed)";
                                    preload.artifacts.add(new SchemaArtifact(groupId, idVers[0], idVers[1]));
                                } else {
                                    preload.artifacts.add(new SchemaArtifact(groupId, artifactId, version));
                                }
                            }), null);
                    }), null)
                ;
        });

        return preload;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "SchemaPreload{" +
            "name='" + name + '\'' +
            ", url='" + url + '\'' +
            ", version='" + version + '\'' +
            ", platforms=" + platforms +
            ", modules=" + modules +
            ", artifacts=" + artifacts +
            '}';
    }
}
