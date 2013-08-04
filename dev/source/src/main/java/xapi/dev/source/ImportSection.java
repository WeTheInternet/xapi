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


import static xapi.dev.source.PrintBuffer.NEW_LINE;

import java.util.TreeMap;
import java.util.regex.Pattern;

public class ImportSection {

	private final TreeMap<String, String> imports = new TreeMap<String, String>();
	private final TreeMap<String, String> importStatic = new TreeMap<String, String>();

	private static final Pattern skipImports = Pattern.compile(
	  "(" +
  	  "(java[.]lang.[^.]*)" + // discard java.lang, but keep java.lang.reflect
	  "|" + //also discard primitives
  	  "((void)|(boolean)|(short)|(char)|(int)|(long)|(float)|(double)" +
  	  "|(String)|(Object)|(Void)|(Boolean)|(Short)|(Character)|(Integer)|(Long)|(Float)|(Double))" +
	  ")" +
	  "[;]*"
  );
	private static final Pattern trimmer = Pattern.compile(
	  "(\\[\\])|(\\s*import\\s+)|(static\\s+)|(\\s*;\\s*)"
	  );

	public ImportSection() {
	}

	public ImportSection addImports(String ... imports){
		for (String iport :  imports) {
		  addImport(iport);
		}
		return this;
	}

	public String addImport(String importName){
	  return tryImport(importName, importName.contains("static "));
	}

	public String addStatic(String importName){
	  return tryImport(importName, true);
	}

	public ImportSection addStatics(String ... imports){
	  for (String iport :  imports)
	    addStatic(iport);
	  return this;
	}

	@Override
	public String toString() {
	  StringBuilder b = new StringBuilder(NEW_LINE);
		for (String importName : imports.values()){
		  if (importName.length() > 0)
  		  b.append("import ")
  		   .append(importName)
  		   .append(';')
  			 .append(NEW_LINE);
		}
		for (String importName : importStatic.values()){
		  if (importName.length() > 0)
		    b.append("import static ")
		    .append(importName)
		    .append(';')
		    .append(NEW_LINE);
		}
		return b.toString()+NEW_LINE;
	}

	public ImportSection reserveSimpleName(String cls) {
	  if (!imports.containsKey(cls))
	    imports.put(cls, "");
	  return this;
	}

	public ImportSection reserveMethodName(String name) {
	  if (!imports.containsKey(name))
	    imports.put(name, "");
	  return this;
	}

	public String addImport(Class<?> cls) {
	  if (cls.isPrimitive() || "java.lang".equals(cls.getPackage().getName()))
	    return cls.getSimpleName();
	  String existing = imports.get(cls.getSimpleName());
	  if (existing != null) {
	    if (existing.equals(cls.getCanonicalName()))
	      return cls.getSimpleName();
	    return cls.getCanonicalName();
	  }
	  imports.put(cls.getSimpleName(), cls.getCanonicalName());
	  return cls.getSimpleName();
	}

  public ImportSection addImports(Class<?> ... imports) {
    for (Class<?> cls : imports) {
      addImport(cls);
    }
    return this;
  }

  protected boolean canMinimize(String importName) {
    String simpleName = importName.substring(importName.lastIndexOf('.')+1);
    String existing = imports.get(simpleName);

    return existing == null || // No type for this simple name
      "".equals(existing) ||  // This simple name was reserved
      existing.equals(importName); // This type is already imported
  }

  protected String tryImport(String importName, boolean staticImport) {
    TreeMap<String, String> map = staticImport ? importStatic : imports;
    int arrayDepth = 0;
    int index = importName.indexOf(".");
    if (index == -1)
      return importName;
    index = importName.indexOf("[]");
    while (index != -1) {
      importName = importName.substring(0, index) +
          (index < importName.length() - 2 ? importName.substring(index + 2) : "");
      index = importName.indexOf("[]", index);
      arrayDepth++;
    }
    importName = trimmer.matcher(importName.trim()).replaceAll("");
    index = importName.indexOf('<');
    String suffix;
    if (index == -1)
      suffix = "";
    else {
      suffix = importName.substring(index);
      // TODO import and strip these generics too.
      importName = importName.substring(0, index);
    }
    while (arrayDepth --> 0)
      suffix += "[]";
    if (skipImports.matcher(importName).matches())
      return importName.replace("java.lang.", "")+suffix;
    importName = importName.replace('$', '.');
    String shortname = importName.substring(1+importName.lastIndexOf('.'));
    if ("*".equals(shortname)) {
      map.put(importName, importName);
      return importName+suffix;
    }

    String existing = map.get(shortname);
    if (existing == null) {
      map.put(shortname, importName);
      return shortname+suffix;
    }
    if (existing.equals(importName))
      return shortname+suffix;
//    map.put(importName+" "+map.size(), importName);
    return importName+suffix;
  }
}
