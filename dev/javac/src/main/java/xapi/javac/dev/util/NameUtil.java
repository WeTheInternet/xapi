package xapi.javac.dev.util;

import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Name;

public class NameUtil {

  private NameUtil() {} // No NameUtil for you!
  
  public static String getName(JCCompilationUnit unit) {
    Name flatName = unit.packge.flatName();
    String name = unit.sourcefile.getName();
    name = name.substring(0, name.length()-5);
    if (flatName.isEmpty()) {
      int ind = name.lastIndexOf('/');
      return name.substring(ind+1);
    } else {
      String pkg = flatName.toString().replace('.', '/');
      return name.substring(name.indexOf(pkg)+1);
    }
  }

  public static boolean equals(javax.lang.model.element.Name nodeName,
      javax.lang.model.element.Name name) {
    // When comparing names from different compilers, the equals() method 
    // in name is optimized based on object identity, so we must resort to .toString() equality
    return nodeName.toString().equals(name.toString());
  }

}
