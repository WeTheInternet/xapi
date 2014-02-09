package xapi.dev.gwtc.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import xapi.annotation.ui.UiTemplate;
import xapi.annotation.ui.UiTemplate.EmbedStrategy;
import xapi.annotation.ui.UiTemplate.Location;
import xapi.collect.api.Fifo;
import xapi.collect.impl.LazyList;
import xapi.dev.gwtc.impl.GwtcContext.GwtcUnit;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.XmlBuffer;
import xapi.file.X_File;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.util.X_Util;

public class UiTemplateGenerator {

  protected static interface WrapElements{
    GwtcUnitType type();
    boolean wrapsAll();
  }
  protected static class WrapsElements implements WrapElements {
    
    private final GwtcUnitType type;
    
    public WrapsElements(GwtcUnitType type) {
      this.type = type;
    }
    
    @Override
    public GwtcUnitType type() {
      return type;
    }
    
    @Override
    public boolean wrapsAll() {
      return false;
    }
  }
  protected static class WrapsAllElements extends WrapsElements {

    public WrapsAllElements(GwtcUnitType type) {
      super(type);
    }

    @Override
    public boolean wrapsAll() {
      return true;
    }
  }
  protected static class WrapsAllPackages extends WrapsAllElements {
    public WrapsAllPackages() {
      super(GwtcUnitType.Package);
    }
  }
  protected static class WrapsAllClasses extends WrapsAllElements {
    public WrapsAllClasses() {
      super(GwtcUnitType.Class);
    }
  }
  protected static class WrapsAllMethods extends WrapsAllElements {
    public WrapsAllMethods() {
      super(GwtcUnitType.Class);
    }
  }
  protected static class WrapsPackages extends WrapsElements {
    public WrapsPackages() {
      super(GwtcUnitType.Package);
    }
  }
  protected static class WrapsClasses extends WrapsElements {
    public WrapsClasses() {
      super(GwtcUnitType.Class);
    }
  }
  protected static class WrapsMethods extends WrapsElements {
    public WrapsMethods() {
      super(GwtcUnitType.Class);
    }
  }
  
  protected class UiContext {
    
    private final EmbedStrategy strategy;
    WrapsAllElements all;
    WrapsElements each;
    

    public UiContext(EmbedStrategy strategy) {
      this.strategy = strategy;
    }
    
    private StringBuilder before = new StringBuilder();
    private StringBuilder after = new StringBuilder();
    private List<UiContext> children = new ArrayList<UiContext>();
    
    protected void addBefore(String txt) {
      before.append(txt);
    }
    
    protected UiContext wrap(UiTemplateGenerator generator) {
      UiContext ctx = generator.new UiContext(strategy);
      if (matchesStrategy(generator)) {
        // When our UiContext matches for a supplied child,
        // we should return a new UiContext which wraps this child.
        
        ctx.addBefore(before.toString());
        ctx.addAfter(after.toString());
        children.add(ctx);
      }
      return ctx;
    }

    private boolean matchesStrategy(UiTemplateGenerator generator) {
      switch (strategy) {
        case Insert:
          return false;
        case WrapAllClasses:
          return generator.type == GwtcUnitType.Class && 
            (generator.parent == null || generator.parent.type == GwtcUnitType.Package);
        case WrapAllPackages:
          if (parent != null) {
            return false;
          }
        case WrapEachPackage:
          return generator.type == GwtcUnitType.Package;
        case WrapEachClass:
          return generator.type == GwtcUnitType.Class;
        case WrapEachMethod:
          return generator.type == GwtcUnitType.Method;
      }
      return false;
    }

    protected void addAfter(String txt) {
      after.append(txt);
    }
    
    @Override
    public String toString() {
      if (children.isEmpty()) {
        return before + after.toString();
      } else {
        StringBuilder b = new StringBuilder();
        for (UiContext child : children) {
          b.append(child.toString());
        }
        return b.toString();
      }
    }
    
  }
  
  protected UiTemplateGenerator parent;

  @SuppressWarnings("unchecked")
  private final Fifo<UiTemplateGenerator> children = X_Inject.instance(Fifo.class);

  // Rather than null-check all six insertion points, we just use lazy providers
  // which will create collections only on demand.
  LazyList<UiTemplate> 
    htmlBodyBefore = newList(),
    htmlBody = newList(),
    htmlBodyAfter = newList(),
    htmlHeadBefore = newList(),
    htmlHead = newList(),
    htmlHeadAfter = newList();

  protected final GwtcUnitType type;

  private WrapsAllPackages allPackages = new WrapsAllPackages();
  private WrapsAllClasses allClasses = new WrapsAllClasses();
  private WrapsAllMethods allMethods = new WrapsAllMethods();
  private WrapsPackages packages = new WrapsPackages();
  private WrapsClasses classes = new WrapsClasses();
  private WrapsMethods methods = new WrapsMethods();
  
  public UiTemplateGenerator(GwtcUnit root) {
    this.type = root.getType();
    for (UiTemplate template : root.gwtc.includeHostHtml()) {
      switch (template.location()) {
        case Body_Prefix:
        case Body_Insert:
        case Body_Suffix:
        case Head_Prefix:
        case Head_Insert:
        case Head_Suffix:
      }
      addTemplate(template);
    }
    for (GwtcUnit child : root.getChildren()) {
      addChild(new UiTemplateGenerator(child));
    }
  }




  private final LazyList<UiTemplate> newList() {
    return new LazyList<UiTemplate>();
  }
  
  


  public UiTemplateGenerator addTemplate(UiTemplate template) {
    final ArrayList<UiTemplate> list;
    switch (template.location()) {
      case Body_Insert:
        list = htmlBody.get();
        break;
      case Body_Prefix:
        list = htmlBodyBefore.get();
        break;
      case Body_Suffix:
        list = htmlBodyAfter.get();
        break;
      case Head_Insert:
        list = htmlHeadBefore.get();
        break;
      case Head_Prefix:
        list = htmlHead.get();
        break;
      case Head_Suffix:
        list = htmlHeadAfter.get();
        break;
      default:
        throw new IllegalStateException("Unhanded location type "+template.location());//impossible
    }
    list.add(template);
    return this;
  }

  protected void addChild(UiTemplateGenerator child) {
    children.give(child);
  }

  public void generate(XmlBuffer head, XmlBuffer body) {
    final PrintBuffer headBefore = new PrintBuffer();
    final PrintBuffer headInsert = new PrintBuffer();
    final PrintBuffer headAfter = new PrintBuffer();
    final PrintBuffer bodyBefore = new PrintBuffer();
    final PrintBuffer bodyInsert = new PrintBuffer();
    final PrintBuffer bodyAfter = new PrintBuffer();

    
    doGenerate(new HashSet<UiTemplateGenerator>(), headBefore, headInsert, headAfter,
        bodyBefore, bodyInsert, bodyAfter);
    

    if (!headBefore.isEmpty()) {
      head.addToBeginning(headBefore);
    }
    if (!headInsert.isEmpty()) {
      head.addToEnd(headInsert);
    }
    if (!headAfter.isEmpty()) {
      head.addToEnd(headAfter);
    }
    if (!bodyBefore.isEmpty()) {
      body.addToBeginning(headBefore);
    }
    if (!bodyInsert.isEmpty()) {
      body.addToEnd(bodyInsert);
    }
    if (!bodyAfter.isEmpty()) {
      body.addToEnd(headAfter);
    }
    
  }
  public void doGenerate(HashSet<UiTemplateGenerator> finished,
      PrintBuffer headBefore, PrintBuffer head, PrintBuffer headAfter, 
      PrintBuffer bodyBefore, PrintBuffer body, PrintBuffer bodyAfter) {
    
    if (htmlHeadBefore.isSet()) {
      for (UiTemplate headResource : htmlHeadBefore.get()) {
        printResource(headBefore, headResource);
      }
    }
    if (htmlHead.isSet()) {
      for (UiTemplate headResource : htmlHead.get()) {
        printResource(head, headResource);
      }
    }
    if (htmlBodyBefore.isSet()) {
      for (UiTemplate headResource : htmlBodyBefore.get()) {
        printResource(bodyBefore, headResource);
      }
    }
    if (htmlBody.isSet()) {
      for (UiTemplate headResource : htmlBody.get()) {
        printResource(body, headResource);
      }
    }
    
    for (UiTemplateGenerator child : children.forEach()) {
      if (finished.add(child)) {
        child.doGenerate(finished, headBefore, head, headAfter, bodyBefore, body, bodyAfter);
      }
    }
    
    if (htmlHeadAfter.isSet()) {
      for (UiTemplate headResource : htmlHeadAfter.get()) {
        printResource(headAfter, headResource);
      }
    }
    if (htmlBodyAfter.isSet()) {
      for (UiTemplate headResource : htmlBodyAfter.get()) {
        printResource(bodyAfter, headResource);
      }
    }
  }
  
  protected String getResource(UiTemplate body) {
    String value = body.value();
    final String contents;
    try {
      InputStream resource;
      switch (body.type()) {
        case Literal:
          contents = body.value();
          break;
        case File:
          resource = X_File.unzipFile(value);
          contents = X_IO.toStringUtf8(resource);
          break;
        case Resource:
          resource = X_File.unzipResource(value, Thread.currentThread().getContextClassLoader());
          contents = X_IO.toStringUtf8(resource);
          break;
        default:
          throw new UnsupportedOperationException(getClass()+" does not support "+body.type());
      }
      return contents;
    } catch (Exception e) { 
      X_Log.warn(getClass(), e, "failure while loading resource",body);
      if (body.required()) {
        throw X_Util.rethrow(e);
      }
      return value;
    }
    
  }
  @SuppressWarnings("incomplete-switch")
  public void printResource(PrintBuffer el, UiTemplate body) {// TODO
    String contents = resolveTemplate(getResource(body), body);
    switch(body.embedStrategy()) {
      case Insert:
        break;
    }
//    UiContext ctx = new UiContext(body.embedStrategy());
//    Template t = new Template(contents, body.keys());
//    Object[] values = getValues(ctx, body.location(), body.keys());
//    X_Log.error(getClass(), "Test render\n\n",t.apply(values));
    el.println(contents);
  }
  

  private Object[] getValues(UiContext ctx, Location location, String[] keys) {
    int i = keys.length;
    Object[] values = new Object[i];
    while (i-->0) {
      values[i] = keys[i];
      switch (keys[i]) {
        case UiTemplate.$child:
          // The value to use is the resolved children
        case UiTemplate.$class:
        case UiTemplate.$id:
        case UiTemplate.$method:
        case UiTemplate.$package:
      }
    }
    return values;
  }
  /**
   * A hook used to allow subclasses to perform custom manipulations on a given template.
   * 
   * @param contents - The resolved plaintext UTF-8 encoded text content of the supplied {@link UiTemplate}
   * @param body  - The {@link UiTemplate} from which the contents were resolved
   * @return - A manipulated form of the contents
   */
  protected String resolveTemplate(String contents, UiTemplate body) {
    return contents;
  }

}
