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

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.TreeSet;


public class ClassBuffer extends PrintBuffer{

	protected int privacy;
	private boolean isClass;
	private boolean isStatic, isFinal, isAbstract;
	private String superClass;
	private String simpleName;
	private final Set<String> interfaces;
	private final Set<String> generics;
	private SourceBuilder<?> context;
  private boolean isWellFormatted;
  private PrintBuffer prefix;

	public ClassBuffer() {
	  this(new SourceBuilder<Object>());
	}
	public ClassBuffer(SourceBuilder<?> context) {
		super();
		this.context = context;
		interfaces = new TreeSet<String>();
		generics = new TreeSet<String>();
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(NEW_LINE);
		b.append(indent);
		if (prefix != null)
		  b.append(prefix);
		if (privacy == Modifier.PUBLIC)
			b.append("public ");
		else if (privacy == Modifier.PRIVATE)
		  b.append("private ");
		else if (privacy == Modifier.PROTECTED)
			b.append("protected ");

		if (isStatic)
			b.append("static ");
		if (isAbstract)
			b.append("abstract ");
		if (isFinal)
			b.append("final ");

		if (isClass)
			b.append("class ");
		else
			b.append("interface ");

		b.append(simpleName+" ");

		if (isClass){
			if (superClass != null)
				b.append("extends "+superClass+" ");
			if (interfaces.size()>0){
				b.append("implements ");
				for (String iface : interfaces){
					b.append(iface);
					b.append(", ");
				}
				b.delete(b.length()-2, b.length()-1);
			}
		}else{
			if (interfaces.size()>0){
				b.append("extends ");
				for (String iface : interfaces){
					b.append(iface);
					b.append(", ");
				}
				b.delete(b.length()-2, b.length());
			}
		}
		if (generics.size()>0){
			b.append("<");
			for (String generic : generics){
				b.append(generic);
				b.append(", ");
			}
			b.delete(b.length()-2, b.length());
			b.append("> ");
		}
		b.append("{");
		b.append(NEW_LINE);
		return b + super.toString();
	}

	@Override
	public PrintBuffer addToBeginning(PrintBuffer buffer) {
	  if (prefix == null)
	    prefix = new PrintBuffer();
	  prefix.addToBeginning(buffer);
	  return this;
	}

	public ClassBuffer setDefinition(String definition, boolean wellFormatted) {
		JavaMetadata metadata = new JavaMetadata(definition);
		isWellFormatted = wellFormatted;
		privacy = metadata.getPrivacy();
		isStatic = metadata.isStatic();
		isFinal = metadata.isFinal();
		isAbstract = metadata.isAbstract();
		isClass = metadata.isClass();
		addInterfaces(metadata.getInterfaces());
		superClass = metadata.getSuperClass();
		definition = metadata.getClassName();

		context.getImports().addImports(metadata.getImports());

		if (metadata.hasGenerics()){
			generics.clear();
			String[] generic = metadata.getGenerics();
			for (String s : generic)
				generics.add(s);
		}

		if (definition.contains(" "))
		  throw new TypeDefinitionException("Found ambiguous class definition in "+definition);
		if (definition.length() == 0)
			throw new TypeDefinitionException("Did not have a class name in class definition "+definition);
		simpleName = definition;
		return this;
	}

	public ClassBuffer addInterfaces(String ... interfaces) {
		for (String superInterface : interfaces){
			superInterface = superInterface.trim();
			int index = superInterface.lastIndexOf(".");
			if (index > 0){
				context.getImports().addImport(superInterface);
				this.interfaces.add(superInterface.substring(index+1));
			}else{//assume imports are handled externally
				this.interfaces.add(superInterface);
			}
		}
		return this;
	}
	public ClassBuffer addInterfaces(Class<?> ... interfaces) {
	  for (Class<?> superInterface : interfaces){
	    assert superInterface.isInterface();
      context.getImports().addImport(superInterface.getCanonicalName());
      // TODO make sure the type name is unique before reducing to simple name
      this.interfaces.add(superInterface.getSimpleName());
	  }
	  return this;
	}
	public ClassBuffer addGenerics(String ... generics) {
		for (String generic : generics){
			generic = generic.trim();
			boolean noImport = generic.contains("!");
			if (noImport){
				this.generics.add(generic.replace("!", ""));
			}else{
				//pull out import statements and shorten them
				for (String part : generic.split(" ")){
					int index = part.lastIndexOf(".");
					if (index > 0){
						context.getImports().addImport(part);
						generic = generic.replace(part.substring(0, index+1), "");
					}
				}
				this.generics.add(generic);
			}
		}
		return this;
	}

	public String getSuperClass() {
		return superClass;
	}

	public void setSuperClass(String superClass) {
		this.superClass = superClass;
	}

	public String getSimpleName() {
		return simpleName;
	}

	public void setSimpleName(String className) {
		this.simpleName = className;
	}

	public ClassBuffer createInnerClass(String classDef) {
	  ClassBuffer inner = new ClassBuffer(context);
	  inner.indent = indent + INDENT;
	  inner.setDefinition(classDef, classDef.trim().endsWith("{"));
	  addToEnd(inner);
	  return inner;
	}
	public MethodBuffer createMethod(String methodDef) {
	  MethodBuffer method = new MethodBuffer(context, indent+INDENT);
	  method.setDefinition(methodDef);
	  addToEnd(method);
	  return method;
	}

	@Override
	protected String footer() {
	  return isWellFormatted ? "" : "\n" + indent+"}\n";
	}

  // Sigh...  to create a fluent api w/out generics,
  // we have to override a bunch of methods...

  @Override
  public ClassBuffer append(boolean b) {
    super.append(b);
    return this;
  }

  @Override
  public ClassBuffer append(char c) {
    super.append(c);
    return this;
  }

  @Override
  public ClassBuffer append(char[] str) {
    super.append(str);
    return this;
  }

  @Override
  public ClassBuffer append(char[] str, int offset, int len) {
    super.append(str, offset, len);
    return this;
  }

  @Override
  public ClassBuffer append(CharSequence s) {
    super.append(s);
    return this;
  }

  @Override
  public ClassBuffer append(CharSequence s, int start, int end) {
    super.append(s, start, end);
    return this;
  }

  @Override
  public ClassBuffer append(double d) {
    super.append(d);
    return this;
  }

  @Override
  public ClassBuffer append(float f) {
    super.append(f);
    return this;
  }

  @Override
  public ClassBuffer append(int i) {
    super.append(i);
    return this;
  }

  @Override
  public ClassBuffer append(long lng) {
    super.append(lng);
    return this;
  }

  @Override
  public ClassBuffer append(Object obj) {
    super.append(obj);
    return this;
  }

  @Override
  public ClassBuffer append(String str) {
    super.append(str);
    return this;
  }

  @Override
  public ClassBuffer indent() {
    super.indent();
    return this;
  }

  @Override
  public ClassBuffer indentln(char[] str) {
    super.indentln(str);
    return this;
  }

  @Override
  public ClassBuffer indentln(CharSequence s) {
    super.indentln(s);
    return this;
  }

  @Override
  public ClassBuffer indentln(Object obj) {
    super.indentln(obj);
    return this;
  }

  @Override
  public ClassBuffer indentln(String str) {
    super.indentln(str);
    return this;
  }

  @Override
  public ClassBuffer outdent() {
    super.outdent();
    return this;
  }

  @Override
  public ClassBuffer println() {
    super.println();
    return this;
  }

  @Override
  public ClassBuffer println(char[] str) {
    super.println(str);
    return this;
  }

  @Override
  public ClassBuffer println(CharSequence s) {
    super.println(s);
    return this;
  }

  @Override
  public ClassBuffer println(Object obj) {
    super.println(obj);
    return this;
  }

  @Override
  public ClassBuffer println(String str) {
    super.println(str);
    return this;
  }
  public String getPackage() {
    return context.getRepackage();
  }
  public ClassBuffer addImports(String ... imports) {
    context.getImports().addImports(imports);
    return this;
  }
  public ClassBuffer addImports(Class<?> ... imports) {
    context.getImports().addImports(imports);
    return this;
  }

}
