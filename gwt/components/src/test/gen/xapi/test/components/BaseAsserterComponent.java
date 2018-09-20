package xapi.test.components;


import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.lazy.ResettableLazy;
import xapi.ui.api.ElementBuilder;
import xapi.ui.api.ElementInjector;
import xapi.ui.api.component.AbstractModelComponent;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ConditionalComponentMixin;
import xapi.ui.api.component.ModelComponentOptions;

public abstract class BaseAsserterComponent <El, ElBuilder extends ElementBuilder<El>> extends AbstractModelComponent<
    El,
    ModelAsserter,
    AsserterComponent<El>
  > implements AsserterComponent<El>, ConditionalComponentMixin<El,ElBuilder> {

  private Lazy<ElBuilder> rootBox = new Lazy<>(this::initRootBox);

  private ResettableLazy<ElBuilder> elIf = new ResettableLazy<>(this::initElIf);

  private ElBuilder selectedElIf;

  private Lazy<ElBuilder> tag = new Lazy<>(this::initTag);

  private Lazy<ElBuilder> elTemplate = new Lazy<>(this::initElTemplate);

  public BaseAsserterComponent (ModelComponentOptions<El, ModelAsserter, AsserterComponent<El>> opts, ComponentConstructor<El, AsserterComponent<El>> ctor) {
    super(opts, ctor);
  }

  public BaseAsserterComponent (Out1<El> el) {
    super(el);
  }

  public BaseAsserterComponent (El el) {
    super(el);
  }

  protected ElBuilder initRootBox () {

    ElBuilder b = newBuilder();

    b.setTagName("box");
    return b;
  }

  public abstract ElBuilder newBuilder (boolean searchable) ;

  public ElBuilder newBuilder () {
    return newBuilder(false);
  }

  protected ElBuilder initElIf () {
    if (getModel().getTag() != null) {
      return selectedElIf = tag.out1();
    }
    return selectedElIf = null;
  }

  protected void onElementResolved (El el) {
    ElementInjector<? super El> inj = newInjector(el);
    inj.appendChild(rootBox.out1().getElement());
    super.onElementResolved(el);
  }

  public abstract ElementInjector<? super El> newInjector (El el) ;

  protected void afterResolved (El el) {
    getModel().onChange("tag", (was, is)-> {
      redrawElIf(rootBox.out1().getElement());
    });
  }

  public El elTag () {
    return tag.out1().getElement();
  }

  protected ElBuilder initTag () {

    ElBuilder b = newBuilder();

    b.append(
      getModel().getTag()
    );
    b.setTagName("label");
    return b;
  }

  private void redrawElIf (El el) {
    renderConditional(el, selectedElIf, elIf, this::findSiblingElIf);
  }

  protected void beforeResolved (El el) {
    redrawElIf(rootBox.out1().getElement());
  }

  protected ElBuilder initElTemplate () {

    ElBuilder b = newBuilder();

    b.setTagName("template");
    return b;
  }

  protected El findSiblingElIf () {
    ElBuilder b = elTemplate.out1();
    return b.getElement();
  }

  public BaseAsserterComponent ui() {
    return this;
  }

}
