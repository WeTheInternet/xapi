package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface InMany extends HasInput {

  Many<HasInput> children();

  default void in(Object ... args) {
    Fu.jutsu.applyArguments(0, children(), args);
  }

  @Override
  default int accept(int index, Object ... values) {
    int res = Fu.jutsu.applyArguments(index, children(), values);
    return res;
  }

  default void in1(Object one) {
    in(one);
  }

  default <I1> In1<I1> toIn1() {
    return this::in1;
  }

  default void in2(Object one, Object two) {
    in(one, two);
  }

  default <I1, I2> In2<I1, I2> toIn2() {
    return this::in2;
  }

  default void in3(Object one, Object two, Object three) {
    in(one, two, three);
  }

  default <I1, I2, I3> In3<I1, I2, I3> toIn3() {
    return this::in3;
  }

  default void in4(Object one, Object two, Object three, Object four) {
    in(one, two, three, four);
  }

  default <I1, I2, I3, I4> In4<I1, I2, I3, I4> toIn4() {
    return this::in4;
  }

  class InManyBuilder {

    Many<Out2<Integer, HasInput>> tail, head = tail = new Many<>();

    InManyBuilder add(In1 in) {
      tail = tail.add(Out2.out2(1, in));
      return this;
    }

    InManyBuilder add(In2 in) {
      tail = tail.add(Out2.out2(2, in));
      return this;
    }

    InManyBuilder add(In3 in) {
      tail = tail.add(Out2.out2(3, in));
      return this;
    }

    InManyBuilder add(In4 in) {
      tail = tail.add(Out2.out2(4, in));
      return this;
    }

    InManyBuilder add(InManyBuilder multi) {
      tail = tail.add(Out2.out2(()->(Integer)multi.size(), ()->multi.build()));
      return this;
    }

    private int size() {
      int size = 1;
      Many<Out2<Integer, HasInput>> test = head;
      while (test.next != null) {
        size ++;
        test = test.next;
      }
      return size;
    }

    InMany build() {
      // Waits until invocation to peek in the stack
      return ()->head.map(Out2::out2);
    }

    InMany buildSnapshot() {
      // Copy from the the stack so when we invoke, we see the builder state at this instant.
      final Many<HasInput> snapshot = head.map(Out2::out2);
      return ()->snapshot;
    }
  }

  static InMany of(HasInput in) {
    return ()->new Many<HasInput>().add(in);
  }

  static InMany of(HasInput before, HasInput after) {
    return ()->new Many<HasInput>().add(before).add(after);
  }

  static InMany of(HasInput before, HasInput middle, HasInput after) {
    return ()->new Many<HasInput>().add(before).add(middle).add(after);
  }

  static InMany of(HasInput... ins) {
    Many<HasInput> tail, head = tail = new Many<HasInput>();
    for (HasInput in : ins) {
      tail = tail.add(in);
    }
    return ()->head;
  }

}
