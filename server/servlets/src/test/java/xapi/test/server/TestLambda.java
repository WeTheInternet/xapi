package xapi.test.server;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/5/17.
 */
interface TestLambda {
    AtomicInteger cnt = new AtomicInteger(0);

    int m();
}
