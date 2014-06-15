package xapi.dev.elemental;

import com.google.gwt.core.ext.typeinfo.JClassType;

import xapi.source.X_Source;

public class ModelProviderImpl implements ModelProvider {

  private String modelName;
  private String modelPackage;
  private String providerMethod;
  private String providerName;
  private String providerPackage;

  public ModelProviderImpl(String modelPkg, String modelName) {
    this.modelPackage = modelPkg;
    this.modelName = modelName;
  }
  public ModelProviderImpl(JClassType from) {
    this(from.getPackage().getName(), from.getQualifiedSourceName().replace(from.getPackage().getName()+".", ""));
  }
  /**
   * @return the modelName
   */
  public String getModelName() {
    return modelName;
  }
  /**
   * @param modelName the modelName to set
   */
  public void setModelName(String modelName) {
    this.modelName = modelName;
  }
  /**
   * @return the modelPackage
   */
  public String getModelPackage() {
    return modelPackage;
  }
  /**
   * @param modelPackage the modelPackage to set
   */
  public void setModelPackage(String modelPackage) {
    this.modelPackage = modelPackage;
  }

  @Override
  public String getModelQualifiedName() {
    return X_Source.qualifiedName(modelPackage, modelName);
  }

  public void setModel(Class<?> model) {
    setModelPackage(model.getPackage().getName());
    setModelName(model.getCanonicalName().replace(modelPackage+".", ""));
  }

  /**
   * @return the providerMethod
   */
  public String getProviderMethod() {
    return providerMethod;
  }
  /**
   * @param providerMethod the providerMethod to set
   */
  public void setProviderMethod(String providerMethod) {
    this.providerMethod = providerMethod;
  }
  /**
   * @return the providerName
   */
  public String getProviderName() {
    return providerName;
  }
  /**
   * @param providerName the providerName to set
   */
  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }
  /**
   * @return the providerPackage
   */
  public String getProviderPackage() {
    return providerPackage;
  }
  /**
   * @param providerPackage the providerPackage to set
   */
  public void setProviderPackage(String providerPackage) {
    this.providerPackage = providerPackage;
  }

  public void setProvider(Class<?> provider, String method) {
    setProviderPackage(provider.getPackage().getName());
    setProviderName(provider.getCanonicalName().replace(modelPackage+".", ""));
    setProviderMethod(method);
  }

}
