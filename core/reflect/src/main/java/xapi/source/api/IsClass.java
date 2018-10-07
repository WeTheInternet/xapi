package xapi.source.api;

import xapi.fu.Maybe;
import xapi.source.impl.IsClassDelegate;

public interface IsClass
extends IsMember,
HasMethods,
HasFields,
HasTypeParams,
HasInterfaces
{

  boolean isArray();

  default String getObjectName() {
    return getQualifiedComponentName();
  }

  boolean isAbstract();
  boolean isFinal();
  boolean isStatic();
  boolean isInterface();
  boolean isAnnotation();
  boolean isPrimitive();
  boolean isEnum();
  IsMethod getEnclosingMethod();
  Iterable<IsClass> getInnerClasses();

  default IsClass toArray(int arrayDepth) {
    if (arrayDepth < 1) {
      return this;
    }
    return new IsClassDelegate(this, arrayDepth);
  }

  Class<?> toClass(ClassLoader loader) throws ClassNotFoundException;

  @Override
  default Maybe<HasTypeParams> ifTypeParams() {
    return Maybe.immutable(this);
  }
}
