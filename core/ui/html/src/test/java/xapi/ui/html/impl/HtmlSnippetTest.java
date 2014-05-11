package xapi.ui.html.impl;

import javax.inject.Named;

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

@Html(
  body=@El(
    className="$name",
    html="Hello World"
  )
)
@Named(HtmlSnippetTest.NAME)
public class HtmlSnippetTest {

  public static final String NAME = "html-snippet";

  private StyleService<?> ctx;

  @SuppressWarnings("rawtypes")
  @Before public void setup () {
    ctx = new StyleService<StyleService>() {
    @Override
    public StyleService addCss(String css, int priority) {

      return this;
    }
    };
  }

  @Test
  public void testHelloWorld() {

    HtmlSnippet<HtmlSnippetTest> snippet = createSnippet(this);

    String result = snippet.convert(this);

    Assert.assertEquals("<div class=\"" + NAME + "\" >Hello World</div>", result);
  }

  @Test
  public void testHelloWorld_Static() {

    String result = X_Html.toHtml(HtmlSnippetTest.class, this, ctx);

    Assert.assertEquals("<div class=\"" + NAME + "\" >Hello World</div>", result);
  }

  private HtmlSnippet<HtmlSnippetTest> createSnippet(
      HtmlSnippetTest test) {
    return new HtmlSnippet<>(test.getClass().getAnnotation(Html.class), toValues(test), ctx );
  }

  private BeanValueProvider toValues(HtmlSnippetTest test) {
    UserInterfaceFactory factory = X_Inject.singleton(UserInterfaceFactory.class);
    return factory.getBeanProvider(test.getClass());
  }

}
