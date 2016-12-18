package xapi.test.server;

import org.junit.Test;
import xapi.model.X_Model;
import xapi.server.api.Route;
import xapi.server.api.RouteMap;

import static org.junit.Assert.assertEquals;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/13/16.
 */
public class RouteMapTest {

    @Test
    public void testSimpleRouteMap() {
        RouteMap map = X_Model.create(RouteMap.class);
        final Route route = X_Model.create(Route.class);
        route.setPath("hello/world");
        map.addRoute(route);
        final RouteMap subroute = map.getSubroutes().get("hello");
        final Route actual = subroute.getSegments().get("world");
        assertEquals("", route, actual);
    }

    @Test
    public void testWildcardMatching() {

        RouteMap map = X_Model.create(RouteMap.class);
        final Route helloWorld = X_Model.create(Route.class);
        helloWorld.setPath("hello/world");
        map.addRoute(helloWorld);
        final Route helloAnyWorld = X_Model.create(Route.class);
        helloAnyWorld.setPath("hello/*/world");
        map.addRoute(helloAnyWorld);
        final Route actual = map.findRoute("hello/world").get();
        final Route actualWildcard = map.findRoute("hello/wildcard/world/").get();
        assertEquals("", helloWorld, actual);
        assertEquals("", helloAnyWorld, actualWildcard);
    }
}
