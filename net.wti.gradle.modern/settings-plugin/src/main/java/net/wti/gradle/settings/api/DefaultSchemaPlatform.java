package net.wti.gradle.settings.api;

/**
 * Abstraction layer over a platform descriptor, like <mod-name replace=["main"] published=true />
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-06-14 @ 1:34 a.m..
 */
public class DefaultSchemaPlatform implements SchemaPlatform {
    private final String name;
    private final String replace;
    private String publishPattern;
    private boolean published;
    private boolean test;

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

    public boolean isPublished() {
        return published;
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
        return new DefaultSchemaPlatform(name,
                publishPattern,
                module.getReplace(),
                published || module.isPublished(),
                test || module.isTest());
    }

    @Override
    public String toString() {
        return "SchemaPlatform{" +
                "name='" + name + '\'' +
                ", publishPattern=" + publishPattern +
                ", replace=" + replace +
                ", published=" + published +
                ", test=" + test +
                '}';
    }
}
