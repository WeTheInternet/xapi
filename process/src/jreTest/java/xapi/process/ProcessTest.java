package xapi.process;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import xapi.annotation.process.ManyToOne;
import xapi.annotation.process.OneToMany;
import xapi.annotation.process.OneToOne;
import xapi.process.impl.AbstractProcess;

public class ProcessTest extends AbstractProcess<Void>{

  static class Signal{

  }

  @OneToMany(stage=1)
  public Queue<Signal> fanOut() {
    //we don't need to actually fill this list now.
    //so long as the iterable blocks until it has exhausted it's supply of data
    final Queue<Signal> list = new LinkedBlockingDeque<Signal>();

    return list;
  }
  @OneToOne(stage=2)
  public void processItem(Signal item, int index) {

  }
  @ManyToOne(stage=2)
  public void fanIn(Signal[] all) {

  }

  @OneToOne(stage=3)
  public void cleanup() {

  }

}
