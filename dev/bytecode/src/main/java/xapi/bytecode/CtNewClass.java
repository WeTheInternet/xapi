package xapi.bytecode;

import xapi.source.X_Modifier;

class CtNewClass extends CtClassType {
    /* true if the class is an interface.
     */
    protected boolean hasConstructor;

    CtNewClass(String name, ClassPool cp,
               boolean isInterface, CtClass superclass) {
        super(name, cp);
        wasChanged = true;
        String superName;
        if (isInterface || superclass == null)
            superName = null;
        else
            superName = superclass.getName();

        classfile = new ClassFile(isInterface, name, superName);
        if (isInterface && superclass != null)
            classfile.setInterfaces(new String[] { superclass.getName() });

        setModifiers(X_Modifier.setPublic(getModifiers()));
        hasConstructor = isInterface;
    }

    @Override
    protected void extendToString(StringBuffer buffer) {
        if (hasConstructor)
            buffer.append("hasConstructor ");

        super.extendToString(buffer);
    }

    @Override
    public void addConstructor(CtConstructor c)
        throws CannotCompileException
    {
        hasConstructor = true;
        super.addConstructor(c);
    }

}
