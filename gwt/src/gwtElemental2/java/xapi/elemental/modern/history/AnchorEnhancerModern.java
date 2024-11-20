package xapi.elemental.modern.history;

import com.google.gwt.core.client.Scheduler;
import elemental2.dom.*;
import xapi.collect.api.StringTo;
import xapi.elemental.modern.api.ElementalIterable;
import xapi.fu.In2;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.util.api.Initable;

import java.util.Iterator;

import static xapi.collect.X_Collect.newStringMap;


public class AnchorEnhancerModern implements Initable {

  private static final AnchorProcessorModern DEFAULT_PROCESSOR = (anchor, paths)->
    X_Log.warn(AnchorEnhancerModern.class,"Unhandled link; path: ",paths, anchor)
  ;

  private AnchorProcessorModern defaultHandler;

  private AnchorEnhancerModern() {
    defaultHandler = DEFAULT_PROCESSOR;
  }

  private static final Out1<AnchorEnhancerModern> enhancer =
    Lazy.deferred1(()->
      X_Inject.singleton(AnchorEnhancerModern.class).run()
    );
  protected final StringTo<AnchorProcessorModern> processors = newStringMap(AnchorProcessorModern.class);

  public AnchorEnhancerModern run() {
    enhanceAll(DomGlobal.document.getElementsByTagName("a"));
    return this;
  }

  public boolean enhance(final Element anchor) {
    String ident = anchor.getAttribute("href");
    if (ident != null && ident.startsWith("/")) {
      anchor.onclick = (e) -> {
          final String[] paths = anchor.getAttribute("href").substring(1).split("/");
          X_Log.info("Link Clicked", anchor, paths, anchor.getAttribute("href"));
          if (!allowDefaults(paths)) {
            e.preventDefault();
          }
          AnchorProcessorModern processor = processors.get(paths[0]);
          if (processor == null) {
            processor = defaultHandler;
          }
          processor.onAnchorClick(anchor, paths);
          return true;
      };
    }
    return true;
  }

  protected boolean allowDefaults(String[] paths) {
    // many moons ago we cared about passing-through /_ah urls to browser, for auth redirectrs
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

  public static AnchorEnhancerModern get() {
    return enhancer.out1();
  }

  public static void enhanceChildren(Element e) {
    enhancer.out1().enhanceAll(e.querySelectorAll("a"));
  }

  public AnchorEnhancerModern addProcessor(String path0, AnchorProcessorModern processor) {
    processors.put(path0, processor);
    return this;
  }

  public AnchorEnhancerModern setDefaultHandler(AnchorProcessorModern processor) {
    assert processor != null : "Don't send null handlers yo!";
    this.defaultHandler = processor;
    return this;
  }

  public AnchorEnhancerModern addDefaultHandler(AnchorProcessorModern processor) {
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
    enhanceAll(ElementalIterable.forEach(anchors));
  }

  public void enhanceAll(final HTMLCollection anchors) {
    enhanceAll(ElementalIterable.forEach(anchors));
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
