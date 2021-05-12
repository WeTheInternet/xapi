package net.wti.gradle.schema.view;

import xapi.fu.In1;

/**
 * ViewChain:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 09/05/2021 @ 2:12 a.m..
 */
public interface ViewChain<Next> {
    Next resolve();
    boolean isResolved();
    ViewChain<Next> whenResolved(In1<? super Next> callback);
}
