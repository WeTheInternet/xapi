package xapi.gwt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@UserAgent(
    shortName="ie10",
    selectorScript="return (ua.indexOf('msie') != -1 && ($doc.documentMode >= 10));",
    fallbacks={UserAgentIE8.class}
)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface UserAgentIE10 {}
