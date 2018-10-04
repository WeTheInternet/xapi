package xapi.source.api;

import xapi.dev.source.SourceBuilder;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/2/18 @ 3:48 AM.
 */
public interface HasSourceBuilder <T> {

    SourceBuilder<T> getSource();
}
