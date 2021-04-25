package xapi.server.api;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.collect.simple.SimpleLinkedList;
import xapi.except.NotYetImplemented;
import xapi.fu.Maybe;
import xapi.fu.Out2;
import xapi.model.X_Model;
import xapi.model.api.Model;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/13/16.
 */
public interface RouteMap extends Model {

    StringTo<RouteMap> getSubroutes();

    default Maybe<Route> findRoute(String path) {

        String[] bits = path.split("/");
        SimpleLinkedList<Out2<RouteMap, Integer>> doMore = new SimpleLinkedList<>();
        doMore.add(Out2.out2Immutable(this, 0));
        while (!doMore.isEmpty()) {
            final Out2<RouteMap, Integer> next = doMore.pop();
            int ind = next.out2();
            final RouteMap map = next.out1();

            if (ind == bits.length) {
                // Trailing /
                final StringTo<Route> segments = map.getSegments();
                if (segments != null) {
                    final Route route = segments.get("");
                    if (route != null) {
                        return Maybe.immutable(route);
                    }
                }
                continue;
            }
            String nextPath = bits[ind];
            if (ind == bits.length-1) {
                // Last segment; check for route.
                final StringTo<Route> segments = map.getSegments();
                if (segments != null) {
                    final Route segment = segments.get(nextPath);
                    return Maybe.immutable(segment);
                }
            }

            final StringTo<RouteMap> wildcards = map.getWildcards();
            if (wildcards != null) {
                for (String wildcard : wildcards.keys()) {
                    switch (wildcard) {
                        case "*":
                            doMore.add(Out2.out2Immutable(wildcards.get("*"), ind+1));
                            break;
                        case "**":
                            throw new NotYetImplemented("** wildcard paths not yet supported");
                        default:
                            // A mix of chars and *
                            throw new NotYetImplemented("Non \"*\" wildcard paths not yet supported");
                    }
                }

            }

            final StringTo<RouteMap> subroutes = map.getSubroutes();
            if (subroutes != null) {
                final RouteMap nextRoute = subroutes.get(nextPath);
                if (nextRoute != null) {
                    doMore.add(Out2.out2Immutable(nextRoute, ind+1));
                }
            }
        }

        return Maybe.not();
    }

    default StringTo<RouteMap> getOrCreateSubroutes() {
        synchronized (this) {
            StringTo<RouteMap> subroutes = getSubroutes();
            if (subroutes == null) {
                subroutes = X_Collect.newStringMap(RouteMap.class);
                setSubroutes(subroutes);
            }
            return subroutes;
        }
    }

    default StringTo<RouteMap> getOrCreateWildcards() {
        synchronized (this) {
            StringTo<RouteMap> wildcards = getWildcards();
            if (wildcards == null) {
                wildcards = X_Collect.newStringMap(RouteMap.class);
                setWildcards(wildcards);
            }
            return wildcards;
        }
    }

    RouteMap setSubroutes(StringTo<RouteMap> routes);

    StringTo<Route> getSegments();

    default StringTo<Route> getOrCreateSegments() {
        synchronized (this) {
            StringTo<Route> segments = getSegments();
            if (segments == null) {
                segments = X_Collect.newStringMap(Route.class);
                setSegments(segments);
            }
            return segments;
        }
    }
    RouteMap setSegments(StringTo<Route> route);

    default RouteMap addSegment(String segment, Route route) {
        int slash = segment.indexOf('/');
        if (slash == -1) {
            if (segment.contains("*")) {
                StringTo<RouteMap> map = getOrCreateWildcards();
                final RouteMap newMap = X_Model.create(RouteMap.class);
                newMap.addSegment("", route);
                map.put(segment, newMap);
            } else {
                getOrCreateSegments()
                    .put(segment, route);
            }
        } else {
            // Need to add a subroute
            String subroute = segment.substring(0, slash);
            StringTo<RouteMap> subroutes;
            if (subroute.contains("*")) {
                subroutes = getOrCreateWildcards();
            } else {
                subroutes = getOrCreateSubroutes();
            }
            final String childRoute = segment.substring(slash + 1);
            RouteMap child = subroutes.get(subroute);
            if (child == null) {
                child = X_Model.create(RouteMap.class);
                subroutes.put(subroute, child);
            }
            child.addSegment(childRoute, route);
        }
        return this;
    }

    StringTo<RouteMap> getWildcards();

    RouteMap setWildcards(StringTo<RouteMap> route);

    default void addRoute(Route route) {
        addSegment(route.getPath(), route);
    }
}
