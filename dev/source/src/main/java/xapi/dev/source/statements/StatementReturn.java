package xapi.dev.source.statements;

/**
 * Represents a return statement.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 8/18/18 @ 10:50 PM.
 */
public class StatementReturn extends StatementBuffer {

    public StatementReturn() {
        super();
    }

    public StatementReturn(int indent) {
        super(indent);
    }

    public StatementReturn(StringBuilder target) {
        super(target);
    }
}
