package xapi.ui.html.impl;

import org.junit.Before;
import org.junit.Test;
import xapi.inject.X_Inject;
import xapi.test.Assert;
import xapi.ui.api.StyleService;
import xapi.ui.autoui.api.BeanValueProvider;
import xapi.ui.autoui.api.UserInterfaceFactory;
import xapi.ui.html.X_Html;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlSnippet;

import javax.inject.Named;

@Html(
  body = @El(
    className = "$name",
    html = "Hello World"
    ))
@Named(HtmlSnippetTest.NAME)
public class HtmlSnippetTest {

  public static final String NAME = "html-snippet";

  private StyleService<?> ctx;

  @SuppressWarnings("rawtypes")
  @Before
  public void setup() {
    ctx = new StyleService<StyleService>() {
      @Override
      public StyleService addCss(final String css, final int priority) {

        return this;
      }

      @Override
      public void loadGoogleFonts(String ... fonts) {
        addCss(X_Html.toGoogleFontUrl(fonts), 0);
      }

      @Override
      public void flushCss() {
      }
    };
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
