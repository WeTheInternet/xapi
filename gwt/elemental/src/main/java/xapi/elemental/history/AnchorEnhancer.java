package xapi.elemental.history;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.NodeList;
import elemental.html.HTMLCollection;
import xapi.collect.api.StringTo;
import xapi.elemental.api.ElementIterable;
import xapi.fu.In2;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.util.api.Initable;

import java.util.Iterator;

import static xapi.collect.X_Collect.newStringMap;

import com.google.gwt.core.client.Scheduler;


public class AnchorEnhancer implements Initable {

  private static final AnchorProcessor DEFAULT_PROCESSOR = (anchor, paths)->
    X_Log.warn(AnchorEnhancer.class,"Unhandled link; path: ",paths, anchor)
  ;

  private AnchorProcessor defaultHandler;

  private AnchorEnhancer() {
    defaultHandler = DEFAULT_PROCESSOR;
  }

  private static final Out1<AnchorEnhancer> enhancer =
    Lazy.deferred1(()->
      X_Inject.singleton(AnchorEnhancer.class).run()
    );
  protected final StringTo<AnchorProcessor> processors = newStringMap(AnchorProcessor.class);

  public AnchorEnhancer run() {
    enhanceAll(Browser.getDocument().getElementsByTagName("a"));
    return this;
  }

  public boolean enhance(final Element anchor) {
    String ident = anchor.getAttribute("href");
    if (ident != null && ident.startsWith("/")) {
      anchor.setOnclick((e) -> {
          final String[] paths = anchor.getAttribute("href").substring(1).split("/");
          X_Log.info("Link Clicked", anchor, paths, anchor.getAttribute("href"));
          if (!allowDefaults(paths)) {
            e.preventDefault();
          }
          AnchorProcessor processor = processors.get(paths[0]);
          if (processor == null) {
            processor = defaultHandler;
          }
          processor.onAnchorClick(anchor, paths);
      });
    }
    return true;
  }

  protected boolean allowDefaults(String[] paths) {
    return "_ah".equals(paths[0]);
  }

  public static String getTitle(Element anchor) {
    return anchor.hasAttribute("title") ? anchor.getAttribute("title") :
      anchor.hasAttribute("name") ? anchor.getAttribute("name") :
      anchor.getAttribute("href");
  }

  public static void initialize() {
    enhancer.out1();
  }

  public static AnchorEnhancer get() {
    return enhancer.out1();
  }

  public static void enhanceChildren(Element e) {
    enhancer.out1().enhanceAll(e.querySelectorAll("a"));
  }

  public AnchorEnhancer addProcessor(String path0, AnchorProcessor processor) {
    processors.put(path0, processor);
    return this;
  }

  public AnchorEnhancer setDefaultHandler(AnchorProcessor processor) {
    assert processor != null : "Don't send null handlers yo!";
    this.defaultHandler = processor;
    return this;
  }

  public AnchorEnhancer addDefaultHandler(AnchorProcessor processor) {
    assert processor != null : "Don't send null handlers yo!";
    if (defaultHandler == DEFAULT_PROCESSOR) {
      this.defaultHandler = processor;
    } else {
      defaultHandler = In2.<Element, String[]>in2(defaultHandler::onAnchorClick)
        .doAfterMe(processor::onAnchorClick)::in;
    }
    return this;
  }

  public void enhanceAll(final NodeList anchors) {
    enhanceAll(ElementIterable.forEach(anchors));
  }

  public void enhanceAll(final HTMLCollection anchors) {
    enhanceAll(ElementIterable.forEach(anchors));
  }

  private void enhanceAll(Iterable<Element> anchors) {
    final Iterator<Element> itr = anchors.iterator();
    Scheduler.get().scheduleIncremental(() ->
      itr.hasNext() && enhance(itr.next())
    );
  }

  @Override
  public void init() {
    // TODO Auto-generated method stub

  }

}
