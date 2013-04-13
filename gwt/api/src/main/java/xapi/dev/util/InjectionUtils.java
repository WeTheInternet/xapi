package xapi.dev.util;

public class InjectionUtils {

  public static String toSourceName(String simpleName) {
    return simpleName.replaceAll("[$]", ".");
  }

  public static String toUniqueName(String simpleName) {
    return simpleName.replaceAll("[$]", "_").replaceAll("[.]", "_");
  }

  public static String generatedProviderName(String simpleName) {
    return "ProviderOf_"+simpleName;
  }
  public static String generatedMagicClassName(String simpleName) {
    return simpleName+"_MC";
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

  //no instances allowed
  private InjectionUtils() {
  }

}
