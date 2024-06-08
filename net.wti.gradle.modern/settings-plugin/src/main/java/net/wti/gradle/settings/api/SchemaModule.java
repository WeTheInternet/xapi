package net.wti.gradle.settings.api;

import org.gradle.api.Named;
import xapi.fu.Debuggable;
import xapi.fu.data.SetLike;

/**
 * Abstraction layer over a module descriptor, like <mod-name require=["a", b] published=true />
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-06 @ 4:39 a.m.
 */
public class SchemaModule implements Named {
    private final String name;
    private final SetLike<String> include;
    private String publishPattern;
    private boolean published;
    private boolean test;
    private boolean force;

    public SchemaModule(String name, SetLike<String> include, boolean published, boolean test, final boolean force) {
        this(name, "main".equals(name) ? "$name" : "$module-$name", include, published, test, force);
    }

    public SchemaModule(String name, String publishPattern, SetLike<String> include, boolean published, boolean test, final boolean force) {
        this.name = name;
        this.publishPattern = publishPattern;
        this.include = include;
        this.published = published;
        this.test = test;
        this.force = force;
    }

    @Override
    public String getName() {
        return name;
    }

    public SetLike<String> getInclude() {
        return include;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isTest() {
        return test;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SchemaModule))
            return false;

        final SchemaModule that = (SchemaModule) o;

        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    public String getPublishPattern() {
        return publishPattern;
    }

    public SchemaModule updatePublished(boolean published) {
        this.published = published;
        return this;
    }

    public SchemaModule updatePublishPattern(String publishPattern) {
        this.publishPattern = publishPattern;
        return this;
    }

    public SchemaModule updateTest(boolean test) {
        this.test = test;
        return this;
    }

    public SchemaModule updateForce(boolean force) {
        this.force = force;
        return this;
    }

    public SchemaModule update(SchemaModule module) {
        return new SchemaModule(name, module.publishPattern, include.addNow(module.include), published || module.published, test || module.test, force || module.force);
    }

    @Override
    public String toString() {
        if (Debuggable.debugEnabled()) {
            return "SchemaModule{" +
                    "name='" + name + '\'' +
                    ", publishedPattern='" + publishPattern + '\'' +
                    ", require=" + include +
                    ", published=" + published +
                    ", test=" + test +
                    '}';
        }
        return "@" + name;
    }

}

