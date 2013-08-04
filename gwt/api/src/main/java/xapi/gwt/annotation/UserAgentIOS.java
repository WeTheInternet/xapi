package xapi.gwt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@UserAgent(
    shortName="ios",
    selectorScript="return (ua.indexOf('Apple-iP') != -1)",
    fallbacks=UserAgentSafari.class
)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface UserAgentIOS {}
