package xapi.dev.gwtc.impl;

/**
 * This class is used to manage the state of a running Gwt compile;
 * it should be used entirely from the calling classloader,
 * and will maintain reflective isolation from the running Gwt compile
 * (which may be running in another classloader, or a completely different JVM).
 *
 * All communication to the running Gwt compilation must be done through
 * de/serializable means; for an external process, we have to pipe everything
 * through standard in/out streams, and for an isolated classloader,
 * we will avoid reflection-hell by using the same stream-based API (io streams, not collection streams)
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/12/17.
 */
public class GwtcJobController {

}
