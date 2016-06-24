package xapi.jre.ui.impl;

import xapi.dev.ui.AbstractUiGeneratorService;
import xapi.dev.ui.DataFeatureGenerator;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiFeatureGenerator;
import xapi.fu.Out2;

import static xapi.fu.Out2.out2;

import java.util.Arrays;

/**
 * Created by james on 6/17/16.
 */
public class UiGeneratorServiceJavaFx extends AbstractUiGeneratorService {



  protected Iterable<Out2<String, UiComponentGenerator>> getComponentGenerators() {
    return Arrays.asList(
        out2("app", new JavaFxAppComponentGenerator()),
        out2("button", new JavaFxButtonComponentGenerator())
    );
  }

  protected Iterable<Out2<String, UiFeatureGenerator>> getFeatureGenerators() {
    return Arrays.asList(
        out2("ref", new UiFeatureGenerator()),
        out2("title", new UiFeatureGenerator()),
        out2("data", new DataFeatureGenerator()),
        out2("body", new JavaFxBodyFeatureGenerator()),
        out2("text", new JavaFxTextFeatureGenerator()),
        out2("onClick", new JavaFxActionFeatureGenerator())
    );
  }
}
