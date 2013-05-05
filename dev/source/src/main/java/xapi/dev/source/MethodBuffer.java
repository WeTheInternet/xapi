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
import java.util.LinkedHashSet;
import java.util.Set;

import xapi.collect.impl.SimpleStack;
import xapi.source.read.JavaLexer;
import xapi.source.read.JavaVisitor.AnnotationMemberVisitor;
import xapi.source.read.JavaVisitor.MethodVisitor;
import xapi.source.read.JavaVisitor.ParameterVisitor;
import xapi.source.read.JavaVisitor.TypeData;



public class MethodBuffer
extends MemberBuffer<MethodBuffer>
implements MethodVisitor<SourceBuilder<?>>
{

	protected SourceBuilder<?> context;
	private boolean once;
	private boolean useJsni = true;
	private String methodName;
	private final Set<String> parameters;
	private final Set<String> exceptions;
  private TypeData returnType;

  public MethodBuffer(SourceBuilder<?> context) {
    this(context, INDENT);
  }
	public MethodBuffer(SourceBuilder<?> context, String indent) {
		super(indent);
		this.context = context;
		this.indent = indent + INDENT;
		parameters = new LinkedHashSet<String>();
		exceptions = new LinkedHashSet<String>();
	}
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(NEW_LINE+origIndent);
		if (annotations.size() > 0) {
		  for (String anno : annotations)
		    b.append('@').append(anno).append(NEW_LINE).append(origIndent);
		}
		b.append(Modifier.toString(modifier));
		b.append(" ");
		//generics
		if (generics.size() > 0) {
		  b.append("<");
		  String prefix = "";
		  for (String generic : generics) {
		    b.append(prefix);
		    b.append(generic);
		    prefix = ", ";
		  }
		  b.append("> ");
		}
		//return type
		b.append(returnType).append(" ");
		//method name
		b.append(methodName);
		//parameters
		b.append(" (");
	  String prefix = "";
	  for (String parameter : parameters) {
	    b.append(prefix).append(parameter);
	    prefix = ", ";
	  }
		b.append(") ");
		if (!exceptions.isEmpty()) {
		  b.append("\n"+indent+"  throws ");
		  prefix = "";
		  for (String exception : exceptions) {
		    b.append(prefix).append(exception);
		    prefix = ", ";
		  }
		}
		final String suffix;
		if (Modifier.isAbstract(modifier)) {
		  prefix = ";\n";
		  suffix = "";
		} else if (Modifier.isNative(modifier)) {
		  if (useJsni) {
		    prefix = "/*-{\n";
		    suffix = (once ? NEW_LINE : "") + origIndent + "}-*/;\n";
		  } else {
		    prefix = ";\n";
		    suffix = "";
		  }
		} else {
		  prefix = "{\n";
		  suffix = (once ? NEW_LINE : "") + origIndent + "}\n";
		}
	  return b.toString()+prefix+super.toString()+suffix;
	}

	public void setDefinition(String definition) {
	  // JavaMetadata will extract all modifiers for us
	  JavaLexer.visitMethodSignature(this, context, definition, 0);
	}

  public void addParameters(String ... parameters) {
		for (String parameter : parameters){
		  parameter = parameter.trim();
      int index = parameter.lastIndexOf(' ');
      TypeData type = JavaLexer.extractType(parameter, 0);
      if (type.pkgName.length() > 0) {
        String shortName = context.getImports().addImport(type.getImportName());
        if (type.getSimpleName().startsWith(shortName)) {
          this.parameters.add(type.getSimpleName()+" "+parameter.substring(index+1).trim());
          continue;
        }
      }
      this.parameters.add(parameter);
		}
	}

  @Override
  public String addImport(Class<?> cls) {
    return context.getImports().addImport(cls);
  }

  @Override
  public String addImport(String cls) {
    if (cls.replace(context.getPackage()+".", "").indexOf('.')==-1)
      return cls;
    return context.getImports().addImport(cls);
  }

  public ClassBuffer createInnerClass(String classDef) {
	  ClassBuffer cls = new ClassBuffer(context);
	  cls.setDefinition(classDef, classDef.trim().endsWith("{"));
	  cls.indent = indent + INDENT;
	  assert cls.privacy == 0 : "A local class cannot be "+Modifier.toString(cls.privacy);
	  addToEnd(cls);
	  return cls;
	}

	@Override
	protected void onAppend() {
	  if (once) {
	    once = false;
	    onFirstAppend();
	  }
	  super.onAppend();
	}
	protected void onFirstAppend() {

	}
  /**
   * @param useJsni - Whether to encapsulate native methods with /*-{ }-* /
   * @return
   */
  public MethodBuffer setUseJsni(boolean useJsni) {
    this.useJsni = useJsni;
    modifier = modifier | Modifier.NATIVE;
    return this;
  }


  public final MethodBuffer makeJsni() {
    setUseJsni(true).makeFinal();
    return this;
  }
  public final MethodBuffer makeNative() {
    if ((modifier & Modifier.ABSTRACT) > 0)
      modifier &= ~Modifier.ABSTRACT;// "Cannot be both native and abstract";
    modifier = modifier | Modifier.NATIVE;
    return this;
  }

  @Override
  public final MethodBuffer makeAbstract() {
    return super.makeAbstract();
  }

  public MethodBuffer addThrowsClause(Class<?> ... throwables) {
    context.getImports().addImports(throwables);
    for (Class<?> cls : throwables) {
      exceptions.add(cls.getSimpleName());
    }
    return this;
  }
  public MethodBuffer addThrowsClause(String ... throwables) {
    for (String cls : throwables) {
      int ind = cls.lastIndexOf('.');
      if (ind == -1)
        exceptions.add(cls);
      else {
        context.getImports().addImport(cls);
        exceptions.add(cls.substring(ind+1));
      }
    }
    return this;
  }
  public MethodBuffer returnValue(String name) {
    return println("return " +name+(name.endsWith(";")?"":";"));
  }

  @Override
  public ParameterVisitor<SourceBuilder<?>> visitParameter() {
    return new ParameterVisitor<SourceBuilder<?>>() {

      int modifier;
      private SimpleStack<String> annotations = new SimpleStack<String>();
      @Override
      public AnnotationMemberVisitor<SourceBuilder<?>> visitAnnotation(String annoName, String annoBody,
          SourceBuilder<?> receiver) {
        annoName = addImport(annoName.startsWith("@")?annoName.substring(1):annoName);
        annotations.add("@"+annoName+
            (annoBody.length()>0?"("+annoBody+")":""));
        return null;
      }

      @Override
      public void visitModifier(int modifier, SourceBuilder<?> receiver) {
        modifier |= modifier;
      }

      @Override
      public void visitType(TypeData type, String name, boolean varargs,
          SourceBuilder<?> receiver) {
        if (type.pkgName.length() > 0)
          receiver.getImports().addImport(type.getImportName());
        StringBuilder b = new StringBuilder();
        for (String anno : annotations) {
          b.append(anno).append(' ');
        }
        b.append(Modifier.toString(modifier));

        if (varargs) {
          b.append(type.getSimpleName().replace("[]", "")+" ... "+name);
        } else {
          b.append(type.getSimpleName()+" "+name);
        }
        parameters.add(b.toString());
      }
    };
  }

  @Override
  public void visitException(String type, SourceBuilder<?> receiver) {
    exceptions.add(type);
  }
  @Override
  public AnnotationMemberVisitor<SourceBuilder<?>> visitAnnotation(String annoName, String annoBody,
      SourceBuilder<?> receiver) {
    addAnnotation("@"+annoName+(annoBody.trim().length()>0 ? "("+annoBody+")":""));
    return null;
  }
  @Override
  public void visitModifier(int modifier, SourceBuilder<?> receiver) {
    assert validModification(this.modifier, modifier);
    setModifier(modifier);
  }

  private boolean validModification(int modifier, int change) {
    if ((change & Modifier.ABSTRACT) > 0) {
      if ( (modifier & Modifier.STATIC) > 0)
          throw new AssertionError("You cannot make a static method abstract.\n"+this);
      if ((modifier & Modifier.FINAL) > 0)
        throw new AssertionError("You cannot make a final method abstract.\n"+this);
    }
    if ((modifier & Modifier.ABSTRACT) > 0) {
      if ( (change & Modifier.STATIC) > 0)
        throw new AssertionError("You cannot make an abstract method static.\n"+this);
      if ((change & Modifier.FINAL) > 0)
        throw new AssertionError("You cannot make an abstract method final.\n"+this);
    }
    return true;
  }

  @Override
  public void visitGeneric(String generic, SourceBuilder<?> receiver) {
    generic = generic.trim();
    if (generic.charAt(0) == '<') {
      generic = generic.substring(1, generic.length()-1);
    }
    for (String importable : JavaLexer.findImportsInGeneric(generic)) {
      String imported = receiver.getImports().addImport(importable);
      if (importable.length() != imported.length()) {
        int len = -1;
        while (len != generic.length()){
          len = generic.length();
          generic = generic.replace(importable, imported);
        }
      }
    }
    generics.add(generic);
  }
  @Override
  public void visitJavadoc(String javadoc, SourceBuilder<?> receiver) {

  }
  @Override
  public void visitReturnType(TypeData returnType, SourceBuilder<?> receiver) {
    this.returnType = returnType;
    if (returnType.pkgName.length() > 0) {
      receiver.getImports().addImport(returnType.getImportName());
    }
  }
  @Override
  public void visitName(String name, SourceBuilder<?> receiver) {
    methodName = name;
  }

}
