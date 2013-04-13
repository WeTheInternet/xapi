package xapi.reflect.api;

import xapi.except.NotLoadedYet;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

public interface ClassDataCallback <T>
extends SuccessHandler<Class<T>>, ErrorHandler<NotLoadedYet>{

}
