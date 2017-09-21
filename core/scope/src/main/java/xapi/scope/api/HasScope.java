package xapi.scope.api;

/**
 * A simple functional interface, to allow more complex abstractions to be built
 * which can provide default methods that all rely on our Scope to store data,
 * so you could do something like:
 *
 * MyBuilder builder = ()->X_Scope.currentScope();
 * builder.doStuff(...);
 * Scope values = builder.build(); // makes a new scope with values from current local scope
 * MyInstance instance = ()->values; // stamp out copies of many types as we please
 * MyOtherInstance other = ()->values;
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/3/17.
 */
public interface HasScope<S extends Scope> {

    S getScope();

}
