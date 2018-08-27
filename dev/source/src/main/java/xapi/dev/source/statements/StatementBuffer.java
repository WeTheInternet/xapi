package xapi.dev.source.statements;

import xapi.dev.source.PrintBuffer;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 8/18/18 @ 22:52 PM.
 */
public class StatementBuffer extends PrintBuffer {

    private boolean removed;

    public StatementBuffer() {
    }

    public StatementBuffer(int indent) {
        super(indent);
    }

    public StatementBuffer(StringBuilder target) {
        super(target);
    }

    public StatementBuffer(StringBuilder target, int indent) {
        super(target);
        for(;indent --> 0; indent());
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    @Override
    public String toSource() {
        if (removed) {
            return "";
        }
        return super.toSource();
    }
}
