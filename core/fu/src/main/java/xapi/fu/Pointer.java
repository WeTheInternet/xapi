package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 1/3/16.
 */
public interface Pointer <T> extends In1<T>, Out1<T> {

  static <T> Pointer <T> pointer() {
    return new PointerSimple<>();
  }

  class PointerSimple <T> implements Pointer<T> {
    private volatile T value;

    public PointerSimple(T value) {
      this.value = value;
    }

    public PointerSimple() {
    }

    @Override
    public void in(T in) {
      this.value = in;
    }

    @Override
    public T out1() {
      return value;
    }
  }

  class PointerDeferred <T> implements Pointer<T>, Log {
    private volatile Out1<T> value;

    public PointerDeferred(Out1<T> value) {
      this.value = value;
    }

    public PointerDeferred() {
      value = this::warnUninitialized;
    }

    protected T warnUninitialized() {
      log(getClass(), "Attempting to get the value from a PointerDeferred, ", this, ", before its value has been initialized");
      return null;
    }

    @Override
    public void in(T in) {
      this.value = Out1.immutable1(in);
    }

    public void supply(Out1<T> deferred) {
      this.value = deferred;
    }

    @Override
    public T out1() {
      return value.out1();
    }
  }

  class PointerImmutable <T> implements Pointer<T> {
    private final T value;

    public PointerImmutable(T value) {
      this.value = value;
    }

    @Override
    public void in(T in) {
    }

    @Override
    public T out1() {
      return value;
    }
  }

  @Override
  void in(T in);

  @Override
  T out1();
}
