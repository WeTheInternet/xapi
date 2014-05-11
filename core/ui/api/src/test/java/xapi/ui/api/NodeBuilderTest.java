package xapi.ui.api;

import org.junit.Test;

import xapi.test.Assert;

public class NodeBuilderTest {

  private static class TestNode extends NodeBuilder<String> {
    @Override
    public void append(Widget<String> child) {
      throw new UnsupportedOperationException();
    }
    @Override
    protected String create(CharSequence node) {
      return node.toString();
    }

    @Override
    protected NodeBuilder<String> wrapChars(CharSequence body) {
      return new TestNode().append(body);
    }
  }

  @Test
  public void testSimpleAdd() {
    TestNode node = new TestNode();
    node.append("hello").append(" world");
    Assert.assertEquals(node.getElement(), "hello world");
  }

  @Test
  public void testAddChildren() {
    TestNode root = new TestNode();
    TestNode child = new TestNode();

    root.append("hello");
    root.addChild(child);
    root.append(" world");
    child.append(" glorious");

    Assert.assertEquals("hello glorious world", root.getElement());
  }

  @Test
  public void testAddSiblings() {
    TestNode root = new TestNode();
    TestNode sibling0 = new TestNode();
    TestNode sibling1 = new TestNode();

    root.append("hello");
    root.addSibling(sibling0);
    root.append(" world");
    root.addSibling(sibling1);
    sibling0.append("!");
    sibling1.append("?");

    Assert.assertEquals("hello world!?", root.getElement());
  }
}
