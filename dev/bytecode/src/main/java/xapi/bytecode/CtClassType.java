package xapi.bytecode;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import xapi.bytecode.CtMember.Cache;
import xapi.bytecode.annotation.Annotation;
import xapi.bytecode.annotation.AnnotationsAttribute;
import xapi.bytecode.attributes.AttributeInfo;
import xapi.bytecode.attributes.InnerClassesAttribute;
import xapi.bytecode.attributes.ParameterAnnotationsAttribute;
import xapi.source.X_Modifier;

public class CtClassType extends CtClass {
  ClassPool classPool;
  boolean wasChanged;
  boolean wasPruned;
  boolean gcConstPool; // if true, the constant pool entries will be garbage
                       // collected.
  ClassFile classfile;
  byte[] rawClassfile; // backup storage

  private WeakReference<Cache> memberCache;

  private Hashtable<?, ?> hiddenMethods; // must be synchronous
  private int uniqueNumberSeed;

  private boolean doPruning = ClassPool.doPruning;
  private int getCount;
  private static final int GET_THRESHOLD = 2; // see compress()

  CtClassType(String name, ClassPool cp) {
    super(name);
    classPool = cp;
    wasChanged = wasPruned = gcConstPool = false;
    classfile = null;
    rawClassfile = null;
    memberCache = null;
    hiddenMethods = null;
    uniqueNumberSeed = 0;
    getCount = 0;
  }

  public CtClassType(InputStream ins, ClassPool cp) throws IOException {
    this((String) null, cp);
    classfile = new ClassFile(new DataInputStream(ins));
    qualifiedName = classfile.getName();
  }

  @Override
  public ClassFile getClassFile2() {
    ClassFile cfile = classfile;
    if (cfile != null)
      return cfile;

    classPool.compress();
    if (rawClassfile != null) {
      try {
        classfile = new ClassFile(new DataInputStream(new ByteArrayInputStream(
            rawClassfile)));
        rawClassfile = null;
        getCount = GET_THRESHOLD;
        return classfile;
      } catch (IOException e) {
        throw new RuntimeException(e.toString(), e);
      }
    }

    InputStream fin = null;
    try {
      fin = classPool.openClassfile(getName());
      if (fin == null)
        throw new NotFoundException(getName());

      fin = new BufferedInputStream(fin);
      ClassFile cf = new ClassFile(new DataInputStream(fin));
      if (!cf.getName().equals(qualifiedName))
        throw new RuntimeException("cannot find " + qualifiedName + ": "
            + cf.getName() + " found in " + qualifiedName.replace('.', '/')
            + ".class");

      classfile = cf;
      return cf;
    } catch (NotFoundException e) {
      throw new RuntimeException(e.toString(), e);
    } catch (IOException e) {
      throw new RuntimeException(e.toString(), e);
    } finally {
      if (fin != null)
        try {
          fin.close();
        } catch (IOException e) {
        }
    }
  }

  /*
   * Inherited from CtClass. Called by get() in ClassPool.
   * 
   * @see javassist.CtClass#incGetCounter()
   * 
   * @see #toBytecode(DataOutputStream)
   */
  @Override
  final void incGetCounter() {
    ++getCount;
  }

  /**
   * Invoked from ClassPool#compress(). It releases the class files that have
   * not been recently used if they are unmodified.
   */
  @Override
  void compress() {
    if (getCount < GET_THRESHOLD)
      if (!isModified() && ClassPool.releaseUnmodifiedClassFile)
        removeClassFile();
      else if (isFrozen() && !wasPruned)
        saveClassFile();

    getCount = 0;
  }

  /**
   * Converts a ClassFile object into a byte array for saving memory space.
   */
  private synchronized void saveClassFile() {
    /*
     * getMembers() and releaseClassFile() are also synchronized.
     */
    if (classfile == null)
      return;

    ByteArrayOutputStream barray = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(barray);
    try {
      classfile.write(out);
      barray.close();
      rawClassfile = barray.toByteArray();
      classfile = null;
    } catch (IOException e) {
    }
  }

  private synchronized void removeClassFile() {
    if (classfile != null && !isModified())
      classfile = null;
  }

  @Override
  public ClassPool getClassPool() {
    return classPool;
  }

  void setClassPool(ClassPool cp) {
    classPool = cp;
  }

  @Override
  public URL getURL() throws NotFoundException {
    URL url = classPool.find(getName());
    if (url == null)
      throw new NotFoundException(getName());
    else
      return url;
  }

  @Override
  public boolean isModified() {
    return wasChanged;
  }

  @Override
  public boolean subtypeOf(CtClass clazz) throws NotFoundException {
    int i;
    String cname = clazz.getName();
    if (this == clazz || getName().equals(cname))
      return true;

    ClassFile file = getClassFile2();
    String supername = file.getSuperclass();
    if (supername != null && supername.equals(cname))
      return true;

    String[] ifs = file.getInterfaces();
    int num = ifs.length;
    for (i = 0; i < num; ++i)
      if (ifs[i].equals(cname))
        return true;

    if (supername != null && classPool.get(supername).subtypeOf(clazz))
      return true;

    for (i = 0; i < num; ++i)
      if (classPool.get(ifs[i]).subtypeOf(clazz))
        return true;

    return false;
  }

  // @Override
  public void setName(String name) throws RuntimeException {
    String oldname = getName();
    if (name.equals(oldname))
      return;

    // check this in advance although classNameChanged() below does.
    classPool.checkNotFrozen(name);
    ClassFile cf = getClassFile2();
    super.setName(name);
    cf.setName(name);
    nameReplaced();
    classPool.classNameChanged(oldname, this);
  }

  private void nameReplaced() {
    CtMember.Cache cache = hasMemberCache();
    if (cache != null) {
      CtMember mth = cache.methodHead();
      CtMember tail = cache.lastMethod();
      while (mth != tail) {
        mth = mth.next();
        mth.nameReplaced();
      }
    }
  }

  /**
   * Returns null if members are not cached.
   */
  protected CtMember.Cache hasMemberCache() {
    if (memberCache != null)
      return (CtMember.Cache) memberCache.get();
    else
      return null;
  }

  @Override
  public boolean isInterface() {
    return X_Modifier.isInterface(getModifiers());
  }

  @Override
  public boolean isAnnotation() {
    return X_Modifier.isAnnotation(getModifiers());
  }

  @Override
  public boolean isEnum() {
    return X_Modifier.isEnum(getModifiers());
  }

  @Override
  public int getModifiers() {
    ClassFile cf = getClassFile2();
    int acc = cf.getAccessFlags();
    acc = X_Modifier.clear(acc, X_Modifier.SUPER);
    int inner = cf.getInnerAccessFlags();
    if (inner != -1 && (inner & X_Modifier.STATIC) != 0)
      acc |= X_Modifier.STATIC;

    return acc;
  }

  @Override
  public CtClass[] getNestedClasses() throws NotFoundException {
    ClassFile cf = getClassFile2();
    InnerClassesAttribute ica = (InnerClassesAttribute) cf
        .getAttribute(InnerClassesAttribute.tag);
    if (ica == null)
      return new CtClass[0];

    String thisName = cf.getName();
    int n = ica.tableLength();
    ArrayList<CtClass> list = new ArrayList<CtClass>(n);
    for (int i = 0; i < n; i++) {
      String outer = ica.outerClass(i);
      /*
       * If a nested class is local or anonymous, the outer_class_info_index is
       * 0.
       */
      if (outer == null || outer.equals(thisName)) {
        String inner = ica.innerClass(i);
        if (inner != null)
          list.add(classPool.get(inner));
      }
    }

    return (CtClass[]) list.toArray(new CtClass[list.size()]);
  }

  @Override
  public void setModifiers(int mod) {
    ClassFile cf = getClassFile2();
    if (X_Modifier.isStatic(mod)) {
      int flags = cf.getInnerAccessFlags();
      if (flags != -1 && (flags & X_Modifier.STATIC) != 0)
        mod = mod & ~X_Modifier.STATIC;
      else
        throw new RuntimeException("cannot change " + getName()
            + " into a static class");
    }

    checkModify();
    cf.setAccessFlags(mod);
  }

  @Override
  public boolean hasAnnotation(Class<?> clz) {
    ClassFile cf = getClassFile2();
    AnnotationsAttribute ainfo = (AnnotationsAttribute) cf
        .getAttribute(AnnotationsAttribute.invisibleTag);
    AnnotationsAttribute ainfo2 = (AnnotationsAttribute) cf
        .getAttribute(AnnotationsAttribute.visibleTag);
    return hasAnnotationType(clz, getClassPool(), ainfo, ainfo2);
  }

  static boolean hasAnnotationType(Class<?> clz, ClassPool cp,
      AnnotationsAttribute a1, AnnotationsAttribute a2) {
    Annotation[] anno1, anno2;

    if (a1 == null)
      anno1 = null;
    else
      anno1 = a1.getAnnotations();

    if (a2 == null)
      anno2 = null;
    else
      anno2 = a2.getAnnotations();

    String typeName = clz.getName();
    if (anno1 != null)
      for (int i = 0; i < anno1.length; i++)
        if (anno1[i].getTypeName().equals(typeName))
          return true;

    if (anno2 != null)
      for (int i = 0; i < anno2.length; i++)
        if (anno2[i].getTypeName().equals(typeName))
          return true;

    return false;
  }

  @Override
  public Object getAnnotation(Class<?> clz) throws ClassNotFoundException {
    ClassFile cf = getClassFile2();
    AnnotationsAttribute ainfo = (AnnotationsAttribute) cf
        .getAttribute(AnnotationsAttribute.invisibleTag);
    AnnotationsAttribute ainfo2 = (AnnotationsAttribute) cf
        .getAttribute(AnnotationsAttribute.visibleTag);
    return getAnnotationType(clz, getClassPool(), ainfo, ainfo2);
  }

  static Object getAnnotationType(Class<?> clz, ClassPool cp,
      AnnotationsAttribute a1, AnnotationsAttribute a2)
      throws ClassNotFoundException {
    Annotation[] anno1, anno2;

    if (a1 == null)
      anno1 = null;
    else
      anno1 = a1.getAnnotations();

    if (a2 == null)
      anno2 = null;
    else
      anno2 = a2.getAnnotations();

    String typeName = clz.getName();
    if (anno1 != null)
      for (int i = 0; i < anno1.length; i++)
        if (anno1[i].getTypeName().equals(typeName))
          return toAnnoType(anno1[i], cp);

    if (anno2 != null)
      for (int i = 0; i < anno2.length; i++)
        if (anno2[i].getTypeName().equals(typeName))
          return toAnnoType(anno2[i], cp);

    return null;
  }

  @Override
  public Object[] getAnnotations() throws ClassNotFoundException {
    return getAnnotations(false);
  }

  @Override
  public Object[] getAvailableAnnotations() {
    try {
      return getAnnotations(true);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Unexpected exception ", e);
    }
  }

  private Object[] getAnnotations(boolean ignoreNotFound)
      throws ClassNotFoundException {
    ClassFile cf = getClassFile2();
    AnnotationsAttribute ainfo = (AnnotationsAttribute) cf
        .getAttribute(AnnotationsAttribute.invisibleTag);
    AnnotationsAttribute ainfo2 = (AnnotationsAttribute) cf
        .getAttribute(AnnotationsAttribute.visibleTag);
    return toAnnotationType(ignoreNotFound, getClassPool(), ainfo, ainfo2);
  }

  static Object[] toAnnotationType(boolean ignoreNotFound, ClassPool cp,
      AnnotationsAttribute a1, AnnotationsAttribute a2)
      throws ClassNotFoundException {
    Annotation[] anno1, anno2;
    int size1, size2;

    if (a1 == null) {
      anno1 = null;
      size1 = 0;
    } else {
      anno1 = a1.getAnnotations();
      size1 = anno1.length;
    }

    if (a2 == null) {
      anno2 = null;
      size2 = 0;
    } else {
      anno2 = a2.getAnnotations();
      size2 = anno2.length;
    }

    if (!ignoreNotFound) {
      Object[] result = new Object[size1 + size2];
      for (int i = 0; i < size1; i++)
        result[i] = toAnnoType(anno1[i], cp);

      for (int j = 0; j < size2; j++)
        result[j + size1] = toAnnoType(anno2[j], cp);

      return result;
    } else {
      ArrayList<Object> annotations = new ArrayList<Object>();
      for (int i = 0; i < size1; i++) {
        try {
          annotations.add(toAnnoType(anno1[i], cp));
        } catch (ClassNotFoundException e) {
        }
      }
      for (int j = 0; j < size2; j++) {
        try {
          annotations.add(toAnnoType(anno2[j], cp));
        } catch (ClassNotFoundException e) {
        }
      }

      return annotations.toArray();
    }
  }

  static Object[][] toAnnotationType(boolean ignoreNotFound, ClassPool cp,
      ParameterAnnotationsAttribute a1, ParameterAnnotationsAttribute a2,
      MethodInfo minfo) throws ClassNotFoundException {
    int numParameters = 0;
    if (a1 != null)
      numParameters = a1.numParameters();
    else if (a2 != null)
      numParameters = a2.numParameters();
    else
      numParameters = Descriptor.numOfParameters(minfo.getDescriptor());

    Object[][] result = new Object[numParameters][];
    for (int i = 0; i < numParameters; i++) {
      Annotation[] anno1, anno2;
      int size1, size2;

      if (a1 == null) {
        anno1 = null;
        size1 = 0;
      } else {
        anno1 = a1.getAnnotations()[i];
        size1 = anno1.length;
      }

      if (a2 == null) {
        anno2 = null;
        size2 = 0;
      } else {
        anno2 = a2.getAnnotations()[i];
        size2 = anno2.length;
      }

      if (!ignoreNotFound) {
        result[i] = new Object[size1 + size2];
        for (int j = 0; j < size1; ++j)
          result[i][j] = toAnnoType(anno1[j], cp);

        for (int j = 0; j < size2; ++j)
          result[i][j + size1] = toAnnoType(anno2[j], cp);
      } else {
        ArrayList<Object> annotations = new ArrayList<Object>();
        for (int j = 0; j < size1; j++) {
          try {
            annotations.add(toAnnoType(anno1[j], cp));
          } catch (ClassNotFoundException e) {
          }
        }
        for (int j = 0; j < size2; j++) {
          try {
            annotations.add(toAnnoType(anno2[j], cp));
          } catch (ClassNotFoundException e) {
          }
        }

        result[i] = annotations.toArray();
      }
    }

    return result;
  }

  private static Object toAnnoType(Annotation anno, ClassPool cp)
      throws ClassNotFoundException {
    try {
      ClassLoader cl = cp.getClassLoader();
      return anno.toAnnotationType(cl, cp);
    } catch (ClassNotFoundException e) {
      ClassLoader cl2 = cp.getClass().getClassLoader();
      return anno.toAnnotationType(cl2, cp);
    }
  }

  @Override
  public boolean subclassOf(CtClass superclass) {
    if (superclass == null)
      return false;

    String superName = superclass.getName();
    CtClass curr = this;
    try {
      while (curr != null) {
        if (curr.getName().equals(superName))
          return true;

        curr = curr.getSuperclass();
      }
    } catch (Exception ignored) {
    }
    return false;
  }

  @Override
  public CtClass getSuperclass() throws NotFoundException {
    String supername = getClassFile2().getSuperclass();
    if (supername == null)
      return null;
    else
      return classPool.get(supername);
  }

  @Override
  public void setSuperclass(CtClass clazz) {
    checkModify();
    if (isInterface())
      addInterface(clazz);
    else
      getClassFile2().setSuperclass(clazz.getName());
  }

  @Override
  public CtClass[] getInterfaces() throws NotFoundException {
    String[] ifs = getClassFile2().getInterfaces();
    int num = ifs.length;
    CtClass[] ifc = new CtClass[num];
    for (int i = 0; i < num; ++i)
      ifc[i] = classPool.get(ifs[i]);

    return ifc;
  }

  @Override
  public void setInterfaces(CtClass[] list) {
    checkModify();
    String[] ifs;
    if (list == null)
      ifs = new String[0];
    else {
      int num = list.length;
      ifs = new String[num];
      for (int i = 0; i < num; ++i)
        ifs[i] = list[i].getName();
    }

    getClassFile2().setInterfaces(ifs);
  }

  @Override
  public void addInterface(CtClass anInterface) {
    checkModify();
    if (anInterface != null)
      getClassFile2().addInterface(anInterface.getName());
  }

  @Override
  public CtClass getDeclaringClass() throws NotFoundException {
    ClassFile cf = getClassFile2();
    InnerClassesAttribute ica = (InnerClassesAttribute) cf
        .getAttribute(InnerClassesAttribute.tag);
    if (ica == null)
      return null;

    String name = getName();
    int n = ica.tableLength();
    for (int i = 0; i < n; ++i)
      if (name.equals(ica.innerClass(i))) {
        String outName = ica.outerClass(i);
        if (outName != null)
          return classPool.get(outName);
        else {
          // we don't support anonymous or local classes
          // // maybe anonymous or local class.
          // EnclosingMethodAttribute ema
          // = (EnclosingMethodAttribute)cf.getAttribute(
          // EnclosingMethodAttribute.tag);
          // if (ema != null)
          // return classPool.get(ema.className());
        }
      }

    return null;
  }

  //
  // @Override
  // public CtMethod getEnclosingMethod() throws NotFoundException {
  // ClassFile cf = getClassFile2();
  // EnclosingMethodAttribute ema
  // = (EnclosingMethodAttribute)cf.getAttribute(
  // EnclosingMethodAttribute.tag);
  // if (ema != null) {
  // CtClass enc = classPool.get(ema.className());
  // return enc.getMethod(ema.methodName(), ema.methodDescriptor());
  // }
  //
  // return null;
  // }

  @Override
  public CtClass makeNestedClass(String name, boolean isStatic) {
    if (!isStatic)
      throw new RuntimeException("sorry, only nested static class is supported");

    checkModify();
    CtClass c = classPool.makeNestedClass(getName() + "$" + name);
    ClassFile cf = getClassFile2();
    ClassFile cf2 = c.getClassFile2();
    InnerClassesAttribute ica = (InnerClassesAttribute) cf
        .getAttribute(InnerClassesAttribute.tag);
    if (ica == null) {
      ica = new InnerClassesAttribute(cf.getConstPool());
      cf.addAttribute(ica);
    }

    ica.append(c.getName(), this.getName(), name,
        (cf2.getAccessFlags() & ~X_Modifier.SUPER) | X_Modifier.STATIC);
    cf2.addAttribute(ica.copy(cf2.getConstPool(), null));
    return c;
  }

  private static boolean isPubCons(CtConstructor cons) {
    return !X_Modifier.isPrivate(cons.getModifiers()) && cons.isConstructor();
  }

  @Override
  public CtConstructor getConstructor(String desc) throws NotFoundException {
    CtMember.Cache memCache = getMembers();
    CtMember cons = memCache.consHead();
    CtMember consTail = memCache.lastCons();

    while (cons != consTail) {
      cons = cons.next();
      CtConstructor cc = (CtConstructor) cons;
      if (cc.getMethodInfo2().getDescriptor().equals(desc)
          && cc.isConstructor())
        return cc;
    }

    return super.getConstructor(desc);
  }

  @Override
  public CtConstructor[] getDeclaredConstructors() {
    CtMember.Cache memCache = getMembers();
    CtMember cons = memCache.consHead();
    CtMember consTail = memCache.lastCons();

    int n = 0;
    CtMember mem = cons;
    while (mem != consTail) {
      mem = mem.next();
      CtConstructor cc = (CtConstructor) mem;
      if (cc.isConstructor())
        n++;
    }

    CtConstructor[] result = new CtConstructor[n];
    int i = 0;
    mem = cons;
    while (mem != consTail) {
      mem = mem.next();
      CtConstructor cc = (CtConstructor) mem;
      if (cc.isConstructor())
        result[i++] = cc;
    }

    return result;
  }

  @Override
  public CtConstructor getClassInitializer() {
    CtMember.Cache memCache = getMembers();
    CtMember cons = memCache.consHead();
    CtMember consTail = memCache.lastCons();

    while (cons != consTail) {
      cons = cons.next();
      CtConstructor cc = (CtConstructor) cons;
      if (cc.isClassInitializer())
        return cc;
    }

    return null;
  }

  @Override
  public CtMethod[] getMethods() {
    HashMap<String, CtMember> h = new HashMap<String, CtMember>();
    getMethods0(h, this);
    return (CtMethod[]) h.values().toArray(new CtMethod[h.size()]);
  }

  private static void getMethods0(HashMap<String, CtMember> h, CtClass cc) {
    try {
      CtClass[] ifs = cc.getInterfaces();
      int size = ifs.length;
      for (int i = 0; i < size; ++i)
        getMethods0(h, ifs[i]);
    } catch (NotFoundException e) {
    }

    try {
      CtClass s = cc.getSuperclass();
      if (s != null)
        getMethods0(h, s);
    } catch (NotFoundException e) {
    }

    if (cc instanceof CtClassType) {
      CtMember.Cache memCache = ((CtClassType) cc).getMembers();
      CtMember mth = memCache.methodHead();
      CtMember mthTail = memCache.lastMethod();

      while (mth != mthTail) {
        mth = mth.next();
        if (!X_Modifier.isPrivate(mth.getModifiers()))
          h.put(((CtMethod) mth).getStringRep(), mth);
      }
    }
  }

  protected synchronized CtMember.Cache getMembers() {
    CtMember.Cache cache = null;
    if (memberCache == null
        || (cache = (CtMember.Cache) memberCache.get()) == null) {
      cache = new CtMember.Cache(this);
      makeFieldCache(cache);
      makeBehaviorCache(cache);
      memberCache = new WeakReference<Cache>(cache);
    }

    return cache;
  }

  private void makeFieldCache(CtMember.Cache cache) {
    List<?> list = getClassFile2().getFields();
    int n = list.size();
    for (int i = 0; i < n; ++i) {
      // FieldInfo finfo = (FieldInfo)list.get(i);
      // CtField newField = new CtField(finfo, this);
      // cache.addField(newField);
    }
  }

  private void makeBehaviorCache(CtMember.Cache cache) {
    List<?> list = getClassFile2().getMethods();
    int n = list.size();
    for (int i = 0; i < n; ++i) {
      MethodInfo minfo = (MethodInfo) list.get(i);
      if (minfo.isMethod()) {
        CtMethod newMethod = new CtMethod(minfo, this);
        cache.addMethod(newMethod);
      } else {
        // TODO do something with ctors
        // CtConstructor newCons = new CtConstructor(minfo, this);
        // cache.addConstructor(newCons);
      }
    }
  }

  @Override
  public CtMethod getMethod(String name, String desc) throws NotFoundException {
    CtMethod m = getMethod0(this, name, desc);
    if (m != null)
      return m;
    else
      throw new NotFoundException(name + "(..) is not found in " + getName());
  }

  private static CtMethod getMethod0(CtClass cc, String name, String desc) {
    if (cc instanceof CtClassType) {
      CtMember.Cache memCache = ((CtClassType) cc).getMembers();
      CtMember mth = memCache.methodHead();
      CtMember mthTail = memCache.lastMethod();

      while (mth != mthTail) {
        mth = mth.next();
        if (mth.getName().equals(name)
            && ((CtMethod) mth).getMethodInfo2().getDescriptor().equals(desc))
          return (CtMethod) mth;
      }
    }

    try {
      CtClass s = cc.getSuperclass();
      if (s != null) {
        CtMethod m = getMethod0(s, name, desc);
        if (m != null)
          return m;
      }
    } catch (NotFoundException e) {
    }

    try {
      CtClass[] ifs = cc.getInterfaces();
      int size = ifs.length;
      for (int i = 0; i < size; ++i) {
        CtMethod m = getMethod0(ifs[i], name, desc);
        if (m != null)
          return m;
      }
    } catch (NotFoundException e) {
    }
    return null;
  }

  @Override
  public CtMethod[] getDeclaredMethods() {
    CtMember.Cache memCache = getMembers();
    CtMember mth = memCache.methodHead();
    CtMember mthTail = memCache.lastMethod();
    int num = CtMember.Cache.count(mth, mthTail);
    CtMethod[] cms = new CtMethod[num];
    int i = 0;
    while (mth != mthTail) {
      mth = mth.next();
      cms[i++] = (CtMethod) mth;
    }

    return cms;
  }

  @Override
  public CtMethod getDeclaredMethod(String name) throws NotFoundException {
    CtMember.Cache memCache = getMembers();
    CtMember mth = memCache.methodHead();
    CtMember mthTail = memCache.lastMethod();
    while (mth != mthTail) {
      mth = mth.next();
      if (mth.getName().equals(name))
        return (CtMethod) mth;
    }

    throw new NotFoundException(name + "(..) is not found in " + getName());
  }

  @Override
  public CtMethod getDeclaredMethod(String name, CtClass[] params)
      throws NotFoundException {
    String desc = Descriptor.ofParameters(params);
    CtMember.Cache memCache = getMembers();
    CtMember mth = memCache.methodHead();
    CtMember mthTail = memCache.lastMethod();

    while (mth != mthTail) {
      mth = mth.next();
      if (mth.getName().equals(name)
          && ((CtMethod) mth).getMethodInfo2().getDescriptor().startsWith(desc))
        return (CtMethod) mth;
    }

    throw new NotFoundException(name + "(..) is not found in " + getName());
  }

  //
  // @Override
  // public void addField(CtField f, String init)
  // throws CannotCompileException
  // {
  // addField(f, CtField.Initializer.byExpr(init));
  // }
  //
  // @Override
  // public void addField(CtField f, CtField.Initializer init)
  // throws CannotCompileException
  // {
  // checkModify();
  // if (f.getDeclaringClass() != this)
  // throw new CannotCompileException("cannot add");
  //
  // if (init == null)
  // init = f.getInit();
  //
  // if (init != null) {
  // init.check(f.getSignature());
  // int mod = f.getModifiers();
  // if (AccessFlagisStatic(mod) && AccessFlagisFinal(mod))
  // try {
  // ConstPool cp = getClassFile2().getConstPool();
  // int index = init.getConstantValue(cp, f.getType());
  // if (index != 0) {
  // f.getFieldInfo2().addAttribute(new ConstantAttribute(cp, index));
  // init = null;
  // }
  // }
  // catch (NotFoundException e) {}
  // }
  //
  // getMembers().addField(f);
  // getClassFile2().addField(f.getFieldInfo2());
  //
  // if (init != null) {
  // FieldInitLink fil = new FieldInitLink(f, init);
  // FieldInitLink link = fieldInitializers;
  // if (link == null)
  // fieldInitializers = fil;
  // else {
  // while (link.next != null)
  // link = link.next;
  //
  // link.next = fil;
  // }
  // }
  // }
  //
  // @Override
  // public void removeField(CtField f) throws NotFoundException {
  // checkModify();
  // FieldInfo fi = f.getFieldInfo2();
  // ClassFile cf = getClassFile2();
  // if (cf.getFields().remove(fi)) {
  // getMembers().remove(f);
  // gcConstPool = true;
  // }
  // else
  // throw new NotFoundException(f.toString());
  // }
  //
  // @Override
  // public CtConstructor makeClassInitializer()
  // throws CannotCompileException
  // {
  // CtConstructor clinit = getClassInitializer();
  // if (clinit != null)
  // return clinit;
  //
  // checkModify();
  // ClassFile cf = getClassFile2();
  // Bytecode code = new Bytecode(cf.getConstPool(), 0, 0);
  // modifyClassConstructor(cf, code, 0, 0);
  // return getClassInitializer();
  // }
  //
  // @Override
  // public void addConstructor(CtConstructor c)
  // throws CannotCompileException
  // {
  // checkModify();
  // if (c.getDeclaringClass() != this)
  // throw new CannotCompileException("cannot add");
  //
  // getMembers().addConstructor(c);
  // getClassFile2().addMethod(c.getMethodInfo2());
  // }
  //
  // @Override
  // public void removeConstructor(CtConstructor m) throws NotFoundException {
  // checkModify();
  // MethodInfo mi = m.getMethodInfo2();
  // ClassFile cf = getClassFile2();
  // if (cf.getMethods().remove(mi)) {
  // getMembers().remove(m);
  // gcConstPool = true;
  // }
  // else
  // throw new NotFoundException(m.toString());
  // }
  //
  // @Override
  // public void addMethod(CtMethod m) throws CannotCompileException {
  // checkModify();
  // if (m.getDeclaringClass() != this)
  // throw new CannotCompileException("bad declaring class");
  //
  // int mod = m.getModifiers();
  // if ((getModifiers() & AccessFlagINTERFACE) != 0) {
  // m.setModifiers(mod | AccessFlagPUBLIC);
  // if ((mod & AccessFlagABSTRACT) == 0)
  // throw new CannotCompileException(
  // "an interface method must be abstract: " + m.toString());
  // }
  //
  // getMembers().addMethod(m);
  // getClassFile2().addMethod(m.getMethodInfo2());
  // if ((mod & AccessFlagABSTRACT) != 0)
  // setModifiers(getModifiers() | AccessFlagABSTRACT);
  // }
  //
  // @Override
  // public void removeMethod(CtMethod m) throws NotFoundException {
  // checkModify();
  // MethodInfo mi = m.getMethodInfo2();
  // ClassFile cf = getClassFile2();
  // if (cf.getMethods().remove(mi)) {
  // getMembers().remove(m);
  // gcConstPool = true;
  // }
  // else
  // throw new NotFoundException(m.toString());
  // }

  @Override
  public byte[] getAttribute(String name) {
    AttributeInfo ai = getClassFile2().getAttribute(name);
    if (ai == null)
      return null;
    else
      return ai.get();
  }

  @Override
  public void setAttribute(String name, byte[] data) {
    checkModify();
    ClassFile cf = getClassFile2();
    cf.addAttribute(new AttributeInfo(cf.getConstPool(), name, data));
  }

  /**
   * @see javassist.CtClass#prune()
   * @see javassist.CtClass#stopPruning(boolean)
   */
  @Override
  public void prune() {
    if (wasPruned)
      return;

    wasPruned = true;
    getClassFile2().prune();
  }

  @Override
  public void rebuildClassFile() {
    gcConstPool = true;
  }

  // @Override
  // public void toBytecode(DataOutputStream out)
  // throws CannotCompileException, IOException
  // {
  // try {
  // if (isModified()) {
  // checkPruned("toBytecode");
  // ClassFile cf = getClassFile2();
  // if (gcConstPool) {
  // cf.compact();
  // gcConstPool = false;
  // }
  //
  // modifyClassConstructor(cf);
  // modifyConstructors(cf);
  // cf.write(out);
  // out.flush();
  // fieldInitializers = null;
  // if (doPruning) {
  // // to save memory
  // cf.prune();
  // wasPruned = true;
  // }
  // }
  // else {
  // classPool.writeClassfile(getName(), out);
  // // to save memory
  // // classfile = null;
  // }
  //
  // getCount = 0;
  // wasFrozen = true;
  // }
  // catch (NotFoundException e) {
  // throw new CannotCompileException(e);
  // }
  // catch (IOException e) {
  // throw new CannotCompileException(e);
  // }
  // }

  @Override
  public boolean stopPruning(boolean stop) {
    boolean prev = !doPruning;
    doPruning = !stop;
    return prev;
  }

  // private void modifyClassConstructor(ClassFile cf)
  // throws CannotCompileException, NotFoundException
  // {
  // if (fieldInitializers == null)
  // return;
  //
  // Bytecode code = new Bytecode(cf.getConstPool(), 0, 0);
  // Javac jv = new Javac(code, this);
  // int stacksize = 0;
  // boolean doInit = false;
  // for (FieldInitLink fi = fieldInitializers; fi != null; fi = fi.next) {
  // CtField f = fi.field;
  // if (AccessFlagisStatic(f.getModifiers())) {
  // doInit = true;
  // int s = fi.init.compileIfStatic(f.getType(), f.getName(),
  // code, jv);
  // if (stacksize < s)
  // stacksize = s;
  // }
  // }
  //
  // if (doInit) // need an initializer for static fileds.
  // modifyClassConstructor(cf, code, stacksize, 0);
  // }
  //
  // private void modifyClassConstructor(ClassFile cf, Bytecode code,
  // int stacksize, int localsize)
  // throws CannotCompileException
  // {
  // MethodInfo m = cf.getStaticInitializer();
  // if (m == null) {
  // code.add(Bytecode.RETURN);
  // code.setMaxStack(stacksize);
  // code.setMaxLocals(localsize);
  // m = new MethodInfo(cf.getConstPool(), "<clinit>", "()V");
  // m.setAccessFlags(AccessFlag.STATIC);
  // m.setCodeAttribute(code.toCodeAttribute());
  // cf.addMethod(m);
  // CtMember.Cache cache = hasMemberCache();
  // if (cache != null)
  // cache.addConstructor(new CtConstructor(m, this));
  // }
  // else {
  // CodeAttribute codeAttr = m.getCodeAttribute();
  // if (codeAttr == null)
  // throw new CannotCompileException("empty <clinit>");
  //
  // try {
  // CodeIterator it = codeAttr.iterator();
  // int pos = it.insertEx(code.get());
  // it.insert(code.getExceptionTable(), pos);
  // int maxstack = codeAttr.getMaxStack();
  // if (maxstack < stacksize)
  // codeAttr.setMaxStack(stacksize);
  //
  // int maxlocals = codeAttr.getMaxLocals();
  // if (maxlocals < localsize)
  // codeAttr.setMaxLocals(localsize);
  // }
  // catch (BadBytecode e) {
  // throw new CannotCompileException(e);
  // }
  // }
  //
  // try {
  // m.rebuildStackMapIf6(classPool, cf);
  // }
  // catch (BadBytecode e) {
  // throw new CannotCompileException(e);
  // }
  // }
  //
  // private void modifyConstructors(ClassFile cf)
  // throws CannotCompileException, NotFoundException
  // {
  // if (fieldInitializers == null)
  // return;
  //
  // ConstPool cp = cf.getConstPool();
  // List list = cf.getMethods();
  // int n = list.size();
  // for (int i = 0; i < n; ++i) {
  // MethodInfo minfo = (MethodInfo)list.get(i);
  // if (minfo.isConstructor()) {
  // CodeAttribute codeAttr = minfo.getCodeAttribute();
  // if (codeAttr != null)
  // try {
  // Bytecode init = new Bytecode(cp, 0,
  // codeAttr.getMaxLocals());
  // CtClass[] params
  // = Descriptor.getParameterTypes(
  // minfo.getDescriptor(),
  // classPool);
  // int stacksize = makeFieldInitializer(init, params);
  // insertAuxInitializer(codeAttr, init, stacksize);
  // minfo.rebuildStackMapIf6(classPool, cf);
  // }
  // catch (BadBytecode e) {
  // throw new CannotCompileException(e);
  // }
  // }
  // }
  // }
  //
  // private static void insertAuxInitializer(CodeAttribute codeAttr,
  // Bytecode initializer,
  // int stacksize)
  // throws BadBytecode
  // {
  // CodeIterator it = codeAttr.iterator();
  // int index = it.skipSuperConstructor();
  // if (index < 0) {
  // index = it.skipThisConstructor();
  // if (index >= 0)
  // return; // this() is called.
  //
  // // Neither this() or super() is called.
  // }
  //
  // int pos = it.insertEx(initializer.get());
  // it.insert(initializer.getExceptionTable(), pos);
  // int maxstack = codeAttr.getMaxStack();
  // if (maxstack < stacksize)
  // codeAttr.setMaxStack(stacksize);
  // }
  //
  // private int makeFieldInitializer(Bytecode code, CtClass[] parameters)
  // throws CannotCompileException, NotFoundException
  // {
  // int stacksize = 0;
  // Javac jv = new Javac(code, this);
  // try {
  // jv.recordParams(parameters, false);
  // }
  // catch (CompileError e) {
  // throw new CannotCompileException(e);
  // }
  //
  // for (FieldInitLink fi = fieldInitializers; fi != null; fi = fi.next) {
  // CtField f = fi.field;
  // if (!AccessFlagisStatic(f.getModifiers())) {
  // int s = fi.init.compile(f.getType(), f.getName(), code,
  // parameters, jv);
  // if (stacksize < s)
  // stacksize = s;
  // }
  // }
  //
  // return stacksize;
  // }

  // Methods used by CtNewWrappedMethod

  Hashtable<?, ?> getHiddenMethods() {
    if (hiddenMethods == null)
      hiddenMethods = new Hashtable<Object, Object>();

    return hiddenMethods;
  }

  int getUniqueNumber() {
    return uniqueNumberSeed++;
  }

  @Override
  public String makeUniqueName(String prefix) {
    HashMap<String, CtClassType> table = new HashMap<String, CtClassType>();
    makeMemberList(table);
    Set<String> keys = table.keySet();
    String[] methods = new String[keys.size()];
    keys.toArray(methods);

    if (notFindInArray(prefix, methods))
      return prefix;

    int i = 100;
    String name;
    do {
      if (i > 999)
        throw new RuntimeException("too many unique name");

      name = prefix + i++;
    } while (!notFindInArray(name, methods));
    return name;
  }

  private static boolean notFindInArray(String prefix, String[] values) {
    int len = values.length;
    for (int i = 0; i < len; i++)
      if (values[i].startsWith(prefix))
        return false;

    return true;
  }

  private void makeMemberList(HashMap<String, CtClassType> table) {
    int mod = getModifiers();
    if (X_Modifier.isAbstract(mod) || X_Modifier.isInterface(mod))
      try {
        CtClass[] ifs = getInterfaces();
        int size = ifs.length;
        for (int i = 0; i < size; i++) {
          CtClass ic = ifs[i];
          if (ic != null && ic instanceof CtClassType)
            ((CtClassType) ic).makeMemberList(table);
        }
      } catch (NotFoundException e) {
      }

    try {
      CtClass s = getSuperclass();
      if (s != null && s instanceof CtClassType)
        ((CtClassType) s).makeMemberList(table);
    } catch (NotFoundException e) {
    }

    List<?> list = getClassFile2().getMethods();
    int n = list.size();
    for (int i = 0; i < n; i++) {
      MethodInfo minfo = (MethodInfo) list.get(i);
      table.put(minfo.getName(), this);
    }

    list = getClassFile2().getFields();
    n = list.size();
    for (int i = 0; i < n; i++) {
      FieldInfo finfo = (FieldInfo) list.get(i);
      table.put(finfo.getName(), this);
    }
  }
}

// class FieldInitLink {
// FieldInitLink next;
// CtField field;
// CtField.Initializer init;
//
// FieldInitLink(CtField f, CtField.Initializer i) {
// next = null;
// field = f;
// init = i;
// }
// }
