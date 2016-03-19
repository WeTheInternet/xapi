package xapi.javac.dev.model;

import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PendingCompilationUnit {

  private final List<Consumer<JCCompilationUnit>> listeners;
  private final String name;
  private JCCompilationUnit unit;
  private boolean finished;

  public PendingCompilationUnit(String name, JCCompilationUnit unit) {
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

  public JCCompilationUnit getUnit() {
    return unit;
  }

  public void setUnit(JCCompilationUnit unit) {
    this.unit = unit;
  }

  public void onFinished(Consumer<JCCompilationUnit> consumer) {
    if (finished) {
      consumer.accept(unit);
    } else {
      listeners.add(consumer);
    }
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof PendingCompilationUnit &&
        ((PendingCompilationUnit)obj).name.equals(name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
