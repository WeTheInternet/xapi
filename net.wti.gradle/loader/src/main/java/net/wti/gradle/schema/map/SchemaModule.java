package net.wti.gradle.schema.map;

import xapi.fu.data.SetLike;

/**
 * Abstraction layer over a module descriptor, like <mod-name require=["a", b] published=true />
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-06 @ 4:39 a.m..
 */
public class SchemaModule {
    private final String name;
    private final SetLike<String> require;
    private final boolean published;
    private final boolean test;

    public SchemaModule(String name, SetLike<String> require, boolean published, boolean test) {
        this.name = name;
        this.require = require;
        this.published = published;
        this.test = test;
    }

    public String getName() {
        return name;
    }

    public SetLike<String> getRequire() {
        return require;
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
        if (!(o instanceof SchemaModule))
            return false;

        final SchemaModule that = (SchemaModule) o;

        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    public SchemaModule update(SchemaModule module) {
        return new SchemaModule(name, require.addNow(module.require), published || module.published, test || module.test);
    }

    @Override
    public String toString() {
        return "SchemaModule{" +
            "name='" + name + '\'' +
            ", require=" + require +
            ", published=" + published +
            ", test=" + test +
            '}';
    }
}
