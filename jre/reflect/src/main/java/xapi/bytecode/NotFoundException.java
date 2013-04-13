package xapi.bytecode;


/**
 * Signals that something could not be found.
 */
public class NotFoundException extends Exception {
	private static final long serialVersionUID = 2491987583471482087L;

	public NotFoundException(String msg) {
        super(msg);
    }

    public NotFoundException(String msg, Exception e) {
        super(msg + " because of " + e.toString());
    }
}
