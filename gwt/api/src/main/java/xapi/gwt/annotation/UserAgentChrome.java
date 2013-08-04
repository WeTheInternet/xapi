package xapi.gwt.annotation;

@UserAgent(
    shortName="chrome",
    selectorScript="return (ua.indexOf('Chrome') != -1)",
    fallbacks=UserAgentSafari.class
)
public @interface UserAgentChrome {}
