package xapi.process.impl;

import xapi.annotation.process.OneToOne;
import xapi.process.api.Process;
import xapi.util.api.HasLifecycle.LifecycleHandler;

public abstract class AbstractProcess <T> extends LifecycleHandler implements Process <T> {

  @Override
  @OneToOne(stage=0)
  public boolean process(float milliTimeLimit) throws Exception {
    return false;
  }

}