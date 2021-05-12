package xapi.annotation.common;

import xapi.annotation.mirror.MirroredAnnotation;

@MirroredAnnotation
public @interface Property {

  String name();
  String value() default "";
  
}
