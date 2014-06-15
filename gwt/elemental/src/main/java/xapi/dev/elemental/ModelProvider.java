package xapi.dev.elemental;


public interface ModelProvider {

  String getModelPackage();
  String getModelName();
  String getModelQualifiedName();

  String getProviderPackage();
  String getProviderName();
  String getProviderMethod();


}
