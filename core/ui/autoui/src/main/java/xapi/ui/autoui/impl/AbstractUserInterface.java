package xapi.ui.autoui.impl;

import xapi.ui.autoui.api.UiRenderer;
import xapi.ui.autoui.api.UiRendererSelector;
import xapi.ui.autoui.api.UiRenderingContext;
import xapi.ui.autoui.api.UserInterface;
import xapi.util.X_Debug;

public abstract class AbstractUserInterface <T> implements UserInterface <T>{

  private UiRenderingContext[] renderers;
  private int[] head, body, tail;
  
  @Override
  public final UserInterface<T> setRenderers(UiRenderingContext[] renderers) {
    this.renderers = renderers;
    head = new int[renderers.length];
    body = new int[renderers.length];
    tail = new int[renderers.length];
    int h=0,b=0,t=0;
    // sort matches
    for (int i = 0, m = renderers.length; i<m; i++) {
      UiRenderingContext ctx = renderers[i];
      if (ctx.isHead()) {
        head[h++] = i;
      } else if (ctx.isTail()) {
        tail[t++] = i;
      } else {
        body[b++] = i;
      }
    }
    // trim
    head = copyOf(head, h);
    body = copyOf(body, b);
    tail = copyOf(tail, t);
    return this;
  }

  private static int[] copyOf(int[] arr, int len) {
    if (len == arr.length) {
      return arr;
    }
    int[] copy = new int[len];
    System.arraycopy(arr, 0, copy, 0, len);
    return copy;
  }

  @Override
  public final UserInterface<T> renderUi(T model) {
    for (int i : head) {
      doRender(renderers[i], model);
    }
    for (int i : body) {
      doRender(renderers[i], model);
    }
    for (int i : head) {
      doRender(renderers[i], model);
    }
    return this;
  }

  protected abstract void doRender(UiRenderingContext ctx, T model);
  
  @SuppressWarnings({"rawtypes", "unchecked"})
  protected void recursiveRender(UiRenderingContext ctx, UiRenderer renderer, T model){
    UiRendererSelector selector = ctx.getSelector();
    if (ctx.isWrapper()) {
      for (String key : ctx.getBeanValueProvider().getChildKeys()) {
        try {
          if (selector.useRenderer(this, renderer, key, model)) {
            renderer.renderInto(this, ctx, key, model);
          }
        } catch(ClassCastException e) {
          e.printStackTrace();
        } catch(Throwable e) {
          throw X_Debug.rethrow(e);
        }
      }
    } else {
      if (selector.useRenderer(this, renderer, ctx.getName(), model)) {
        renderer.renderInto(this, ctx, ctx.getName(), model);
      }
    }
  }
  
}
