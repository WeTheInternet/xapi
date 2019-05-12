package xapi.annotation.ui;

import xapi.annotation.reflect.MirroredAnnotation;

@MirroredAnnotation
public @interface UiTemplate {

  final String
    $id = "$id",
    $class = "$class",
    $package = "$package",
    $method = "$method",
    $child = "$child";

  public enum SourceType {
    Literal, File, Resource
  }

  public enum Location {
    Body_Insert, Body_Prefix, Body_Suffix,
    Head_Insert, Head_Prefix, Head_Suffix
  }

  public enum EmbedStrategy {
    Insert, WrapEachMethod, WrapEachClass,
    WrapAllClasses, WrapAllPackages, WrapEachPackage;
  }

  Location location() default Location.Body_Insert;
  EmbedStrategy embedStrategy() default EmbedStrategy.Insert;
  String id() default "";
  String[] keys() default {"$id", "$child"};
  boolean required() default true;
  SourceType type() default SourceType.Literal;
  String value();


}
