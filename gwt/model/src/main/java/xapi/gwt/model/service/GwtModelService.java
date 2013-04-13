package xapi.gwt.model.service;

import xapi.annotation.inject.SingletonOverride;
import xapi.inject.X_Inject;
import xapi.model.api.Model;
import xapi.model.service.ModelService;
import xapi.platform.GwtPlatform;
import xapi.util.api.SuccessHandler;

@GwtPlatform
@SingletonOverride(implFor=ModelService.class)
public class GwtModelService implements ModelService
{

  @Override
  public <T extends Model> T create(Class<T> key) {
    // GWT dev will make it here, and it can handle non-class-literal injection.
    // GWT prod requires magic method injection here.
    return X_Inject.instance(key);
  }

  @Override
  public void persist(Model model, SuccessHandler<Model> callback) {

  }

}
