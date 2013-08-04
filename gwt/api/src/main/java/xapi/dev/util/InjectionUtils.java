package xapi.dev.util;

public final class InjectionUtils {

  //no instances allowed
  private InjectionUtils() {}

  public static String generatedProviderName(String simpleName) {
    return "ProviderOf_"+simpleName;
  }
  public static String generatedAsyncProviderName(String simpleName) {
    return "CallbackFor_"+simpleName;
  }

  public static String generatedCallbackName(String simpleName) {
    return "RunAsync_"+simpleName;
  }

  public static String generatedSingletonName(String simpleName) {
    return "SingletonFor_"+simpleName;
  }


}
