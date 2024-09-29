package xapi.annotation.process;

import java.lang.annotation.*;

/**
 * Use to mark types that should be considered multiplexed.
 *
 * This annotation _is_ inherited, but that does not apply to superinterface inheritance,
 * so you have to actually add it to a class for it to be inherited.
 *
 * Can also be used to denote that the type should injected with new instances.
 *
 * Interleaving multiplexed subtypes can allow you to assemble delegates;
 * you have a @DoNotMultiplexed RealService, which your @Multiplexed LocalService
 * accesses directly, to add caching, authz, process-local callbacks, whatever you want.
 *
 * When choosing the default arity, (annotation for abstract base class)
 * ask yourself this: "does my type have any mutable state?"
 * If the answer is yes, you probably want a default Multiplexed.
 * If the answer is no, you can probably handle Uniplexed
 * (unless you have expensive resources that must be obtained, or other concurrency problems).
 *
 * Created for the purpose of attaching multiple session scopes to a given global scope,
 * but can be adapted for use elsewhere.
 *
 * The default is uniplexed, as the api contract specifies a class-to-instance contract,
 * where it seems like the key is the class itself (uniplexed),
 * not multiplexed, with a tuple key of the class and the object identity of the value (multiplexed).
 *
 * We use object identity because we wouldn't want to accidentally trigger expensive,
 * or worse, state mutating operations (resolving lazies), just to put things in a cache.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/24/17.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Multiplexed {
}
