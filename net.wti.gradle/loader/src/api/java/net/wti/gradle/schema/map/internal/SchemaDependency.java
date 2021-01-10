package net.wti.gradle.schema.map.internal;

import net.wti.gradle.require.api.DependencyType;
import net.wti.gradle.require.api.PlatformModule;
import xapi.fu.itr.MappedIterable;
import xapi.util.X_String;

/**
 * An object representing a dependency relationship created in schema.xapi;
 * Given:
 * <code><pre>
 * <xapi-schema
 *      // these are "global requires", which are added as implementation scope dependencies to all projects defined in this schema.
 *      // use requires at this level sparingly / only in "leaf" modules.
 *      requires = {
 *         project: common, util
 *         internal: [ 'jre:spi', 'api' ] // api would be 'main:api' (or whatever is default platform for consumer gradle project)
 *         external: [ 'g:n:v', 'jre', 'api' ]
 *      }
 *
 *      // or...
 *
 *      requires = [ common ] // translates to: requires = { project: [ common ] }
 *
 *      platforms = [
 *         <gwt requires = { external 'g:gwt-user:$gwtVersion' } /gwt>
 *     ]
 *
 *     modules = [
 *         <impl requires = { ... } /impl>
 *     ]
 *
 *     projects = [
 *          <some-project
 *              requires = {
 *                  // global to this project
 *                  project 'other-project'
 *                  // special key by_module to support per-module requires within a project configuration.
 *                  by_module: {
 *                      spi: { project 'some-spi' }
 *                  }
 *              }
 *          /some-project>
 *     ]
 * /xapi-schema>
 *
 *
 * </pre></code>
 *
 *  We want to create a SchemaDependency object to encapsulated each entry created in schema.xapi requires = {} json.
 *
 *  Created by James X. Nelson (James@WeTheInter.net) on 2020-02-09 @ 7:30 a.m..
 */
public class SchemaDependency {
    private final DependencyType type;
    private final String group, version;
    private final PlatformModule coords;
    private final CharSequence name;

    public SchemaDependency(
        DependencyType type,
        PlatformModule coords,
        String group,
        String version,
        CharSequence name
    ) {
        this.type = type;
        this.coords = coords;
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public DependencyType getType() {
        return type;
    }

    public String getGroup() {
        return group;
    }

    // TODO: consider making this a structured object with a name and a transitivity / other options? or just put single item on SchemaDependency itself?
    public String getName() {
        return name.toString();
    }

    public String getVersion() {
        return version;
    }

    public PlatformModule getCoords() {
        return coords;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SchemaDependency))
            return false;

        final SchemaDependency that = (SchemaDependency) o;

        if (!getGroup().equals(that.getGroup()))
            return false;
        if (!getVersion().equals(that.getVersion()))
            return false;
        if (!coords.equals(that.coords))
            return false;
        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        int result = getGroup().hashCode();
        result = 31 * result + getVersion().hashCode();
        result = 31 * result + coords.hashCode();
        result = 31 * result + getName().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SchemaDependency{" +
            "type=" + type +
            ", group='" + group + '\'' +
            ", version='" + version + '\'' +
            ", coords=" + coords +
            ", name=" + name +
            '}';
    }
}
