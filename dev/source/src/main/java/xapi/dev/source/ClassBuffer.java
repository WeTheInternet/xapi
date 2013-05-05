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

import xapi.source.read.JavaLexer;


public class ClassBuffer extends MemberBuffer<ClassBuffer>{

	protected int privacy;
	private boolean isClass;
	private String superClass;
	private String simpleName;
	private final Set<String> interfaces;
	protected SourceBuilder<?> context;
  private boolean isWellFormatted;
  private PrintBuffer prefix;

	public ClassBuffer() {
	  this(new SourceBuilder<Object>());
	}
	public ClassBuffer(SourceBuilder<?> context) {
	  this(context, "");
	}
	public ClassBuffer(SourceBuilder<?> context, String indent) {
	  super(indent);
	  indent();
	  this.context = context;
	  interfaces = new TreeSet<String>();
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(NEW_LINE);
		b.append(NEW_LINE);
		if (annotations.size() > 0) {
		  for (String anno : annotations) {
		    b.append(origIndent).append("@").append(anno).append(NEW_LINE);
		  }
		}
		b.append(origIndent);
		if (prefix != null)
		  b.append(prefix);
		if (privacy == Modifier.PUBLIC)
			b.append("public ");
		else if (privacy == Modifier.PRIVATE)
		  b.append("private ");
		else if (privacy == Modifier.PROTECTED)
			b.append("protected ");

		if (isStatic())
			b.append("static ");
		if (isAbstract())
			b.append("abstract ");
		if (isFinal())
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

	protected String superString() {
	  return super.toString();
	}

	@Override
	public PrintBuffer addToBeginning(PrintBuffer buffer) {
	  if (prefix == null)
	    prefix = new PrintBuffer();
	  prefix.addToBeginning(buffer);
	  return this;
	}

	public ClassBuffer setDefinition(String definition, boolean wellFormatted) {
		JavaLexer metadata = new JavaLexer(definition);
		isWellFormatted = wellFormatted;
		privacy = metadata.getPrivacy();
		if (metadata.isStatic())makeStatic();
		if (metadata.isFinal())makeFinal();
		if (metadata.isAbstract())makeAbstract();
		else makeConcrete();
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
			addInterface(superInterface);
		}
		return this;
	}

	public ClassBuffer addInterface(String iface) {
	  iface = iface.trim();
	  if (iface.indexOf('.') > 0)
	    iface = context.getImports().addImport(iface);
    this.interfaces.add(iface);
    return this;
	}

	public ClassBuffer addInterfaces(Class<?> ... interfaces) {
	  for (Class<?> superInterface : interfaces){
	    addInterface(superInterface);
	  }
	  return this;
	}

	public ClassBuffer addInterface(Class<?> iface) {
	  assert iface.isInterface();
    String name = context.getImports().addImport(iface.getCanonicalName());
    this.interfaces.add(name);
	  return this;
	}

	@Override
  public String addImport(String importName) {
	  // Don't import types in the same package as us.
	  if (importName.startsWith(getPackage())) {
	    // Make sure it's not in a sub-package
	    String stripped = importName.substring(getPackage().length()+1);
	    // Assuming java camel case naming convention.
	    if (Character.isUpperCase(stripped.charAt(0))){
	      // This is our package.  Don't import, but do try to strip package.
	      if (context.getImports().canMinimize(importName)) {
	        context.getImports().reserveSimpleName(stripped.substring(stripped.lastIndexOf('.')+1));
	        return stripped;
	      } else {
	        return importName;
	      }
	    }
	  }
	  return context.getImports().addImport(importName);
	}

	@Override
  public String addImport(Class<?> cls) {
	  return context.getImports().addImport(cls);
	}

	public String getSuperClass() {
		return superClass;
	}

	public void setSuperClass(String superClass) {
		this.superClass = superClass;
	}

	public String getPackage() {
    return context.getPackage();
  }
  public String getSimpleName() {
		return simpleName;
	}

	public void setSimpleName(String className) {
		this.simpleName = className;
	}

	public ClassBuffer createInnerClass(String classDef) {
	  ClassBuffer inner = new ClassBuffer(context, memberIndent());
	  inner.setDefinition(classDef, classDef.trim().endsWith("{"));
	  addToEnd(inner);
	  return inner;
	}

	public ClassBuffer createAnonymousClass(final String classDef) {
	  class AnonymousClass extends ClassBuffer {
	    public AnonymousClass(SourceBuilder<?> context, String indent) {
	      super(context, indent);
      }

	    @Override
	    public String toString() {
	      return
	        NEW_LINE + origIndent+
	        "new "+classDef+"() {" +NEW_LINE+
	      	superString();
	    }

      @Override
	    protected String memberIndent() {
	      return indent + INDENT;
	    }
	  }
	  ClassBuffer inner = new AnonymousClass(context, indent+INDENT);
	  inner.setDefinition(classDef, classDef.trim().endsWith("{"));
	  addToEnd(inner);
	  return inner;
	}

	protected String memberIndent() {
    return origIndent + INDENT;
  }

  public MethodBuffer createMethod(String methodDef) {
	  MethodBuffer method = new MethodBuffer(context, memberIndent());
	  method.setDefinition(methodDef);
	  addToEnd(method);
	  return method;
	}

	public FieldBuffer createField(Class<?> type, String name) {
	  return createField(type.getCanonicalName(), name);
	}

	public FieldBuffer createField(Class<?> type, String name, int modifier) {
	  FieldBuffer field = new FieldBuffer(this, type.getCanonicalName(), name, memberIndent());
	  field.setModifier(modifier);
	  addToEnd(field);
	  return field;
	}

	public FieldBuffer createField(String type, String name) {
	  FieldBuffer field = new FieldBuffer(this, type, name, memberIndent());
	  addToEnd(field);
	  return field;
	}

	public FieldBuffer createField(String type, String name, int modifier) {
	  FieldBuffer field = new FieldBuffer(this, type, name, memberIndent());
	  field.setModifier(modifier);
	  addToEnd(field);
	  return field;
	}

	@Override
	protected String footer() {
	  return isWellFormatted ? "" : "\n" + origIndent+"}\n";
	}

  @Override
  public final ClassBuffer makeAbstract() {
    return super.makeAbstract();
  }

}
