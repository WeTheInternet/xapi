package xapi.dev.source;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.TreeSet;

public abstract class MemberBuffer <Self extends MemberBuffer<Self>> extends PrintBuffer{


  protected final Set<String> annotations;
  protected final Set<String> generics;
  protected final String origIndent;

  protected int modifier = Modifier.PUBLIC;

  public MemberBuffer() {
    this("");
  }

  public MemberBuffer(String indent) {
    this.indent = indent;
    origIndent = indent;
    annotations = new TreeSet<String>();
    generics = new TreeSet<String>();
  }

  @SuppressWarnings("unchecked")
  private final Self self(){
    return (Self)this;
  }

  public final Self addGenerics(String ... generics) {
    for (String generic : generics){
      generic = generic.trim();
      boolean noImport = generic.contains("!");
      if (noImport){
        this.generics.add(generic.replace("!", ""));
      }else{
        //pull fqcn into import statements and shorten them
        for (String part : generic.split(" ")){
          int index = generic.lastIndexOf(".");
          if (index > 0){
            String shortened = addImport(part);
            if (shortened.length() != part.trim().length()){
              // We can safely strip package names.
              generic = generic.replace(part.substring(0, part.length()-shortened.length()), "");
            }
          }
        }
        this.generics.add(generic);
      }
    }
    return self();
  }

  public abstract String addImport(String cls);
  public abstract String addImport(Class<?> cls);

  public final Self addImports(String ... clses) {
    for (String cls : clses) {
      addImport(cls);
    }
    return self();
  }
  public final Self addImports(Class<?> ... clses) {
    for (Class<?> cls : clses) {
      addImport(cls);
    }
    return self();
  }


  public final Self addAnnotation(Class<?> anno) {
    this.annotations.add(addImport(anno));
    return self();
  }
  public final Self addAnnotation(String anno) {
    if (anno.charAt(0) == '@')
      anno = anno.substring(1);
    anno = anno.trim();// never trust user input :)

    int openParen = anno.indexOf('(');
    if (openParen == -1) {
      int hasPeriod = anno.lastIndexOf('.');
      if (hasPeriod != -1) {
        //fqcn is the whole string.
        anno = addImport(anno);
      }
    } else {
      // Need to check fqcn for imports
      int hasPeriod = anno.lastIndexOf('.', openParen);
      if (hasPeriod != -1) {
        //fqcn is the whole string.
        String annoName = addImport(anno.substring(0, openParen));
        anno = annoName + anno.substring(openParen);
      }

    }
    this.annotations.add(anno);
    return self();
  }

  public final Self makeFinal() {
    if ((modifier & Modifier.ABSTRACT) > 0)
      modifier &= ~Modifier.ABSTRACT;// "Cannot be both final and abstract";
    modifier = modifier | Modifier.FINAL;
    return self();
  }

  protected Self makeAbstract() {
    if ((modifier & Modifier.FINAL) > 0)
      modifier &= ~Modifier.FINAL;// "Cannot be both final and abstract";
    if ((modifier & Modifier.STATIC) > 0)
      modifier &= ~Modifier.STATIC;// "Cannot be both static and abstract";
    modifier = modifier | Modifier.ABSTRACT;
    return self();
  }

  public final Self makeStatic() {
    if ((modifier & Modifier.ABSTRACT) > 0)
      modifier &= ~Modifier.ABSTRACT; // "Cannot be both static and abstract";
    modifier = modifier | Modifier.STATIC;
    return self();
  }


  public final Self makeConcrete() {
    modifier = modifier & ~Modifier.ABSTRACT;
    return self();
  }

  public final Self makePublic() {
    modifier = modifier & 0xFFF8 + Modifier.PUBLIC;
    return self();
  }

  public final Self makeProtected() {
    modifier = modifier & 0xFFF8 + Modifier.PROTECTED;
    return self();
  }

  public final Self makePrivate() {
    modifier = modifier & 0xFFF8 + Modifier.PRIVATE;
    return self();
  }

  public final Self makePackageProtected() {
    modifier = modifier & 0xFFF8;
    return self();
  }


  public boolean isStatic() {
    return (modifier & Modifier.STATIC) > 0;
  }

  public boolean isFinal() {
    return (modifier & Modifier.FINAL) > 0;
  }

  protected boolean isAbstract() {
    return (modifier & Modifier.ABSTRACT) > 0;
  }

  public Self setModifier(int modifier) {
    if ((modifier & 7) > 0)
    switch (modifier & 7) {
    case Modifier.PUBLIC:
      makePublic();
      break;
    case Modifier.PRIVATE:
      makePrivate();
      break;
    case Modifier.PROTECTED:
      makeProtected();
      break;
    }
    this.modifier |= modifier;
    return self();
  }


  @Override
  public final Self append(boolean b) {
    super.append(b);
    return self();
  }

  @Override
  public final Self append(char c) {
    super.append(c);
    return self();
  }

  @Override
  public final Self append(char[] str) {
    super.append(str);
    return self();
  }

  @Override
  public final Self append(char[] str, int offset, int len) {
    super.append(str, offset, len);
    return self();
  }

  @Override
  public final Self append(CharSequence s) {
    super.append(s);
    return self();
  }

  @Override
  public final Self append(CharSequence s, int start, int end) {
    super.append(s, start, end);
    return self();
  }

  @Override
  public final Self append(double d) {
    super.append(d);
    return self();
  }

  @Override
  public final Self append(float f) {
    super.append(f);
    return self();
  }

  @Override
  public final Self append(int i) {
    super.append(i);
    return self();
  }

  @Override
  public final Self append(long lng) {
    super.append(lng);
    return self();
  }

  @Override
  public final Self append(Object obj) {
    super.append(obj);
    return self();
  }

  @Override
  public final Self append(String str) {
    super.append(str);
    return self();
  }

  @Override
  public final Self indent() {
    super.indent();
    return self();
  }

  @Override
  public final Self indentln(char[] str) {
    super.indentln(str);
    return self();
  }

  @Override
  public final Self indentln(CharSequence s) {
    super.indentln(s);
    return self();
  }

  @Override
  public final Self indentln(Object obj) {
    super.indentln(obj);
    return self();
  }

  @Override
  public final Self indentln(String str) {
    super.indentln(str);
    return self();
  }

  @Override
  public final Self outdent() {
    super.outdent();
    return self();
  }

  @Override
  public final Self println() {
    super.println();
    return self();
  }

  @Override
  public final Self println(char[] str) {
    super.println(str);
    return self();
  }

  @Override
  public final Self println(CharSequence s) {
    super.println(s);
    return self();
  }

  @Override
  public final Self println(Object obj) {
    super.println(obj);
    return self();
  }

  @Override
  public final Self println(String str) {
    super.println(str);
    return self();
  }

  @Override
  public Self print(String str) {
    super.print(str);
    return self();
  }

}
