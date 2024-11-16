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

        final Route helloWorld = X_Model.create(Route.class);
        helloWorld.setPath("hello/world");
        final Route helloAnyWorld = X_Model.create(Route.class);
        helloAnyWorld.setPath("hello/*/world");

        // need to test that hello/* matches hello, hello/ and hello/world

        RouteMap map;
        map = X_Model.create(RouteMap.class);
        map.addRoute(helloWorld);
        map.addRoute(helloAnyWorld);
        Route actual = map.findRoute("hello/world").get();
        Route actualWildcard = map.findRoute("hello/wildcard/world/").get();
        assertEquals("", helloWorld, actual);
        assertEquals("", helloAnyWorld, actualWildcard);

        map = X_Model.create(RouteMap.class);
        // invert the order we add items
        map.addRoute(helloAnyWorld);
        map.addRoute(helloWorld);
        actual = map.findRoute("hello/world").get();
        actualWildcard = map.findRoute("hello/wildcard/world/").get();
        assertEquals("", helloWorld, actual);
        assertEquals("", helloAnyWorld, actualWildcard);
    }
}
