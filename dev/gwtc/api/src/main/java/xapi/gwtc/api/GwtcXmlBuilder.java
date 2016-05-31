package xapi.gwtc.api;

import xapi.annotation.compile.Resource;
import xapi.dev.source.XmlBuffer;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.util.X_Runtime;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

public class GwtcXmlBuilder {

  private static final Charset UTF8 = Charset.forName("utf-8");
  public static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit 2.7.0/EN\" \"http://gwtproject.org/doctype/2.7.0/gwt-module.dtd\">\n";

  private final String inheritName;
  private final boolean debug;
  private final String fileName;
  private final XmlBuffer out;
  private final Set<String> sources;
  private final Set<String> inherits;
  private XmlBuffer entryPoint;

  public GwtcXmlBuilder(String pkg, String name) {
    this(pkg, name, false);
  }

  public GwtcXmlBuilder(String pkg, String name, boolean debug) {
    out = new XmlBuffer("module").setTrimWhitespace(false);
    this.debug = debug || X_Runtime.isDebug();
    if (pkg.length() > 0 && !pkg.endsWith(".")) {
      pkg = pkg + ".";
    }
    inheritName = pkg+name.replaceAll(".gwt.*xml", "");
    String fileName = pkg.replace('.', '/')+name;
    if (name.indexOf('.') == -1) {
      this.fileName = fileName + ".gwt.xml";
    } else {
      this.fileName = fileName;
    }
    sources = new HashSet<>();
    inherits = new HashSet<>();
  }

  public static GwtcXmlBuilder generateGwtXml(Gwtc gwtc, String pkg, String name) {
    GwtcXmlBuilder builder = new GwtcXmlBuilder(pkg, name, gwtc.debug());
    if (!pkg.endsWith(".")) {
      pkg = pkg + ".";
    }
    X_Log.info(GwtcXmlBuilder.class, "Generating gwt xml for ",pkg+name);
    builder.generate(gwtc);
    return builder;
  }

  protected void generate(Gwtc gwtc) {
    for (Resource gwtXml : gwtc.includeGwtXml()) {
      addResource(gwtXml);
    }
    for (String source : gwtc.includeSource()) {
      addSource(source);
    }
  }

  public String getInheritName() {
    return inheritName;
  }

  public String getFileName() {
    return fileName;
  }

  public void addSource(String source) {
    if (sources.add(source)) {
      out.makeTagAtBeginning("source").setAttribute("path", source);
    }
  }

  protected void addResource(Resource gwtXml) {
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

  public void save(File outputFile) {
    outputFile = new File(outputFile, fileName);
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
      X_Log.warn(getClass(),"Unable to create generated gwt.xml file", outputFile, e);
    }
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      String value = HEADER + out;
      X_IO.drain(fos, new ByteArrayInputStream(value.getBytes(UTF8)));
    } catch (IOException e) {
      X_Log.warn(getClass(), "Unable to save generated gwt.xml file to ",outputFile,e,"\n"+out);
    }
  }

  public void inherit(String inherit) {
    out.makeTagAtBeginning("inherits")
    .setAttribute("name", inherit);
  }

  public void setEntryPoint(String qualifiedName) {
    if (entryPoint == null) {
      entryPoint = out.makeTag("entry-point")
          .setAttribute("class", qualifiedName);
    }
    entryPoint.setAttribute("class", qualifiedName);
  }

  public void addInherit(String value) {
    if (inherits.add(value)) {
      out.makeTag("inherits")
      .setAttribute("name", value);
    }
  }

  public XmlBuffer getBuffer() {
    return out;
  }

  public void setRenameTo(String renameTo) {
    out.setAttribute("rename-to", renameTo);
  }

  public void addConfigurationProperty(String name, String value) {
    out.makeTag("set-configuration-property")
      .setAttribute("name", name)
      .setAttribute("value", value);
  }

  public void setPublic(String path) {
    out.makeTag("public")
      .setAttribute("path", path);
  }

}
