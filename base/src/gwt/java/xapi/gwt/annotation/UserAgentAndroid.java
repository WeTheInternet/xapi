package xapi.gwt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@UserAgent(
    shortName="android",
    selectorScript="return (ua.indexOf('Android') != -1)"
)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface UserAgentAndroid {}
