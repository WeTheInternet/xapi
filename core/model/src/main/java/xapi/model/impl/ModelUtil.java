package xapi.model.impl;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import xapi.annotation.model.DeleterFor;
import xapi.annotation.model.GetterFor;
import xapi.annotation.model.IsModel;
import xapi.annotation.model.SetterFor;
import xapi.annotation.reflect.Fluent;
import xapi.except.NotConfiguredCorrectly;
import xapi.model.api.Model;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelManifest.MethodData;

public class ModelUtil {

  public static ModelManifest createManifest(final Class<? extends Model> cls) {
    final ModelManifest manifest = new ModelManifest(cls);
    final Set<Class<?>> allTypes = new LinkedHashSet<>();
    collectAllTypes(allTypes, cls);
    // Collect up all the fields that are user defined...
    for (final Class<?> type : allTypes) {
      if (type == Model.class) {
        continue;
      }
      for (final Method method : type.getMethods()) {
        if (method.getDeclaringClass() != Model.class) {
          if (!manifest.hasSeenMethod(method.getName())) {
            final MethodData property = manifest.addProperty(method.getName(), method.getAnnotation(GetterFor.class),
                method.getAnnotation(SetterFor.class), method.getAnnotation(DeleterFor.class));
            final Class<?> dataType;
            if (property.isGetter(method.getName())) {
              // For a getter, we will determine the field type by the return type
              dataType = method.getReturnType();
            } else {
              // For setters and deleters, we will determine the field type by the first parameter type.
              // However, a removeAll method may have no parameters, in which case we will have to wait.
              if (method.getParameterTypes().length > 0) {
                dataType = method.getParameterTypes()[0];
              } else {
                dataType = null;
              }
            }
            if (dataType != null) {
              final Class<?> oldType = property.getType();
              if (oldType != null && oldType != dataType) {
                throw new NotConfiguredCorrectly("Field "+property.getName()+" for "+cls+" has data type "
                    + "disagreement; already saw type "+oldType+" but now saw "+dataType+". Get/set/remove methods "
                    + "must have identical type information");
              }
              property.setType(dataType);
            }
            property.addAnnotations(method.getAnnotations());
          }
        }
      }
    }
    return manifest;
  }

  private static void collectAllTypes(final Collection<Class<?>> into, final Class<?> cls) {
    into.add(cls);
    final Class<?> superCls = cls.getSuperclass();
    if (superCls != null && superCls != Object.class) {
      collectAllTypes(into, cls);
    }
    collectInterfaces(into, cls);
  }

  private static void collectInterfaces(final Collection<Class<?>> into, final Class<?> cls) {
    for (final Class<?> iface : cls.getInterfaces()) {
      if (into.add(iface)) {
        collectInterfaces(into, iface);
      }
    }
  }

  /**
   * @param method
   * @return
   */
  public static boolean isFluent(final Method method) {
    final Class<?> methodType = method.getDeclaringClass();
    final Class<?> returnType = method.getReturnType();
    if (returnType == null || returnType == void.class) {
      return false;
    }
    if (
        areAssignable(methodType, returnType)
        ) {
      // Returning this would be allowed.
      // However, we should guard against methods that may actually want to return a field
      // that is the same type as itself.
      final Fluent fluent = method.getAnnotation(Fluent.class);
      if (fluent != null) {
        return fluent.value();
      }
      if (method.getParameterTypes().length > 0) {
        // check if there is a single parameter type which is also compatible,
        // and throw an error telling the user that they must specify @Fluent(true) or @Fluent(false)
        // Because the method signature is ambiguous
        if (areAssignable(methodType, method.getParameterTypes()[0])) {
          throw new NotConfiguredCorrectly("Method "+method.toGenericString()+" in "+methodType
              +" has ambiguous return type; cannot tell if this method is Fluent. Please annotate "
              + "this method with @Fluent(true) if the method is supposed to `return this;` or "
              + "use @Fluent(false) if this method is supposed to return the first parameter.");
        }
      }
      return true;
    }
    return false;
  }

  /**
   * @return true if either type is assignable to the other.
   */
  public static boolean areAssignable(final Class<?> type1, final Class<?> type2) {
    return type1.isAssignableFrom(type2)
        || type2.isAssignableFrom(type1);
  }

  /**
   * @param cls -> The class to convert into a model name.
   * @return cls.getSimpleName().replace("Model", "");
   */
  public static String guessModelType(final Class<? extends Model> cls) {
    final IsModel isModel = cls.getAnnotation(IsModel.class);
    if (isModel == null) {
      return guessModelType(cls.getSimpleName());
    } else {
      return isModel.modelType();
    }
  }

  public static String guessModelType(final String simpleName) {
    final String type = simpleName.replace("Model", "");
    assert type.length() > 0 : "Cannot have a model class named Model!";
    return Character.toLowerCase(type.charAt(0)) + type.substring(1);
  }

}
