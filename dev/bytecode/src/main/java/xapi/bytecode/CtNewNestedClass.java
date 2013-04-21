package xapi.bytecode;

import xapi.bytecode.attributes.InnerClassesAttribute;
import xapi.source.api.AccessFlag;

class CtNewNestedClass extends CtNewClass {
    CtNewNestedClass(String realName, ClassPool cp, boolean isInterface,
                     CtClass superclass) {
        super(realName, cp, isInterface, superclass);
    }

    /**
     * This method does not change the STATIC bit.  The original value is kept.
     */
    public void setModifiers(int mod) {
        mod = mod & ~AccessFlag.STATIC;
        super.setModifiers(mod);
        updateInnerEntry(mod, getName(), this, true);
    }

    private static void updateInnerEntry(int mod, String name, CtClass clazz, boolean outer) {
        ClassFile cf = clazz.getClassFile2();
        InnerClassesAttribute ica = (InnerClassesAttribute)cf.getAttribute(
                                                InnerClassesAttribute.tag);
        if (ica == null)
            return;

        int n = ica.tableLength();
        for (int i = 0; i < n; i++)
            if (name.equals(ica.innerClass(i))) {
                int acc = ica.accessFlags(i) & AccessFlag.STATIC;
                ica.setAccessFlags(i, mod | acc);
                String outName = ica.outerClass(i);
                if (outName != null && outer)
                    try {
                        CtClass parent = clazz.getClassPool().get(outName);
                        updateInnerEntry(mod, name, parent, false);
                    }
                    catch (NotFoundException e) {
                        throw new RuntimeException("cannot find the declaring class: "
                                                   + outName);
                    }

                break;
            }
    }
}
