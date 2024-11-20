package xapi.elemental.history;

import elemental.dom.Element;

public interface AnchorProcessor {

  void onAnchorClick(Element e, String ... paths);

}
