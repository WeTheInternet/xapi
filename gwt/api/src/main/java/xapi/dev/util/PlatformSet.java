package xapi.dev.util;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;

import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.inject.SingletonOverride;
import xapi.except.NotConfiguredCorrectly;
import xapi.log.X_Log;
import xapi.platform.Platform;

import com.google.gwt.core.ext.typeinfo.JClassType;

public class PlatformSet extends LinkedHashSet<Class<? extends Annotation>>{

  private static final long serialVersionUID = 2912913282325899050L;


  public JClassType prefer(JClassType first, JClassType second, Class<? extends Annotation> qualifier) {
    Annotation firstQualifier = first.getAnnotation(qualifier);
    Annotation secondQualifier = second.getAnnotation(qualifier);
    if (firstQualifier == null)
      return secondQualifier == null ? null : second;
    if (secondQualifier == null)
      return first;

    for (Class<? extends Annotation> best : this) {
      for (Annotation anno : first.getAnnotations()) {
        if (anno.annotationType() == best) {
          // first matches our best platform type.  We may have to check
          // priority if second is also of the desire platform type
          for (Annotation secondAnno : second.getAnnotations()) {
            if (secondAnno.annotationType() == best) {
              X_Log.debug("Choosing between",first, second);
              // Both types are of the same platform.
              if (qualifier.getName().endsWith("Default"))
                throw new NotConfiguredCorrectly("Cannot have two types registered " +
                		"as the "+qualifier.getSimpleName()+" for the same platform.\n" +
            				"Problem types are: "+first.getQualifiedBinaryName()+" and "+
                		second.getQualifiedBinaryName()+".  These types may not both " +
            				"be marked as default.  Change one to "+
                		qualifier.getSimpleName().replace("Default", "Override")+"."
            		);
              int firstPriority, secondPriority;
              if (qualifier == SingletonOverride.class) {
                firstPriority = ((SingletonOverride)firstQualifier).priority();
                secondPriority = ((SingletonOverride)firstQualifier).priority();
              } else if (qualifier == InstanceOverride.class) {
                firstPriority = ((InstanceOverride)firstQualifier).priority();
                secondPriority = ((InstanceOverride)firstQualifier).priority();
              } else {
                throw new IllegalStateException("Unknown injection qualifier: "+qualifier);
              }
              JClassType winner = secondPriority
              < firstPriority
              ? first : second;
              X_Log.debug("Winner:",winner);
              return winner;
            }
          }
          X_Log.debug("Selecting",first);
          // didn't return, second does not have our best platform type
          return first;
        }
      }
      // didn't return, first is not best
      for (Annotation anno : second.getAnnotations()) {
        if (anno.annotationType() == best) {
          X_Log.debug("Selecting",second);
          return second;
        }
      }
    }
    X_Log.debug("Selecting",first);
    return first;
  }

  public boolean isAllowedType (JClassType type) {

    // If this type has a platform annotation, it must match the types contained
    // within this collection.  If there is no platform anno, we accept the type
    boolean useType = true;
    for (Annotation anno : type.getAnnotations()) {
      if (anno.annotationType().getAnnotation(Platform.class) != null) {
        if (contains(anno.annotationType()))
          return true;
        else
          // A platform we don't match doesn't preclude having a platform
          // we do match
          useType = false;
      }
    }

    return useType;
  }

}
