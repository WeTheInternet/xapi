package net.wti.gradle.schema.map;

/**
 * Abstraction layer over a platform descriptor, like <mod-name replace=["main"] published=true />
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-06 @ 4:39 a.m..
 */
public class SchemaPlatform {
    private final String name;
    private final String replace;
    private final boolean published;
    private final boolean test;

    public SchemaPlatform(String name, String replace, boolean published, boolean test) {
        this.name = name;
        this.replace = replace;
        this.published = published;
        this.test = test;
    }

    public String getName() {
        return name;
    }

    public String getReplace() {
        return replace;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isTest() {
        return test;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SchemaPlatform))
            return false;

        final SchemaPlatform that = (SchemaPlatform) o;

        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    public SchemaPlatform update(SchemaPlatform module) {
        return new SchemaPlatform(name, module.replace, published || module.published, test || module.test);
    }

    @Override
    public String toString() {
        return "SchemaPlatform{" +
            "name='" + name + '\'' +
            ", replace=" + replace +
            ", published=" + published +
            ", test=" + test +
            '}';
    }
}
