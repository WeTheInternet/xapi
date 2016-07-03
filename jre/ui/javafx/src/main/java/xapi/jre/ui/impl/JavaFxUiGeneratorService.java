package xapi.jre.ui.impl;

import xapi.dev.ui.AbstractUiImplementationGenerator;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiFeatureGenerator;
import xapi.fu.Out2;
import xapi.jre.ui.impl.feature.JavaFxAlignFeatureGenerator;
import xapi.jre.ui.impl.feature.JavaFxFillFeatureGenerator;
import xapi.jre.ui.impl.feature.JavaFxSizeFeatureGenerator;

import static xapi.fu.Out2.out2;

import java.util.Arrays;

/**
 * Created by james on 6/17/16.
 */
public class JavaFxUiGeneratorService extends AbstractUiImplementationGenerator {

  @Override
  protected Iterable<Out2<String, UiComponentGenerator>> getComponentGenerators() {
    return Arrays.asList(
        out2("button", new JavaFxButtonComponentGenerator()),
        out2("box", new JavaFxBoxComponentGenerator()),
        out2("app", new JavaFxAppComponentGenerator())
    );
  }

  @Override
  protected String getImplPrefix() {
    return "JavaFx";
  }

  @Override
  protected Iterable<Out2<String, UiFeatureGenerator>> getFeatureGenerators() {
    return Arrays.asList(
        out2("ref", new UiFeatureGenerator()),
        out2("title", new UiFeatureGenerator()),
        out2("data", new UiFeatureGenerator()),
        out2("body", new JavaFxBodyFeatureGenerator()),
        out2("text", new JavaFxTextFeatureGenerator()),
        out2("align", new JavaFxAlignFeatureGenerator()),
        out2("fill", new JavaFxFillFeatureGenerator()),
        out2("size", new JavaFxSizeFeatureGenerator()),
        out2("onClick", new JavaFxActionFeatureGenerator())
    );
  }
}
