package xapi.dev.source;


public class HtmlBuffer {

  private final XmlBuffer root;
  private String doctype;
  private HeadBuffer head;
  private DomBuffer body;
  
  public static class HeadBuffer {
    
    private XmlBuffer title;
    private final DomBuffer buffer = new DomBuffer("head");
    
    public HeadBuffer addCss(String css) {
      makeTag("style")
        .setType("text/css")
        .append(css);
      return this;
    }
    
    public DomBuffer makeTag(String name) {
      return buffer.makeTag(name);
    }
    
    public HeadBuffer addScript(String src) {
      script(src);
      buffer.println();
      return this;
    }

    private DomBuffer script(String src) {
      return makeTag("script")
        .setType("text/javascript")
        .setSrc(src)
        .setNewLine(false)
        .append(" ")// force script tags to have a body;
      ;
    }

    public HeadBuffer addScript(String src, boolean async) {
      script(src).setAttribute("async", Boolean.toString(async));
      buffer.println();
      return this;
    }
    
    public HeadBuffer addStylesheet(String cssUrl) {
      makeTag("link")
        .setRel("stylesheet")
        .setType("text/css")
        .setHref(cssUrl)
        .setNewLine(false)
        .allowAbbreviation(false)
      ;
      buffer.println();
      return this;
    }
    
    public HeadBuffer addMeta(String ... tagPairs) {
      XmlBuffer meta = makeTag("meta").setNewLine(false).allowAbbreviation(false);
      buffer.println();
      assert tagPairs.length / 2 == tagPairs.length / 2.0 : "Only send even numbers of tagPairs to HeadBuffer.addMeta";
      for (int i = 0; i < tagPairs.length; i+= 2 ) {
        meta.setAttribute(tagPairs[i], tagPairs[i+1]);
      }
      return this;
    }

    public HeadBuffer setCharset(String charset) {
      addMeta("http-equiv","Content-Type", "content", "text/html", "charset", charset);
      addMeta("charset", charset);
      return this;
    }
    
    public HeadBuffer setTitle(String title) {
      if (this.title == null) {
        this.title = makeTag("title").setNewLine(false);
        buffer.println();
      } else {
        this.title.clear();
      }
      this.title.append(title);
      return this;
    }
    
    @Override
    public String toString() {
      return buffer.toString();
    }

    public HeadBuffer setLang(String lang) {
      buffer.setAttribute("lang", lang);
      return this;
    }

    public HeadBuffer addLink(String rel, String href) {
      buffer.makeTag("link")
        .setRel(rel)
        .setHref(href)
        .allowAbbreviation(true)
      ;
      return this;
    }
  }

  public HtmlBuffer() {
    root = new XmlBuffer("html");
    doctype = "<!doctype html>\n"; 
  }
  
  public final DomBuffer getBody() {
    return body == null ? (setBody(createBody())) : body;
  }
  
  public final HeadBuffer getHead() {
    return head == null ? (setHead(createHead())) : head;
  }
  
  protected DomBuffer createBody() {
    return new DomBuffer("body");
  }

  protected HeadBuffer createHead() {
    return new HeadBuffer();
  }
  
  private DomBuffer setBody(DomBuffer body) {
    this.body = body;
    root.addToEnd(body);
    return body;
  }

  private HeadBuffer setHead(HeadBuffer head) {
    this.head = head;
    root.addToBeginning(head.buffer);
    return head;
  }
  
  public String toString() {
    return doctype + root;
  }
}
