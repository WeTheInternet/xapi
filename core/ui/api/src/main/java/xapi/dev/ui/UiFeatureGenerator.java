package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiExpr;
import xapi.fu.Lazy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/3/16.
 */
public class UiFeatureGenerator {

  private String featurePattern;
  // TODO: a regex abstraction like GWT's RegExp class (but no GWT dependencies)
  private Lazy<Pattern> pattern;

  public UiFeatureGenerator() {
    resetPattern();
  }

  protected void resetPattern() {
    pattern = Lazy.ofDeferred(Pattern::compile, this::getFeaturePattern);
  }

  public double matchScore(String featureName, UiExpr value) {
    final Pattern regex = pattern.out1();
    final Matcher matcher = regex.matcher(featureName);
    if (matcher.matches()) {
      if (regex.toString().equals(featureName)) {
        return 1;
      }
      double score = featureName.length();
      return Math.max(0.01, Math.min(0.9, (score - matcher.groupCount()) / score));
    }
    return 0;
  }

  public String getFeaturePattern() {
    return featurePattern;
  }

  public void setFeaturePattern(String featurePattern) {
    this.featurePattern = featurePattern;
  }

  public boolean hasSideEffects() {
    return false;
  }

  public boolean shouldVisitorSuper(UiGeneratorService service, UiComponentGenerator myGenerator, UiAttrExpr n) {
    return true;
  }

  public void finishVisit(UiGeneratorService service, UiComponentGenerator myGenerator, UiAttrExpr n) {

  }
}
