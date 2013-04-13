package xapi.dev.source;

import xapi.source.Modifier;
import xapi.source.impl.AbstractClass;
import xapi.source.service.SourceService;
import xapi.util.X_String;


public class MutableClass extends AbstractClass{

  protected MutableClass(SourceService service, String pkg, String simple, int modifiers) {
    super(service, pkg, simple, modifiers);
  }

  public static MutableClass create(String pkg, String cls) {
    assert cls.indexOf('.')== -1 && cls.indexOf('$') == -1 :
      "Do not send inner class definitions to MutableClass.create(package, class);" +
      "\nInstead use MutableClass.create(package, enclosing, class);";
    SourceContext ctx = SourceContext.getContext();
    return new MutableClass(ctx.getService(), pkg, cls, Modifier.PUBLIC);
  }
  public static MutableClass create(String pkg, String enclosing, String cls) {
    if (X_String.isEmpty(enclosing)) {
      return create(pkg, cls);
    }
    // Prefer binary java/lang/Naming$Syntax
    enclosing = enclosing.replace('.', '$');// single char replace is fast
    String[] hierarchy = enclosing.split("\\$");
    MutableClass parent = create(pkg, hierarchy[0]);
    for (int i = 1, m = hierarchy.length; i < m; i++ ) {
      parent = parent.createInnerClass(hierarchy[1]);
    }
    return parent.createInnerClass(cls);
  }

  public MutableClass createInnerClass(String string) {
    return null;
  }

}
