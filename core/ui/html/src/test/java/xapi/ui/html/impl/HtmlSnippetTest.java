package xapi.ui.html.impl;

import org.junit.Before;
import org.junit.Test;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.test.Assert;
import xapi.ui.api.StyleService;
import xapi.ui.api.style.HasStyleResources;
import xapi.ui.autoui.api.BeanValueProvider;
import xapi.ui.autoui.api.UserInterfaceFactory;
import xapi.ui.html.X_Html;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlSnippet;

import com.google.gwt.dom.client.StyleElement;

import javax.inject.Named;

@Html(
  body = @El(
    className = "$name",
    html = "Hello World"
    ))
@Named(HtmlSnippetTest.NAME)
public class HtmlSnippetTest {

  public static final String NAME = "html-snippet";

  private StyleService<?, ?> ctx;

  private static class TestService implements StyleService<StyleElement, HasStyleResources> {
    @Override
    public void addCss(final String css, final int priority) {

    }

    @Override
    public void loadGoogleFonts(String ... fonts) {
      addCss(StyleService.toGoogleFontUrl(fonts), 0);
    }

    @Override
    public void flushCss() {
    }

    @Override
    public StyleElement injectStyle(
        Class<? extends HasStyleResources> bundle, Class<?>[] styles
    ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Out1<StyleElement> registerStyle(
        Class<? extends HasStyleResources> bundle, String css, Class<?>[] styles
    ) {
      throw new UnsupportedOperationException();
    }
  }

  @SuppressWarnings("rawtypes")
  @Before
  public void setup() {
    ctx = new TestService();
  }

  @Test
  public void testHelloWorld() {

    final HtmlSnippet<HtmlSnippetTest> snippet = createSnippet(this);

    final String result = snippet.convert(this);

    Assert.assertEquals("<div class=\"" + NAME + "\" >Hello World</div>",
      result);
  }

  @Test
  public void testHelloWorld_Static() {

    final String result = X_Html.toHtml(HtmlSnippetTest.class, this, ctx);

    Assert.assertEquals("<div class=\"" + NAME + "\" >Hello World</div>",
      result);
  }

  private HtmlSnippet<HtmlSnippetTest> createSnippet(
    final HtmlSnippetTest test) {
    return new HtmlSnippet<>(test.getClass().getAnnotation(Html.class),
      toValues(test), ctx);
  }

  private BeanValueProvider toValues(final HtmlSnippetTest test) {
    final UserInterfaceFactory factory = X_Inject
      .instance(UserInterfaceFactory.class);
    return factory.getBeanProvider(test.getClass());
  }

}
