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


public class JavaMetadata {

	private final int modifier;

	private final boolean isClass;
	private final boolean isGenerics;
	private final Set<String> interfaces;
	private final Set<String> generics;
	private final Set<String> imports;
	private final String superClass;
	private final String className;

	public JavaMetadata(String definition){
		interfaces = new TreeSet<String>();
		generics = new TreeSet<String>();
		imports = new TreeSet<String>();
		String original = definition;
		int modifier = 0;
		if (definition.contains("public ")){
			definition = definition.replace("public ", "");
			modifier = Modifier.PUBLIC;
		}else if (definition.contains("protected ")){
			definition = definition.replace("protected ", "");
			modifier = Modifier.PROTECTED;
		}else if (definition.contains("private ")){
			definition = definition.replace("private ", "");
			modifier = Modifier.PRIVATE;
		}else{
			modifier = 0;
		}
		//eat opening brackets; we will supply our own
		definition = definition.replace("{", "");

		if (definition.contains("static ")){
			modifier |= Modifier.STATIC;
			definition = definition.replace("static ","");
		}

		if (definition.contains("final ")){
		  modifier |= Modifier.FINAL;
			definition = definition.replace("final ","");
		}

		if (definition.contains("native ")){
		  modifier |= Modifier.NATIVE;
			definition = definition.replace("native ","");
		}

		if (definition.contains("synchronized ")){
		  modifier |= Modifier.SYNCHRONIZED;
		  definition = definition.replace("synchronized ","");
		}
		// not bothering with strictfp, transient or volatile just yet
		if (definition.contains("abstract ")){
		  modifier |= Modifier.ABSTRACT;
			if (Modifier.isFinal(modifier))
				throw new TypeDefinitionException("A class or method cannot be both abstract and final!");
			if (Modifier.isNative(modifier))
				throw new TypeDefinitionException("A method cannot be both abstract and native!");
			//can't check static until we know whether we're parsing a class or a method
			definition = definition.replace("abstract ","");
		}

		this.modifier = modifier;

		int index;

    if (definition.contains("interface ")){
      definition = definition.replace("interface ", "");
      isClass = false;
      superClass = null;
      //extends applies to superinterfaces
      index = definition.indexOf("extends ");
      if (index > 0){
        for (String iface : definition.substring(index+8).split(",")){
          iface = iface.trim();
          index = iface.lastIndexOf('.');
          if (index > 0){
            imports.add(iface);
            iface = iface.substring(index+1);
          }
          interfaces.add(iface);
        }
        definition = definition.substring(0, index);
      }
    }else{
      isClass = definition.contains("class ");
      if (isClass) {

        definition = definition.replace("class ", "");
        //extends applies to superclass
        index = definition.indexOf("extends ");
        if (index > 0){
          definition = definition.replace("extends ", "");
          int endIndex = definition.indexOf(' ', index);
          if (endIndex == -1) {
            superClass = definition.substring(index);
            definition = definition.replace(superClass, "");
          } else {
            superClass = definition.substring(index, endIndex);
            definition = definition.replace(superClass+" ", "");
          }
        }else{
          superClass = null;
        }
        index = definition.indexOf("implements ");
        if (index > 0){
          for (String iface : definition.substring(index+11).split(",")){
            iface = iface.trim();
            int period = iface.lastIndexOf('.');
            if (period > 0){
              // we have to pull generics off this iface as well
              int generic = iface.indexOf('<');
              if (generic == -1) {
                imports.add(iface);
              } else {
                imports.add(iface.substring(0, generic));
              }
              iface = iface.substring(period+1);
            }
            interfaces.add(iface);
          }
          definition = definition.substring(0, index);
        }
      }else {
        superClass = null;
      }
    }

		index = definition.indexOf('<');
		if (index > -1){
		  int methodLim = definition.indexOf('(');
		  if (methodLim < 0 || methodLim > index) {
  			isGenerics = true;
  			int end = findEnd(definition, index);
  			String generic = definition.substring(index+1, end);
  			for (String gen : generic.split(",")){
  				gen = gen.trim();
  				boolean noImport = gen.contains("!");
  				if (noImport){
  					gen = gen.replaceAll("[!]", "");
  				}else{
  					for (String part : gen.split(" ")){
  						int period = part.lastIndexOf('.');
  						if (period < 0)
  							continue;
  						imports.add(part);
  						gen = gen.replace(part.substring(0, period+1), "");
  					}
  				}
  				generics.add(gen);
  			}
  			String prefix = definition.substring(0, index);
  			if (end < definition.length()-1){
  				definition = prefix + definition.substring(end+1);
  			}else
  				definition = prefix;
		  } else {
		    isGenerics = false;
		  }
		}else{
			isGenerics = false;
		}

		definition = definition.trim();
		// some runtime validation
		if (definition.contains(" ") && isClass)
		  throw new TypeDefinitionException("Found ambiguous class definition in "+original+"; leftover: "+definition);
		if (definition.length() == 0)
			throw new TypeDefinitionException("Did not have a class name in class definition "+original);
    if (Modifier.isStatic(modifier) && Modifier.isAbstract(modifier) && !isClass)
      throw new TypeDefinitionException("A method cannot be both abstract and static!");
		className = definition;
	}

	private int findEnd(String definition, int index) {
	  int opened = 1;
	  while (index < definition.length()) {
	    switch (definition.charAt(++index)) {
	    case '<':
	      opened ++;
	      break;
	    case '>':
	      if (--opened==0)
	        return index;
	    }
	  }
    return -1;
  }

  public String getClassName() {
		return className;
	}

	public int getPrivacy(){
		return modifier&7;//bitmask, so value can do == matching
	}

	public int getModifier(){
	  return modifier;
	}

	public String getSuperClass() {
		return superClass;
	}

	public String[] getGenerics(){
		return generics.toArray(new String[generics.size()]);
	}

	public String[] getImports(){
		return imports.toArray(new String[imports.size()]);
	}

	public String[] getInterfaces(){
		return interfaces.toArray(new String[interfaces.size()]);
	}

	public boolean isPublic() {
		return Modifier.isPublic(modifier);
	}

	public boolean isPrivate() {
	  return Modifier.isPrivate(modifier);
	}

	public boolean isProtected() {
	  return Modifier.isProtected(modifier);
	}

	public boolean isStatic() {
	  return Modifier.isStatic(modifier);
	}

	public boolean isFinal() {
	  return Modifier.isFinal(modifier);
	}

	public boolean isAbstract() {
	  return Modifier.isAbstract(modifier);
	}

	public boolean isNative() {
	  return Modifier.isNative(modifier);
	}

	public boolean isClass() {
		return isClass;
	}

	public boolean hasGenerics() {
		return isGenerics;
	}

}
