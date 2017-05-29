package xapi.test.components.xapi.test.components;


import xapi.ui.api.component.AbstractModelComponent;
import xapi.fu.Out1;
import xapi.model.X_Model;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ModelComponentOptions;

public abstract class BaseAsserterComponent <Node, El extends Node, ElBuilder extends NodeBuilder<El>> extends AbstractModelComponent<
    Node,
    El,
    ModelAsserter,
    AsserterComponent<Node, El>
  > implements AsserterComponent<Node, El> {

  protected ModelAsserter model;

  public ElBuilder root;

  public ElBuilder tag;

  public BaseAsserterComponent (Out1<El> El) {
    super(El);
  }

  public BaseAsserterComponent (El El) {
    super(El);
  }

  public BaseAsserterComponent (ModelComponentOptions<Node, El, ModelAsserter, AsserterComponent<Node, El>> opts, ComponentConstructor<Node, El, AsserterComponent<Node, El>> ctor) {
    super(opts, ctor);
  }

  public ModelAsserter getModel () {
    return model;
  }

  @Override
  public BaseAsserterComponent<Node, El, ElBuilder> setModel (ModelAsserter model) {
    this.model = model;
    return this;
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

  @Override
  protected ModelAsserter createModel() {
    return X_Model.create(ModelAsserter.class);
  }

}
