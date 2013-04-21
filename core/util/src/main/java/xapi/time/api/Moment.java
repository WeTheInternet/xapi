package xapi.time.api;

import java.io.Serializable;

public interface Moment extends Serializable, Comparable<Moment>{

  double millis();

}
