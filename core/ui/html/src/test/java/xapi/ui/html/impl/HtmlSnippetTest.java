package xapi.ui.html.impl;

import javax.inject.Named;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import xapi.dev.source.HtmlBuffer;
import xapi.inject.X_Inject;
import xapi.ui.autoui.X_AutoUi;
import xapi.ui.autoui.api.BeanValueProvider;
import xapi.ui.autoui.api.UserInterfaceFactory;
import xapi.ui.html.X_Html;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlSnippet;

@Html(
  elements=@El(
    html="Hello World"
  )
)
@Named(HtmlSnippetTest.NAME)
public class HtmlSnippetTest {

  public static final String NAME = "html-snippet";
  
  private HtmlBuffer ctx;

  @Before public void setup () {
    ctx = new HtmlBuffer();
  }
  
  @Test
  public void testHelloWorld() {
    
    HtmlSnippet<HtmlSnippetTest> snippet = createSnippet(this);
    
    String result = snippet.convert(this);
    
    Assert.assertEquals("<div class=\"" + NAME + "\" >Hello World</div>", result);
  }
  
  @Test
  public void testHelloWorld_Static() {
    
    String result = X_Html.toHtml(HtmlSnippet.class, this, new HtmlBuffer());
    
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
