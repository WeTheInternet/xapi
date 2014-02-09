package xapi.ui.autoui;

import xapi.inject.X_Inject;
import xapi.ui.autoui.api.UserInterface;
import xapi.ui.autoui.api.UserInterfaceFactory;

public class X_AutoUi {

  private X_AutoUi(){}
  
  @SuppressWarnings("unchecked")
  public static <T, U extends UserInterface<T>> U makeUi(T model, Class<? extends T> uiOptions, Class<U> uiType) {
    if (uiOptions == null) {
      assert model != null : "You must provide either a model object, or a model class";
      uiOptions = (Class<? extends T>) model.getClass();
    }
    U ui = instantiate(uiOptions, uiType);
    try {
      return ui;
    } finally {
      if (model != null) {
        ui.renderUi(model);
      }
    }
  }

  static <T, U extends UserInterface<T>> U instantiate(Class<? extends T> cls, Class<U> uiType) {
      return X_Inject
          .instance(UserInterfaceFactory.class)
          .createUi(cls, uiType);
  }
  
}
