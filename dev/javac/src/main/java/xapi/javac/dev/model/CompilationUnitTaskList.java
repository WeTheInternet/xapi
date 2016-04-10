package xapi.javac.dev.model;

import com.sun.source.tree.CompilationUnitTree;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.fu.In1;

public class CompilationUnitTaskList {

  private final IntTo<In1<CompilationUnitTree>> listeners;
  private final String name;
  private CompilationUnitTree unit;
  private boolean finished;

  public CompilationUnitTaskList(String name, CompilationUnitTree unit) {
    this.name = name;
    this.unit = unit;
    assert name != null;
    listeners = X_Collect.newList(In1.class);
  }

  public boolean isFinished() {
    return finished;
  }

  public void finish() {
    finished = true;
    listeners.forEachValue(In1.receiver(unit));
    listeners.clear();
  }

  public String getName() {
    return name;
  }

  public CompilationUnitTree getUnit() {
    return unit;
  }

  public void setUnit(CompilationUnitTree unit) {
    this.unit = unit;
  }

  public void onFinished(In1<CompilationUnitTree> consumer) {
    if (finished) {
      consumer.in(unit);
    } else {
      listeners.add(consumer);
    }
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof CompilationUnitTaskList &&
        ((CompilationUnitTaskList)obj).name.equals(name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

}
