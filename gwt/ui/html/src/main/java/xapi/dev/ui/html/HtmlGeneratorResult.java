package xapi.dev.ui.html;

import xapi.source.X_Source;

import com.google.gwt.core.ext.typeinfo.JClassType;

public class HtmlGeneratorResult {
  private JClassType sourceType;
  private final String finalName;
  private final String packageName;

  public HtmlGeneratorResult(JClassType existing, String pkgName, String name) {
    this.setSourceType(existing);
    this.finalName = name;
    this.packageName = pkgName;
  }
  public final String getFinalName() {
    return finalName;
  }

  public final String getFinalNameQualified() {
    return X_Source.qualifiedName(packageName, finalName);
  }
  /**
   * @return the sourceType
   */
  public JClassType getSourceType() {
    return sourceType;
  }
  /**
   * @param sourceType the sourceType to set
   */
  public void setSourceType(JClassType sourceType) {
    this.sourceType = sourceType;
  }
  /**
   * @return the packageName
   */
  public String getPackageName() {
    return packageName;
  }
}