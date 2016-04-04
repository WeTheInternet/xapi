package xapi.javac.dev.model;

import com.sun.source.tree.CompilationUnitTree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CompilationUnitTaskList {

  private final List<Consumer<CompilationUnitTree>> listeners;
  private final String name;
  private CompilationUnitTree unit;
  private boolean finished;

  public CompilationUnitTaskList(String name, CompilationUnitTree unit) {
    this.name = name;
    this.unit = unit;
    listeners = new ArrayList<>();
  }

  public boolean isFinished() {
    return finished;
  }

  public void finish() {
    finished = true;
    listeners.forEach(c -> c.accept(unit));
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

  public void onFinished(Consumer<CompilationUnitTree> consumer) {
    if (finished) {
      consumer.accept(unit);
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
