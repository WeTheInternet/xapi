package xapi.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public abstract class ConstInfo {
    public abstract int getTag();

    public String getClassName(ConstPool cp) { return null; }
    public void renameClass(ConstPool cp, String oldName, String newName) {}
    public void renameClass(ConstPool cp, Map<?, ?> classnames) {}
    public abstract int copy(ConstPool src, ConstPool dest, Map<?, ?> classnames);
                        // ** classnames is a mapping between JVM names.

    public abstract void write(DataOutput out) throws IOException;
    public abstract void print(PrintWriter out);

    void makeHashtable(ConstPool cp) {}     // called after read() finishes in ConstPool.

    boolean hashCheck(int a, int b) { return false; }

    @Override
    public String toString() {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(bout);
        print(out);
        return bout.toString();
    }
}