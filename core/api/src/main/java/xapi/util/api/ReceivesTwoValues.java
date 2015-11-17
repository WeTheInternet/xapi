package xapi.util.api;

public interface ReceivesTwoValues <V0, V1> {

  ReceivesTwoValues NO_OP = new ReceivesTwoValues() {
    @Override public void receiveValues(Object o, Object o2) {}
  };

  void receiveValues(V0 v0, V1 v1);

}
