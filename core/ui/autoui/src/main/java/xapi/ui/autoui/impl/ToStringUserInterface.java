package xapi.ui.autoui.impl;

import java.io.IOException;

import xapi.ui.autoui.api.UiRenderingContext;

@SuppressWarnings("rawtypes")
public class ToStringUserInterface extends AbstractUserInterface implements Appendable{

  final StringBuilder b = new StringBuilder();
  
  @Override
  @SuppressWarnings("unchecked")
  protected void doRender(UiRenderingContext ctx, Object model) {
    recursiveRender(ctx, ctx.getRenderer(), model);
  }

  @Override
  public int hashCode() {
    return b.toString().hashCode();
  }
  
  @Override
  public String toString() {
    return b.toString();
  }
  
  @Override
  public boolean equals(Object obj) {
    return toString().equals(String.valueOf(obj));
  }

  @Override
  public Appendable append(CharSequence csq) throws IOException {
    b.append(csq);
    return this;
  }

  @Override
  public Appendable append(CharSequence csq, int start, int end)
      throws IOException {
    b.append(csq, start, end);
    return this;
  }

  @Override
  public Appendable append(char c) throws IOException {
    b.append(c);
    return this;
  }

}
