package xapi.jre.ui.impl;

import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiFeatureGenerator;
import xapi.dev.ui.AbstractUiGeneratorService;
import xapi.fu.Out2;

import java.util.Arrays;

/**
 * Created by james on 6/17/16.
 */
public class UiGeneratorServiceJavaFx extends AbstractUiGeneratorService {



  protected Iterable<Out2<String, UiComponentGenerator>> getComponentGenerators() {
    return Arrays.asList(
        Out2.out2("app", new JavaFxAppComponentGenerator()),
        Out2.out2("button", new UiComponentGenerator())
    );
  }

  protected Iterable<Out2<String, UiFeatureGenerator>> getFeatureGenerators() {
    return Arrays.asList(
        Out2.out2("ref", new UiFeatureGenerator()),
        Out2.out2("title", new UiFeatureGenerator()),
        Out2.out2("body", new UiFeatureGenerator()),
        Out2.out2("text", new UiFeatureGenerator()),
        Out2.out2("onClick", new UiFeatureGenerator())
    );
  }
}
