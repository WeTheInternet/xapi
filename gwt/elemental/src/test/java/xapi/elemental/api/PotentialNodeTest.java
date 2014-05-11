package xapi.elemental.api;

import org.junit.Assert;
import org.junit.Test;

import elemental.dom.Element;

public class PotentialNodeTest {

  private static class TestNode extends PotentialNode<Element> {
    @Override
    protected Element build(String html) {
      return null;
    }
    @Override
    public String toString() {
      StringBuilder b = new StringBuilder();
      toHtml(b);
      return b.toString();
    }
  }

  @Test
  public void testSimpleHtml() {
    TestNode node = new TestNode();
    node.setTagName("div");
    node.append("Hello World");
    Assert.assertEquals("<div>Hello World</div>", node.toString());
  }

  @Test
  public void testSimpleAttributes() {
    TestNode node = new TestNode();
    node.setTagName("div");
    node.setAttribute("class", "awesome");
    node.append("Hello World");
    Assert.assertEquals("<div class='awesome'>Hello World</div>", node.toString());
  }

  @Test
  public void testStyle_AttributeSet() {
    TestNode node = new TestNode();
    node.setTagName("div");
    node.setAttribute("style", "totally:awesome");
    node.append("Hello World");
    Assert.assertEquals("<div style='totally:awesome;'>Hello World</div>", node.toString());
    node.setAttribute("style", "really:great");
    Assert.assertEquals("<div style='really:great;'>Hello World</div>", node.toString());
  }

  @Test
  public void testStyle_AttributeAdd() {
    TestNode node = new TestNode();
    node.setTagName("div");
    node.addAttribute("style", "totally:awesome");
    node.append("Hello World");
    Assert.assertEquals("<div style='totally:awesome;'>Hello World</div>", node.toString());
    node.addAttribute("style", "really:great");
    Assert.assertEquals("<div style='totally:awesome;really:great;'>Hello World</div>", node.toString());
  }

  @Test
  public void testStyle_DirectSet() {
    TestNode node = new TestNode();
    node.setTagName("div");
    node.setStyle("totally", "awesome");
    node.append("Hello World");
    Assert.assertEquals("<div style='totally:awesome;'>Hello World</div>", node.toString());
    node.setAttribute("style", "really:great");
    Assert.assertEquals("<div style='really:great;'>Hello World</div>", node.toString());
  }

  @Test
  public void testStyle_DirectAdd() {
    TestNode node = new TestNode();
    node.setTagName("div");
    node.setStyle("totally","awesome");
    node.append("Hello World");
    Assert.assertEquals("<div style='totally:awesome;'>Hello World</div>", node.toString());
    node.setStyle("really","great");
    Assert.assertEquals("<div style='totally:awesome;really:great;'>Hello World</div>", node.toString());
  }

  @Test
  public void testComplexBuilder() {
    TestNode node = new TestNode();
    TestNode child = new TestNode();
    node.setTagName("div");
    node.setAttribute("class", "awesome");
    node.append("Hello");
    node.addChild(child);
    child.setTagName("br");
    node.append("World");

    System.out.println(node.toString());
    Assert.assertEquals("<div class='awesome'>Hello<br/>World</div>", node.toString());
  }

}
