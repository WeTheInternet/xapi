package xapi.process.api;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/18/17.
 */
@FunctionalInterface
public interface HasThreadGroup {

    ThreadGroup getThreadGroup();
}
