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



public class MethodBuffer extends PrintBuffer{

	private int modifier;
	protected SourceBuilder<?> context;
	private boolean once;
	private boolean useJsni = true;
	private String methodName;
	private final Set<String> annotations;
	private final Set<String> generics;
	private final Set<String> parameters;
	private final Set<String> exceptions;
  private String returnType;
  final String origIndent;

  public MethodBuffer(SourceBuilder<?> context) {
    this(context, INDENT);
  }
	public MethodBuffer(SourceBuilder<?> context, String indent) {
		super();
		this.context = context;
		origIndent = indent;
		this.indent = indent + INDENT;
		generics = new LinkedHashSet<String>();
		parameters = new LinkedHashSet<String>();
		annotations = new LinkedHashSet<String>();
		exceptions = new LinkedHashSet<String>();
	}
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(NEW_LINE+origIndent);
		if (annotations.size() > 0) {
		  for (String anno : annotations)
		    b.append('@').append(anno).append(NEW_LINE+origIndent);
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
		JavaMetadata metadata = new JavaMetadata(definition);
		int genericPos = definition.indexOf('>');
		modifier = metadata.getModifier();
		genericPos -= Modifier.toString(Modifier.methodModifiers() & modifier ).length();
		definition = metadata.getClassName().trim();
		context.getImports().addImports(metadata.getImports());


		boolean usedGenerics = false;
		// Add the return type
		int ind = definition.indexOf(' ');
		if (ind == -1) {
		  // we are a constructor
		  returnType = "";
		} else {
  		returnType = definition.substring(0, ind);
  		if (ind < genericPos) {
  		  usedGenerics = true;
  		}
  		definition = definition.substring(ind+1);
  		ind = returnType.lastIndexOf('.');
  		if (ind > 0) {
  		  context.getImports().addImport(returnType);
  		  returnType = returnType.substring(ind+1);
  		}
  		if (usedGenerics && metadata.hasGenerics()) {
  		  returnType += "<"+join(", ", metadata.getGenerics())+"> ";
  		}
		}
		
		// Add any generics extracted
		if (!usedGenerics && metadata.hasGenerics()){
		  generics.clear();
		  String[] generic = metadata.getGenerics();
		  for (String s : generic)
		    generics.add(s);
		}

		// Resolve parameters; reducing fqcn to import statements
		ind = definition.indexOf('(');
		assert ind > 0;
		methodName = definition.substring(0, ind).trim();
		definition = definition.substring(ind+1, definition.lastIndexOf(')')).trim();
		for (String param : definition.split(",")) {
		  param = param.trim();
		  // might be varargs...
		  ind = param.lastIndexOf("...");
		  if (ind == -1) {
		    ind = param.lastIndexOf('.');
		  } else {
		    ind = param.lastIndexOf('.', ind-1);
		  }
		  if (ind > 0) {
		    String importable = param.substring(0, param.indexOf(' '));
		    int end = importable.indexOf('<');
		    if (end < 0) {
		      context.getImports().addImport(importable);
		    } else {
		      context.getImports().addImport(importable.substring(0, end));
		    }
	      param = param.substring(ind+1);
		  }
		  parameters.add(param);
		}

		if (methodName.contains(" "))
		  throw new TypeDefinitionException("Found ambiguous method definition in "+definition);
		if (methodName.length() == 0)
			throw new TypeDefinitionException("Did not have a class name in class definition "+definition);
	}
	
  public void addParameters(String ... parameters) {
		for (String parameter0 : parameters){
		  for (String parameter : parameter0.split(",")){
  			parameter = parameter.trim();
  			int index = parameter.lastIndexOf('.');
  			if (index > 0){
  			  // trim the fqcn and import it
  			  int end = parameter.lastIndexOf(' ');
  			  assert end > 0;
  			  String fqcn = parameter.substring(0,end);
  				context.getImports().addImport(fqcn);
  				//keep the simple name
  				this.parameters.add(parameter.substring(index+1));
  			}else{
  			  //assume imports are handled externally
  				this.parameters.add(parameter);
  			}
  		}
		}
	}
	public void addGenerics(String ... generics) {
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
						context.getImports().addImport(part);
						generic = generic.replace(part.substring(0, index+1), "");
					}
				}
				this.generics.add(generic);
			}
		}
	}

	public MethodBuffer addAnnotation(Class<?> anno) {
	  context.getImports().addImports(anno);
	  this.annotations.add(anno.getSimpleName());
	  return this;
	}
	public MethodBuffer addAnnotation(String anno) {
	  if (anno.charAt(0) == '@')
	    anno = anno.substring(1);
	  anno = anno.trim();// never trust user input :)

	  int hasPeriod = anno.indexOf('.');
	  if (hasPeriod != -1) {
	    int openParen = anno.indexOf('(');
	    if (openParen == -1) {
	      hasPeriod = anno.lastIndexOf('.');
	      //fqcn is the whole string.
	      context.getImports().addImport(anno);
	    } else {
	      hasPeriod = anno.lastIndexOf('.', openParen);
	      context.getImports().addImport(anno.substring(0, openParen).trim());
	    }
	    // TODO only shorten imports if addImport returns true
	    anno = anno.substring(hasPeriod+1);
	  }
	  this.annotations.add(anno);
	  return this;
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


  // Sigh...  to create a fluent api w/out generics,
  // we have to override a bunch of methods...

  @Override
  public MethodBuffer append(boolean b) {
    super.append(b);
    return this;
  }

  @Override
  public MethodBuffer append(char c) {
    super.append(c);
    return this;
  }

  @Override
  public MethodBuffer append(char[] str) {
    super.append(str);
    return this;
  }

  @Override
  public MethodBuffer append(char[] str, int offset, int len) {
    super.append(str, offset, len);
    return this;
  }

  @Override
  public MethodBuffer append(CharSequence s) {
    super.append(s);
    return this;
  }

  @Override
  public MethodBuffer append(CharSequence s, int start, int end) {
    super.append(s, start, end);
    return this;
  }

  @Override
  public MethodBuffer append(double d) {
    super.append(d);
    return this;
  }

  @Override
  public MethodBuffer append(float f) {
    super.append(f);
    return this;
  }

  @Override
  public MethodBuffer append(int i) {
    super.append(i);
    return this;
  }

  @Override
  public MethodBuffer append(long lng) {
    super.append(lng);
    return this;
  }

  @Override
  public MethodBuffer append(Object obj) {
    super.append(obj);
    return this;
  }

  @Override
  public MethodBuffer append(String str) {
    super.append(str);
    return this;
  }

  @Override
  public MethodBuffer indent() {
    super.indent();
    return this;
  }

  @Override
  public MethodBuffer indentln(char[] str) {
    super.indentln(str);
    return this;
  }

  @Override
  public MethodBuffer indentln(CharSequence s) {
    super.indentln(s);
    return this;
  }

  @Override
  public MethodBuffer indentln(Object obj) {
    super.indentln(obj);
    return this;
  }

  @Override
  public MethodBuffer indentln(String str) {
    super.indentln(str);
    return this;
  }

  @Override
  public MethodBuffer outdent() {
    super.outdent();
    return this;
  }

  @Override
  public MethodBuffer println() {
    super.println();
    return this;
  }

  @Override
  public MethodBuffer println(char[] str) {
    super.println(str);
    return this;
  }

  @Override
  public MethodBuffer println(CharSequence s) {
    super.println(s);
    return this;
  }

  @Override
  public MethodBuffer println(Object obj) {
    super.println(obj);
    return this;
  }

  @Override
  public MethodBuffer println(String str) {
    super.println(str);
    return this;
  }

  public MethodBuffer makeJsni() {
    setUseJsni(true).makeFinal();
    return this;
  }
  public MethodBuffer makeNative() {
    if ((modifier & Modifier.ABSTRACT) > 0)
      modifier &= ~Modifier.ABSTRACT;// "Cannot be both native and abstract";
    modifier = modifier | Modifier.NATIVE;
    return this;
  }

  public MethodBuffer makeFinal() {
    if ((modifier & Modifier.ABSTRACT) > 0)
      modifier &= ~Modifier.ABSTRACT;// "Cannot be both final and abstract";
    modifier = modifier | Modifier.FINAL;
    return this;
  }

  public MethodBuffer makeStatic() {
    if ((modifier & Modifier.ABSTRACT) > 0)
      modifier &= ~Modifier.ABSTRACT; // "Cannot be both static and abstract";
    modifier = modifier | Modifier.STATIC;
    return this;
  }

  public MethodBuffer makeAbstract() {
    if ((modifier & Modifier.STATIC) > 0)
      modifier &= ~Modifier.STATIC; // "Cannot be both static and abstract";
    if ((modifier & Modifier.NATIVE) > 0)
      modifier &= ~Modifier.NATIVE; // "Cannot be both native and abstract";
    modifier = modifier | Modifier.ABSTRACT;
    return this;
  }

  public MethodBuffer makeConcrete() {
    modifier = modifier & ~Modifier.ABSTRACT;
    return this;
  }

  public MethodBuffer makePublic() {
    modifier = modifier & 0xFFF8 + Modifier.PUBLIC;
    return this;
  }

  public MethodBuffer makeProtected() {
    modifier = modifier & 0xFFF8 + Modifier.PROTECTED;
    return this;
  }

  public MethodBuffer makePrivate() {
    modifier = modifier & 0xFFF8 + Modifier.PRIVATE;
    return this;
  }

  public MethodBuffer makePackageProtected() {
    modifier = modifier & 0xFFF8;
    return this;
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

}
