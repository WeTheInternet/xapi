package xapi.ui.api.style;

/**
 * A special interface that we use for css types that we want to be proxy-able on the server.
 *
 * You do not need to implement this type; it will be implemented dynamically on the server,
 * by gluing dynamic handling of these methods into a {@link java.lang.reflect.Proxy}
 * which also handles returning the correct classnames for a given type.
 *
 * It is present here in case some shared code wants to be able to reference a proxy
 * without actually depending on it for it's primary purpose
 * (the use case for this is in the private wti repo).
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/12/17.
 */
public interface CssProxy <Css> {

    String proxyCacheKey();

    default void maybeRefreshNames() {}

    Css getProxy();

    boolean sameProxy(Css css);

    void resetProxy(String newMapRoot);
}
