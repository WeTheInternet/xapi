package xapi.annotation.process;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;


/**
 * Used on STATIC methods that should be run only when a thread is put down to rest.
 *
 * Note that in some environments, a thread will outlive a process;
 * if you are using the X_Process manager, you can simply register
 * an X_Process#onProcessComplete() handler to perform cleanup,
 * or use X_Process#getProcessLocal() to have a ThreadLocal-like cache,
 * that will be cleaned for you whenever the process is complete.
 *
 * Using OnProcessComplete is preferred, as a single thread may run many
 * processes, and a single process may span multiple threads.
 *
 * Thread-level caching / cleanup is recommended when you are running many
 * processes in a single thread; or if your objects are not threadsafe enough
 * to withstand highly parallel access.
 *
 * If you can handle the concurrency, wrapping many processes in a larger
 * process will allow for more sharing between threads, and a potentially
 * lighter memory footprint.
 *
 * For a string-key map optimized for highly concurrent thread safety,
 * see {@link xapi.collect.impl.MultithreadedStringTrie}.  It's bad in single or lightly threaded
 * processes, but can kick ass in a highly concurrent environment.
 * (It is a prefix-trie which locks on each node going down,
 * so threads working in "a.b.c.d" rarely interrupt those in "a.bb.c.d",
 * as they only contend whilst descending common paths.)
 *
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(METHOD)
public @interface OnThreadDeath {

}
