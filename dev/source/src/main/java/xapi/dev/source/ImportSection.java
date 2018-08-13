/*
 * Copyright 2013, We The Internet Ltd.
 *
 * All rights reserved.
 *
 * Distributed under a modified BSD License as follow:
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistribution in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution, unless otherwise
 * agreed to in a written document signed by a director of We The Internet Ltd.
 *
 * Neither the name of We The Internet nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package xapi.dev.source;

import xapi.fu.In1;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static xapi.dev.source.PrintBuffer.NEW_LINE;
import static xapi.fu.iterate.ArrayIterable.iterate;

public class ImportSection implements CanAddImports {

  public static class PackageAwareImports extends ImportSection implements HasPackage {

    private String packageName;

    @Override
    public String getPackageName() {
      return packageName;
    }

    public void setPackageName(String packageName) {
      this.packageName = packageName;
    }
  }

  // It's ok to use a plain hashmap, we sort all imports before printing.
  private final Map<String, String> imports = new HashMap<>();
  private final Map<String, String> importStatic = new HashMap<>();
  private final Set<String> starImports = new HashSet<>();
  private boolean canIgnoreOwnPackage;
  private boolean replaceDollarSign;

  public ImportSection() {
    canIgnoreOwnPackage = true;
  }

  public boolean hasStarImports() {
    return !starImports.isEmpty();
  }

  public ImportSection addImports(final String... imports) {
    for (final String iport : imports) {
      addImport(iport);
    }
    return this;
  }

  public String addImport(final String importName) {
    return tryImport(importName, importName.contains("static "), false);
  }

  public String addImport(final String importName, boolean skipNoPackages) {
    return tryImport(importName, importName.contains("static "), skipNoPackages);
  }

  public String addStaticImport(final Class<?> cls, final String importName) {
    return addStaticImport(cls.getCanonicalName(), importName);
  }

  public String addStaticImport(final String cls, final String importName) {
    final boolean hasStatic = importName!=null&&importName.length()>0;
    return tryImport(cls+(hasStatic ? "."+importName : ""), hasStatic, shouldReplaceDollarSign());
  }

  public boolean shouldReplaceDollarSign() {
    return replaceDollarSign;
  }

  public String addStaticImport(final String importName) {
    return tryImport(importName, true, false);
  }

  public ImportSection addStatics(final String... imports) {
    for (final String iport : imports) {
      addStaticImport(iport);
    }
    return this;
  }

  @Override
  public final String toString() {
    return toSource();
  }

  public String toSource() {
    final StringBuilder b = new StringBuilder();

    // Print static imports first
    String[] values = importStatic.values().toArray(new String[importStatic.size()]);
    // nicely sorted
    Arrays.sort(values);
    printImports(b, values, "import static ");
    if (values.length > 0) {
      b.append(NEW_LINE);
    }

    values = imports.values().toArray(new String[imports.size()]);
    Arrays.sort(values);
    printImports(b, values, "import ");
    return b.toString();
  }

  private void printImports(final StringBuilder b, final String[] values, final String importType) {
    if (values.length == 0) {
      return;
    }
    String prefix = prefixOf(values[0]);
    for (final String importName : values) {
      if (importName.length() > 0) {
        final String newPrefix = prefixOf(importName);
        if (!newPrefix.equals(prefix)) {
          b.append(NEW_LINE);
          prefix = newPrefix;
        }
        b.append(importType).append(importName).append(';').append(NEW_LINE);
      }
    }
  }

  /**
   * We keep track of the prefix of each import,
   * so we can group similar packages together.
   *
   * This uses the first packagename,
   * so all com.* and org.* will be grouped together,
   * but for concise packagenames like xapi.* or elemental.*,
   * this will ensure we don't get a spamming of newlines.
   *
   */
  private String prefixOf(final String string) {
    final int ind = string.indexOf('.');
    return ind == -1 ? string : string.substring(0, ind);
  }

  public String tryReserveSimpleName(final String simple, final String full) {
    String was = imports.get(simple);
    if (was == null) {
      imports.put(simple, "");
    } else {
      if (simple.equals(full) && !simple.contains(".")) {
        return simple;
      }
      if (!was.equals(full) && !"".equals(was)) {
        // will throw exception for us
        reserveSimpleName(simple);
      }
    }
    return simple;
  }
  public String reserveSimpleName(final String ... classes) {
    for (String cls : classes) {
      if (!imports.containsKey(cls)) {
        imports.put(cls, "");
        return cls;
      }
    }
    throw new IllegalArgumentException("The names " + iterate(classes) + " are unavailable");
  }

  public String addImport(final Class<?> cls) {
    if (cls.isPrimitive() || "java.lang".equals(cls.getPackage().getName())) {
      return cls.getSimpleName();
    }
    final String existing = imports.get(cls.getSimpleName());
    if (existing != null) {
      if (existing.equals(cls.getCanonicalName())) {
        return cls.getSimpleName();
      }
      return cls.getCanonicalName();
    }
    imports.put(cls.getSimpleName(), cls.getCanonicalName());
    return cls.getSimpleName();
  }

  public ImportSection addImports(final Class<?>... imports) {
    for (final Class<?> cls : imports) {
      addImport(cls);
    }
    return this;
  }

  protected boolean canMinimize(final String importName) {
    final String simpleName = importName.substring(importName.lastIndexOf('.') + 1);
    final String existing = imports.get(simpleName);

    return existing == null || // No type for this simple name
        "".equals(existing) || // This simple name was reserved
        existing.equals(importName); // This type is already imported
  }

  protected String tryImport(final String importName, final boolean staticImport, boolean skipNoPackage) {
    return tryImport(importName, staticImport, shouldReplaceDollarSign(), skipNoPackage);
  }

  protected String tryImport(String importName, final boolean staticImport, final boolean replaceDollarSigns, final boolean skipNoPackages) {
    final String originalImport = importName;
    final Map<String, String> map = staticImport ? importStatic : imports;
    int arrayDepth = 0;
    int index = importName.indexOf(".");
    // do not import primitives
    final boolean hasDot = index != -1;
    importName = trim(importName);

    // rip off generics, and optionally try to import them as well
    // Be sure to do generics before array types, or else List<String[]> will become List<String>[]
    index = importName.indexOf('<');
    String suffix;
    if (index == -1) {
      suffix = "";
    } else {
      int endIndex = importName.lastIndexOf('>');
      suffix = importName.substring(index, endIndex);
      suffix = importFullyQualifiedNames(suffix);
      if (endIndex < importName.length()) {
        String tail = importName.substring(endIndex);
        suffix += tail;
      }
      importName = importName.substring(0, index);
    }
    if (hasDot) {
      if (Character.isUpperCase(importName.charAt(0))) {
        // Trying to add a package-local nested type...
        tryReserveSimpleName(importName.substring(importName.lastIndexOf('.')+1), importName);
        return importName + suffix;
      }
    } else {
      return tryReserveSimpleName(importName, importName) + suffix;
    }

    // ignore any []
    index = importName.indexOf("[]");
    while (index != -1) {
      importName = importName.substring(0, index)
          + (index < importName.length() - 2 ? importName.substring(index + 2)
          : "");
      index = importName.indexOf("[]", index);
      arrayDepth++;
    }


    // put back any array definitions we ignored
    while (arrayDepth-- > 0) {
      suffix += "[]";
    }

    // check if we need to import this type...
    if (skipImports(importName, skipNoPackages)) {
      if (importName.startsWith("java.lang.")) {
        final String was = importName;
        importName = was.substring(10);
        tryReserveSimpleName(importName, was);
      }
      return importName + suffix;
    }

    // a name with a . in it; check if we need to import it
    if (!staticImport && canIgnoreOwnPackage() && this instanceof HasPackage) {
      String pkg = ((HasPackage)this).getPackageName();
      if (pkg != null && !pkg.isEmpty()) {
        final String noPkg = importName.replace(pkg + ".", "");
        if (noPkg.indexOf('.') == -1) {
          String existing = map.get(noPkg);
          if (existing == null || existing.isEmpty()) {
            map.put(noPkg, importName);
            return noPkg + suffix;
          } else {
            if (importName.equals(existing)) {
              return noPkg + suffix;
            } else {
              return existing + suffix;
            }
          }
        }
      }
    }

    if (!staticImport && skipImports(importName, skipNoPackages)) {
      return importName.replace("java.lang.", "") + suffix;
    }
    if (replaceDollarSigns) {
      importName = importName.replace('$', '.');
    }
    final String shortname = importName.substring(1 + importName.lastIndexOf('.'));
    if ("*".equals(shortname)) {
      map.put(importName, importName);
      assert suffix.length() == 0 : "Bad import; has a suffix with a * import: " + originalImport;
      return importName;
    }

    final String existing = map.get(shortname);
    if (existing == null) {
      map.put(shortname, importName);
      return shortname + suffix;
    }
    // if the existing match was this classname, we are allowed to return shortname
    if (existing.equals(importName)) {
      return shortname + suffix;
    }
    // if there was an existing name that wasn't us, we can't perform the import.
    return importName + suffix;
  }

  public String importFullyQualifiedNames(String suffix) {
    // use two builders: one for the final result,
    StringBuilder result = new StringBuilder();
    // and the other for java packagenames (java identifiers and dots)
    StringBuilder word = new StringBuilder();

    // we'll want to use the logic to clear the word buffer more than once...
    In1<Boolean> tryImport = hasDot->{
      if (word.length() > 0) {
        if (hasDot) {
          String imported = addImport(word.toString(), true);
          result.append(imported);
        } else {
          result.append(word);
        }
        word.setLength(0);
      }
    };

    // loop through whole string
    boolean hasDot = false;
    int pos = 0;
    while (pos < suffix.length()) {
      char c = suffix.charAt(pos++);
      // record . and java identifiers in word builders
      if (c == '.') {
        hasDot = true;
        word.append(c);
      } else {
        if (Character.isJavaIdentifierPart(c)) {
          word.append(c);
        } else {
          tryImport.in(hasDot);
          hasDot = false;
          result.append(c);
        }
      }
    }
    tryImport.in(hasDot);
    return result.toString();
  }

  protected boolean canIgnoreOwnPackage() {
    return canIgnoreOwnPackage;
  }

  private boolean skipImports(final String importName, boolean skipSingleNames) {
    if (importName.matches("("
        + "(java[.]lang.[^.]*)" + // discard java.lang, but keep java.lang.reflect, etc.
        "|" +  // also discard primitives
          "(void)|(boolean)|(short)|(char)|(int)|(long)|(float)|(double)"
        + "|extends|super|import|static|[?]"
        + "|(String)|(Class)|(Object)|(Void)|(Boolean)|(Short)|(Character)|(Integer)|(Long)|(Float)|(Double)|(Iterable))"
        + "[;]*")) {
      return true;
    }
    return skipSingleNames && importName.indexOf('.') == -1;
  }

  private String trim(final String importName) {
    return importName.replaceAll(
        //"(\\[\\s*\\])|" +
        "(\\s*import\\s+)|" +
        "(\\s*static\\s+)|" +
        "(\\s*;\\s*)", "");
  }

  @Override
  public ImportSection getImports() {
    return this;
  }

  public void setCanIgnoreOwnPackage(boolean canIgnoreOwnPackage) {
    this.canIgnoreOwnPackage = canIgnoreOwnPackage;
  }

  public void setReplaceDollarSign(boolean replaceDollarSign) {
    this.replaceDollarSign = replaceDollarSign;
  }
}
