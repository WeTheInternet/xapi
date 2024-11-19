package xapi.ui.autoui;

import xapi.inject.X_Inject;
import xapi.ui.autoui.api.UserInterface;
import xapi.ui.autoui.api.UserInterfaceFactory;

public class X_AutoUi {

  private X_AutoUi(){}

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T, U extends UserInterface<T>> U makeUi(T model, Class<? extends T> uiOptions, Class<? super U> uiType) {
    if (uiOptions == null) {
      assert model != null : "You must provide either a model object, or a model class";
      uiOptions = (Class<? extends T>) model.getClass();
    }
    U ui = (U) instantiate((Class)uiOptions, (Class)uiType);
    try {
      return ui;
    } finally {
      if (model != null) {
        ui.renderUi(model);
      }
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static <T, U extends UserInterface<T>> U instantiate(Class<? extends T> cls, Class<? super U> uiType) {
      return (U) X_Inject
          .instance(UserInterfaceFactory.class)
          .createUi((Class)cls, (Class)uiType);
  }

}
