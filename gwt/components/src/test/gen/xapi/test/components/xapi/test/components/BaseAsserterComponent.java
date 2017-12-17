package xapi.test.components.xapi.test.components;


import xapi.fu.Out1;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.component.AbstractModelComponent;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ModelComponentOptions;

public abstract class BaseAsserterComponent <El, ElBuilder extends NodeBuilder<El>> extends AbstractModelComponent<
    El,
    ModelAsserter,
    AsserterComponent<El>
  > implements AsserterComponent<El> {

  public ElBuilder root;

  public ElBuilder tag;

  public BaseAsserterComponent (ModelComponentOptions<El, ModelAsserter, AsserterComponent<El>> opts, ComponentConstructor<El, AsserterComponent<El>> ctor) {
    super(opts, ctor);
  }

  public BaseAsserterComponent (Out1<El> El) {
    super(El);
  }

  public BaseAsserterComponent (El El) {
    super(El);
  }

  public ElBuilder toDom () {
    ElBuilder root = newBuilder();
    root.append("<box>");
    if (getModel().getTag() != null) {
      ElBuilder tag = newBuilder();
      tag.append("<label>");
      tag.append(getModel().getTag());
      tag.append("</label>");
      root.addChild(tag);
    }
    ElBuilder elTemplate = newBuilder();
    elTemplate.append("<template>");
    elTemplate.append("</template>");
    root.addChild(elTemplate);
    root.append("</box>");
    return root;
  }

  public abstract ElBuilder newBuilder () ;

  public BaseAsserterComponent ui() {
    return this;
  }

}
