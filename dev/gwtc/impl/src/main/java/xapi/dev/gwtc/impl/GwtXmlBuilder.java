package xapi.dev.gwtc.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import xapi.annotation.compile.Resource;
import xapi.dev.source.XmlBuffer;
import xapi.gwtc.api.Gwtc;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.util.X_Runtime;

public class GwtXmlBuilder {

  private static final Charset UTF8 = Charset.forName("utf-8");
  static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit 2.5.1//EN\" \"http://google-web-toolkit.googlecode.com/svn/tags/2.5.1/distro-source/core/src/gwt-module.dtd\">\n";
  private final String inheritName;
  private final boolean debug;

  public GwtXmlBuilder(Gwtc gwtc, String pkg, String name, File workDir) {
    debug = gwtc.debug() || X_Runtime.isDebug();
    if (!pkg.endsWith(".")) {
      pkg = pkg + ".";
    }
    inheritName = pkg+name.replaceAll(".gwt.*xml", "");
    String fileName = pkg.replace('.', '/')+name;
    if (name.indexOf('.') == -1) {
      fileName += ".gwt.xml";
    }
    File outputFile = new File(workDir, fileName);
    
    XmlBuffer out = new XmlBuffer("module");
    generate(gwtc, out);
    save(out, outputFile);
  }

  protected void generate(Gwtc gwtc, XmlBuffer out) {
    for (Resource gwtXml : gwtc.includeGwtXml()) {
      addResource(gwtXml, out);
    }
    for (String source : gwtc.includeSource()) {
      addSource(source, out);
    }
  }

  protected void addSource(String source, XmlBuffer out) {
    out.makeTagAtBeginning("source").setAttribute("path", source);
  }

  protected void addResource(Resource gwtXml, XmlBuffer out) {
    switch (gwtXml.type()) {
      case CLASSPATH_RESOURCE:
        out.makeTag("inherits")
           .setAttribute("name", gwtXml.value());
        break;
      case ABSOLUTE_FILE:
      case ARTIFACT_ID:
      case CLASS_NAME:
      case PACKAGE_NAME:
        throw new UnsupportedOperationException("Resource type "+gwtXml.type()+" not supported by "+getClass());
      case LITERAL_VALUE:
        out.println(gwtXml.value());
    }
  }

  protected void save(XmlBuffer out, File outputFile) {
    if (debug) {
      X_Log.info(getClass(), "Saving generated gwt.xml file",outputFile,"\n"+out);
    } else {
      X_Log.debug(getClass(), "Saving generated gwt.xml file",outputFile,"\n"+out);
    }
    try {
      if (outputFile.exists()) {
        outputFile.delete();
      }
      outputFile.getParentFile().mkdirs();
      outputFile.createNewFile();
    } catch (IOException e) {
      X_Log.warn("Unable to create generated gwt.xml file", outputFile);
    }
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      String value = HEADER + out;
      X_IO.drain(fos, new ByteArrayInputStream(value.getBytes(UTF8)));
    } catch (IOException e) {
      X_Log.warn("Unable to save generated gwt.xml file to ",outputFile,e,"\n"+out);
    }
  }

  public String getInheritName() {
    return inheritName;
  }
  
}
