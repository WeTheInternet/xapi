package xapi.test.components.xapi.test.components.bdd;


import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.ui.api.ElementBuilder;
import xapi.ui.api.ElementInjector;
import xapi.ui.api.component.AbstractComponent;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;

public abstract class BaseToDoAppComponent <El, ElBuilder extends ElementBuilder<El>> extends AbstractComponent< El, ToDoAppComponent<El>> implements ToDoAppComponent<El> {

  public Lazy<StringTo<Object>> refData = 
      Lazy.deferred1(()->{
        StringTo<Object> data = X_Collect.newStringMap(Object.class);
        data.put("done", false);
        data.put("text", "");
        return data;
      });


  private Lazy<ElBuilder> rootDiv = new Lazy<>(this::initRootDiv);

  private Lazy<ElBuilder> rootDiv_1 = new Lazy<>(this::initRootDiv_1);

  public BaseToDoAppComponent (ComponentOptions<El, ToDoAppComponent<El>> opts, ComponentConstructor<El, ToDoAppComponent<El>> ctor) {
    super(opts, ctor);
  }

  public BaseToDoAppComponent (Out1<El> el) {
    super(el);
  }

  public BaseToDoAppComponent (El el) {
    super(el);
  }

  protected ElBuilder initRootDiv () {

    ElBuilder b = newBuilder();

    b.append(
      "{text}"
    );
    b.setTagName("div");
    return b;
  }

  public abstract ElBuilder newBuilder (boolean searchable) ;

  public ElBuilder newBuilder () {
    return newBuilder(false);
  }

  protected void onElementResolved (El el) {
    ElementInjector<? super El> inj = newInjector(el);
    inj.appendChild(rootDiv.out1().getElement());
    inj.appendChild(rootDiv_1.out1().getElement());
    super.onElementResolved(el);
  }

  public abstract ElementInjector<? super El> newInjector (El el) ;

  protected ElBuilder initRootDiv_1 () {

    ElBuilder b = newBuilder();

    b.setTagName("div");
    return b;
  }

}
