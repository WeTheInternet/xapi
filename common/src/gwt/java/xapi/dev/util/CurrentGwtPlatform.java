package xapi.dev.util;

import java.lang.annotation.Annotation;
import java.util.List;

import xapi.collect.init.AbstractMultiInitMap;
import xapi.except.NotConfiguredCorrectly;
import xapi.platform.Platform;
import xapi.util.X_Util;

import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;

public class CurrentGwtPlatform {

  private static final AbstractMultiInitMap<String,PlatformSet,GeneratorContext>
    runtime = new AbstractMultiInitMap<String,PlatformSet,GeneratorContext>(
      AbstractMultiInitMap.PASS_THRU) {

	@SuppressWarnings("unchecked")
  @Override
    protected PlatformSet initialize(String key, GeneratorContext context) {
      PlatformSet all = new PlatformSet();
      boolean isDebug;
      try {
        ConfigurationProperty debugProp = context.getPropertyOracle().getConfigurationProperty("xapi.debug");
        isDebug = debugProp == null ? false : debugProp.getValues().contains("true");
      }catch (Exception e) {
        isDebug = false;
      }

      // Step one: load the platform type we need.
      try {
        Class<?> cls = Class.forName(key);
        assert (Annotation.class.isAssignableFrom(cls)) :
          "You may only use annotation types when selecting a platform type. " +
          "You sent "+key;
        // Once we have the class, let's grab it's Platform annotation.
        Platform platform = cls.getAnnotation(Platform.class);
        if (!isDebug && platform.isDebug()) {
          // debug mismatch.  bail now.
          return all;
        }
        if (platform == null)
          throw new NotConfiguredCorrectly(
            "You have specified a runtime platform of type "+key+", but this " +
            		"annotation is not annotated with @Platform."
          );
        assert !platform.isServer() : "Server platforms should never be used in gwt.  " +
        		"Check your annotation hierarchy for unexpected fallback types.";
        // prefer self first
        all.add((Class<? extends Annotation>)cls);
        //now, throw in any fallback types...
        for (Class<? extends Annotation> fallback : platform.fallback()) {
          // need to check these fallback types for debug level as well.
          Platform anno = fallback.getAnnotation(Platform.class);
          // more helpful error message than NPE if something went wrong.
          assert anno != null : "Fallback annotation "+fallback.getName()+" " +
          		"is specified as a platform type, but is not annotated w/ @Platform";
          if (isDebug || !anno.isDebug()) {
            // only add types if they are non-debug type, or we are in debug mode.
            all.add(fallback);
          }
        }
      } catch (Exception e) {
        // probably don't have the annotation types on classpath...
        e.printStackTrace();
        assert false : e;
      }
      return all;
    };
  };

  public static PlatformSet getPlatforms(GeneratorContext context) {
    final ConfigurationProperty prop;
    try {
      if (context.isProdMode()) {
        prop = context.getPropertyOracle().getConfigurationProperty("xapi.platform.prod");
      } else {
        prop = context.getPropertyOracle().getConfigurationProperty("xapi.platform.dev");
      }
    } catch (Throwable e) {
      throw X_Util.rethrow(e);
    }
    // TODO: filter on isDebug based on whether or not the flag is set...
    PlatformSet platforms = new PlatformSet();
    List<String> vals = prop.getValues();
    for (int i = 0; i < vals.size(); i++) {
      for (Class<? extends Annotation> platform : runtime.get(vals.get(i), context)) {
        platforms.add(platform);
      }
    }
    assert platforms.size() > 0 : "No runtime platform found for gwt.";
    return platforms;
  }

}
