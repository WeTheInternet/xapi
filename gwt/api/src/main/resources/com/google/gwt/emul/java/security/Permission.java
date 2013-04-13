package java.security;

/**
 * Simple emulation layer compatibility for java permissions.
 */

public abstract class Permission implements java.io.Serializable {

    private static final long serialVersionUID = -5636570222231596674L;

    private String name;

    public Permission(String name) {
	this.name = name;
    }

    public void checkGuard(Object object) throws SecurityException {
      //Does nothing in gwt; we aren't actually implementing security manager (yet, at least).
    }

    public abstract boolean implies(Permission permission);

    public abstract boolean equals(Object obj);

    public abstract int hashCode();

    public final String getName() {
    	return name;
    }

    public abstract String getActions();

//    public PermissionCollection newPermissionCollection() {
//	return null;
//    }

    public String toString() {
	String actions = getActions();
	if ((actions == null) || (actions.length() == 0)) { // OPTIONAL
	    return "(" + getClass().getName() + " " + name + ")";
	} else {
	    return "(" + getClass().getName() + " " + name + " " +
		actions + ")";
	}
    }
}


