package xapi.process.api;

import xapi.util.api.HasLifecycle;

public interface Process <T> extends HasLifecycle{

  boolean process(float milliTimeLimit) throws Exception;

}
