package net.wti.gradle.settings.api;

import xapi.fu.Debuggable;
import xapi.fu.log.Log;

///
/// Abstraction layer over a platform descriptor, like `<mod-name replace=["main"] published=true />`
///
/// Created by James X. Nelson (James@WeTheInter.net) on 2020-06-14 @ 1:34 a.m.
///
public class DefaultSchemaPlatform implements SchemaPlatform {
    private final String name;
    private final String replace;
    private String publishPattern;
    private boolean published;
    private boolean sourcePublished;
    private boolean sourceConsumed;
    private boolean test;
    private boolean disabled;

    public DefaultSchemaPlatform(
            String name,
            String publishPattern,
            String replace,
            boolean published,
            boolean test
    ) {
        this.name = name;
        this.publishPattern = publishPattern;
        this.replace = replace;
        this.published = published;
        this.test = test;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPublishPattern() {
        return publishPattern;
    }

    public String getReplace() {
        return replace;
    }

    @Override
    public boolean isPublished() {
        return published;
    }

    @Override
    public boolean isSourcePublished() {
        return sourcePublished;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public void setSourcePublished(final boolean sourcePublished) {
        this.sourcePublished = sourcePublished;
    }

    @Override
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public void setPublished(boolean published) {
        this.published = published;
    }

    public boolean isTest() {
        return test;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DefaultSchemaPlatform))
            return false;

        final DefaultSchemaPlatform that = (DefaultSchemaPlatform) o;

        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public SchemaPlatform update(SchemaPlatform module) {
        DefaultSchemaPlatform plat = new DefaultSchemaPlatform(name,
                publishPattern,
                module.getReplace(),
                published || module.isPublished(),
                test || module.isTest());
        if (isSourcePublished() || module.isSourcePublished()) {
            plat.setSourcePublished(true);
        }
        if (isSourceConsumed() || module.isSourceConsumed()) {
            plat.setSourceConsumed(true);
        }
        return plat;
    }

    @Override
    public String toString() {
        if (Debuggable.debugEnabled()) {
            return "SchemaPlatform{" +
                    "name='" + name + '\'' +
                    ", publishPattern=" + publishPattern +
                    ", replace=" + replace +
                    ", published=" + published +
                    ", test=" + test +
                    '}';
        }
        return "[" + name + "]";
    }

    @Override
    public boolean isSourceConsumed() {
        return sourceConsumed;
    }

    @Override
    public void setSourceConsumed(final boolean sourceConsumed) {
        this.sourceConsumed = sourceConsumed;
    }
}
