package xapi.jre.ui.impl;

import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.ui.AbstractUiImplementationGenerator;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiFeatureGenerator;
import xapi.fu.Out2;
import xapi.jre.ui.impl.feature.JavaFxAlignFeatureGenerator;
import xapi.jre.ui.impl.feature.JavaFxCssFeatureGenerator;
import xapi.jre.ui.impl.feature.JavaFxFillFeatureGenerator;
import xapi.jre.ui.impl.feature.JavaFxSizeFeatureGenerator;

import java.util.Arrays;

/**
 * Created by james on 6/17/16.
 */
public class JavaFxUiGeneratorService extends AbstractUiImplementationGenerator<JavaFxUiGeneratorService.Ctx> {

  static final class Ctx extends ApiGeneratorContext<Ctx> {

  }

  @Override
  protected Iterable<Out2<String, UiComponentGenerator>> getComponentGenerators() {
    return Arrays.asList(
        Out2.out2Immutable("button", new JavaFxButtonComponentGenerator()),
        Out2.out2Immutable("box", new JavaFxBoxComponentGenerator()),
        Out2.out2Immutable("app", new JavaFxAppComponentGenerator())
    );
  }

  @Override
  protected String getImplPrefix() {
    return "JavaFx";
  }

  @Override
  protected Iterable<Out2<String, UiFeatureGenerator>> getFeatureGenerators() {
    return Arrays.asList(
        Out2.out2Immutable("ref", new UiFeatureGenerator()),
        Out2.out2Immutable("title", new UiFeatureGenerator()),
        Out2.out2Immutable("data", new UiFeatureGenerator()),
        Out2.out2Immutable("class", new JavaFxCssFeatureGenerator()),
        Out2.out2Immutable("body", new JavaFxBodyFeatureGenerator()),
        Out2.out2Immutable("text", new JavaFxTextFeatureGenerator()),
        Out2.out2Immutable("align", new JavaFxAlignFeatureGenerator()),
        Out2.out2Immutable("fill", new JavaFxFillFeatureGenerator()),
        Out2.out2Immutable("size", new JavaFxSizeFeatureGenerator()),
        Out2.out2Immutable("onClick", new JavaFxActionFeatureGenerator())
    );
  }
}
