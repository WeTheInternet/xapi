package xapi.elemental.music;

import xapi.inject.impl.SingletonProvider;
import elemental.dom.Element;

public class MusicPlayer {

  SingletonProvider<Element> root = new SingletonProvider<Element>(){
    protected Element initialValue() {
      return initRoot();
    };
  };
  
  public void attachTo(Element el) {
    
  }

  protected Element initRoot() {
    return null;
  }
  
}
