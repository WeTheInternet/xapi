package xapi.annotation.compile;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/22/16.
 */
public @interface Specifier {

  String
      TYPE_JAR = "jar",
      TYPE_POM = "pom",
      TYPE_WAR = "war",
      CLASSIFIER_SOURCES = "sources",
      CLASSIFIER_TESTS = "tests";

  String classifier() default "";

  String type() default "jar";
}
