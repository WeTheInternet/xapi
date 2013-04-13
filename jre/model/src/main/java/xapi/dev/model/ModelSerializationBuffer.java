package xapi.dev.model;

import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;

public class ModelSerializationBuffer extends MethodBuffer {

  public ModelSerializationBuffer(SourceBuilder<ModelGeneratorContext> context) {
    super(context);
  }

  @Override
  protected void onFirstAppend() {
    //on first call, let's print our boilerplate
    context.getImports().addImport(StringBuffer.class.getName());
    println("StringBuffer buffer = new StringBuffer();");
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
