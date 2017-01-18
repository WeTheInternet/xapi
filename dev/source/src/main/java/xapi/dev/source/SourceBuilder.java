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

public class SourceBuilder<Payload> implements CanAddImports {

  protected class SourceBuilderImports extends ImportSection implements HasPackage {

    @Override
    public String getPackageName() {
      return SourceBuilder.this.getPackage();
    }
  }

  public enum JavaType {
    CLASS("class"),
    INTERFACE("interface"),
    ENUM("enum"),
    ANNOTATION("@interface"),
    UNKNOWN("");

    private final String keyword;

    JavaType(String keyword) {
      this.keyword = keyword;
    }
    public void initialize(SourceBuilder builder, String pkg, String filename) {
      builder.setClassDefinition("public " + keyword + " " + filename, false);
      builder.setPackage(pkg);
    }
  }

  private final PrintBuffer head;
  private PrintBuffer buffer;
  private SourceBuilderImports imports;
  private ClassBuffer classDef;
  private Payload payload;
  private String pkgName = "";
  private int skip;

  public SourceBuilder() {
    head = buffer = new PrintBuffer();
  }

  public SourceBuilder(Payload payload) {
    this();
    setPayload(payload);
  }

  public SourceBuilder(String classDef) {
    this();
    setClassDefinition(classDef, classDef.trim().endsWith("{"));
  }

  public SourceBuilder<Payload> replaceSource(CharSequence src) {
    destroy();
    head.clear();
    head.append(src);
    head.println();

    return this;
  }

  public PrintBuffer getBuffer() {
    return buffer;
  }

  public ClassBuffer getClassBuffer() {
    if (classDef == null) {
      throw new TypeDefinitionException(
          "setClassDefinition() has not been called yet.\n"
              + "If you are running the template generator, your template "
              + "does include a //@classDefinition()// declaration,\n"
              + "or your generator is attempting to access the class "
              + "definition before it is parsed.");
    }
    return classDef;
  }

  protected void setBuffer(ClassBuffer from) {
    // make sure import buffer comes before class def
    getImports();
    classDef = from;
    // create a new print buffer for content after class definition
    head.addToEnd(classDef);
    head.setNotIndent();
    addBuffer(new PrintBuffer());
  }

  public SourceBuilder<Payload> setClassDefinition(String definition,
      boolean wellFormatted) {
    if (classDef == null) {
      setBuffer(new ClassBuffer(this, null, ""));
    }
    classDef.setDefinition(definition, wellFormatted);
    return this;
  }

  public ImportSection getImports() {
    if (imports == null) {
      imports = new SourceBuilderImports();
    }
    return imports;
  }

  public Payload getPayload() {
    return payload;
  }

  protected SourceBuilder<Payload> addBuffer(PrintBuffer newBuffer) {
    if (newBuffer == buffer) {
      return this;
    }
    head.addToEnd(newBuffer);
    head.setNotIndent();
    buffer = newBuffer;
    return this;
  }

  public SourceBuilder<Payload> setPayload(Payload payload) {
    this.payload = payload;
    return this;
  }

  public String getPackage() {
    return pkgName;
  }

  public SourceBuilder<Payload> setPackage(String pkgName) {
    if (pkgName.endsWith(";")) {
      pkgName = pkgName.substring(0, pkgName.length() - 1);
    }
    if (pkgName.startsWith("package ")) {
      pkgName = pkgName.substring(8);
    }
    this.pkgName = pkgName;
    return this;
  }

  public SourceBuilder<Payload> addImports(Class<?>... cls) {
    getImports().addImports(cls);
    return this;
  }

  public SourceBuilder<Payload> addImports(String... cls) {
    getImports().addImports(cls);
    return this;
  }

  public SourceBuilder<Payload> addInterfaces(Class<?>... cls) {
    getClassBuffer().addInterfaces(cls);
    return this;
  }

  public SourceBuilder<Payload> addInterfaces(String... cls) {
    getClassBuffer().addInterfaces(cls);
    return this;
  }

  public SourceBuilder<Payload> setSuperClass(Class<?> cls) {
    getClassBuffer().setSuperClass(cls);
    return this;
  }

  public SourceBuilder<Payload> setSuperClass(String cls) {
    getClassBuffer().setSuperClass(cls);
    return this;
  }

  public SourceBuilder<Payload> setLinesToSkip(int i) {
    this.skip = i;
    return this;
  }

  public int getLinesToSkip() {
    try {
      return skip;
    } finally {
      skip = 0;
    }
  }

  @Override
  public final String toString() {
    return toSource();
  }

  public String toSource() {
    StringBuilder source = new StringBuilder();
    String body = head.toSource();
    if (pkgName.length() > 0) {
      if (body.trim().startsWith("package")) {
        int ind = body.indexOf(';', body.indexOf("package"));
        body = body.substring(ind + 1);
      }
      source.append("package " + pkgName + ";\n\n");
    }
    if (imports != null) {
      source.append(imports.toSource());
    }
    source.append(body);
    return source.toString();
  }

  public String getQualifiedName() {
    return getClassBuffer().getQualifiedName();
  }

  public String getSourceFileName() {
    final String pkg = getPackage();
    if (pkg.length() == 0) {
      return getSimpleName() + ".java";
    }
    return pkg.replace('.', '/') + "/" + getSimpleName() + ".java";
  }

  public String getClassFileName() {
    final String pkg = getPackage();
    if (pkg.length() == 0) {
      return getSimpleName() + ".class";
    }
    return pkg.replace('.', '/') + "/" + getSimpleName() + ".class";
  }

  public String getSimpleName() {
    return getClassBuffer().getSimpleName();
  }

  public void destroy() {
    setPayload(null);
    head.tail = null;
    head.head = null;
    buffer = null;
    imports = null;
    classDef = null;
  }

  public boolean isDefined() {
    return classDef != null;
  }
}
