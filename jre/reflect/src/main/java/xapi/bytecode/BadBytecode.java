package xapi.bytecode;

public class BadBytecode extends Exception {
	private static final long serialVersionUID = -757741172417827436L;

	public BadBytecode(int opcode) {
        super("bytecode " + opcode);
    }

    public BadBytecode(String msg) {
        super(msg);
    }

    public BadBytecode(String msg, Throwable cause) {
        super(msg, cause);
    }
}
