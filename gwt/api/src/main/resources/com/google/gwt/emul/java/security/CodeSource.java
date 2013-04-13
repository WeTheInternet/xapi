package java.security;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.gwt.core.shared.GWT;

/**
 * Very simple emulation of CodeSource; only provided to give jre-compliant
 * access to location URL.
 */

public class CodeSource implements java.io.Serializable {

  private static final long serialVersionUID = 4977541819976013951L;

  /**
   * The code location.
   * 
   * @serial
   */
  private URL location;

  public CodeSource(String location) {
    try {
      this.location = new URL(location);
    } catch (MalformedURLException e) {
      GWT.log("Bad location URL: " + location, e);
    }
  }

  public int hashCode() {
    if (location != null)
      return location.hashCode();
    else
      return 0;
  }

  /**
   * Tests for equality between the specified object and this object. Two
   * CodeSource objects are considered equal if their locations are of identical
   * value.
   * 
   * @param obj the object to test for equality with this object.
   * @return true if the objects are considered equal, false otherwise.
   */
  public boolean equals(Object obj) {
    if (obj == this) 
        return true;
  
    // objects types must be equal
    if (!(obj instanceof CodeSource))
        return false;
  
    CodeSource cs = (CodeSource) obj;
  
    // URLs must match
    if (location == null) {
        // if location is null, then cs.location must be null as well
        return cs.location == null;
    } else {
        // if location is not null, then it must equal cs.location
        return location.equals(cs.location);
    }
  }

  /**
   * Returns the location associated with this CodeSource.
   * 
   * @return the location (URL).
   */
  public final URL getLocation() {
    /* since URL is practically immutable, returning itself is not a security
     * problem */
    return this.location;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    sb.append(this.location);
    sb.append(")");
    return sb.toString();
  }
}
