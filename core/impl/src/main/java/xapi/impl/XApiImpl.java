package xapi.impl;

import xapi.api.XApiService;
import xapi.fu.In1.In1Unsafe;
import xapi.inject.X_Inject;
import xapi.util.X_Debug;

import javax.inject.Provider;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/14/16.
 */
public class XApiImpl {

  private XApiImpl() {}

  private static final Provider<XApiService> api = X_Inject.singletonLazy(XApiService.class);

  public static void runInXApi(In1Unsafe<XApiService> todo) {
    final XApiService x = api.get();
    try {
      todo.inUnsafe(x);
    } catch (Throwable throwable) {
      X_Debug.rethrow(throwable);
    }
  }


}
