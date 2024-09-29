package xapi.annotation.process;

import java.lang.annotation.*;

/**
 * Use on types that want to exempt from inheritance of {@link Multiplexed} state.
 *
 * This annotation _is_ inherited, but that does not apply to superinterface inheritance,
 * so you have to actually add it to a class for it to be inherited.
 *
 * Can also be used to denote that the type should be a singleton.
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
 *
 * The default is uniplexed, as the api contract specifies a class-to-instance contract,
 * where it seems like the key is the class itself (uniplexed),
 * rather than a tuple key of the class and the object identity of the value (multiplexed).
 *
 * We use object identity because we wouldn't want to accidentally trigger expensive,
 * or worse, state mutating operations (resolving lazies), just to get/put things in a cache.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 8/13/18 @ 1:07 AM.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Uniplexed {
}
