package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class ClassInfo extends ConstInfo {
    static final int tag = 7;
    int name;
    int index;

    public ClassInfo(int className, int i) {
        name = className;
        index = i;
    }

    public ClassInfo(DataInput in, int i) throws IOException {
        name = in.readUnsignedShort();
        index = i;
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public String getClassName(ConstPool cp) {
        return cp.getUtf8Info(name);
    };

    @Override
    public void renameClass(ConstPool cp, String oldName, String newName) {
        String nameStr = cp.getUtf8Info(name);
        if (nameStr.equals(oldName)) {
          name = cp.addUtf8Info(newName);
        } else if (nameStr.charAt(0) == '[') {
            String nameStr2 = Descriptor.rename(nameStr, oldName, newName);
            if (nameStr != nameStr2) {
              name = cp.addUtf8Info(nameStr2);
            }
        }
    }

    @Override
    public void renameClass(ConstPool cp, Map<?, ?> map) {
        String oldName = cp.getUtf8Info(name);
        if (oldName.charAt(0) == '[') {
            String newName = Descriptor.rename(oldName, map);
            if (oldName != newName) {
              name = cp.addUtf8Info(newName);
            }
        }
        else {
            String newName = (String)map.get(oldName);
            if (newName != null && !newName.equals(oldName)) {
              name = cp.addUtf8Info(newName);
            }
        }
    }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<?, ?> map) {
        String classname = src.getUtf8Info(name);
        if (map != null) {
            String newname = (String)map.get(classname);
            if (newname != null) {
              classname = newname;
            }
        }

        return dest.addClassInfo(classname);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(tag);
        out.writeShort(name);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("Class #");
        out.println(name);
    }

    @Override
    void makeHashtable(ConstPool cp) {
        String name = Descriptor.toJavaName(getClassName(cp));
        cp.classes.put(name, this);
    }
}