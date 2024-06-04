package net.wti.gradle.settings.api;

import net.wti.gradle.system.tools.GradleCoerce;

import static net.wti.gradle.system.tools.GradleCoerce.isEmptyString;

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
 *         <gwt requires = { external: 'g:gwt-user:$gwtVersion' } /gwt>
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
 *  We want to create a SchemaDependency object to encapsulate each entry created in schema.xapi requires = {} json.
 *  For every require = declared, we will create a dependency object describing that request for a dependency.
 *
 *  This object does not represent a single discrete platform+module to platform+module dependency;
 *  we will consider omitted modules or platforms to mean "default platform" (main) or "default module" (main).
 *
 *  The PlatformModule {@link #coords} field describes any optional platform:module specifications.
 *
 *  Created by James X. Nelson (James@WeTheInter.net) on 2020-02-09 @ 7:30 a.m.
 */
public class SchemaDependency {
    private final DependencyType type;
    private final CharSequence group, name, version;
    private final PlatformModule coords;
    private Transitivity transitivity;
    private String extraGnv;

    public SchemaDependency(
            DependencyType type,
            PlatformModule coords,
            CharSequence group,
            CharSequence version,
            CharSequence name
    ) {
        this.type = type;
        this.coords = coords;
        this.group = group == null ? QualifiedModule.UNKNOWN_VALUE : group;
        this.name = name;
        this.version = version == null ? QualifiedModule.UNKNOWN_VALUE : version;
    }

    public SchemaDependency rebase(PlatformModule toMod) {
        SchemaDependency dep = new SchemaDependency(type, toMod, group, version, name);
        dep.transitivity = transitivity;
        dep.extraGnv = extraGnv;
        return dep;
    }

    public DependencyType getType() {
        return type;
    }

    public String getGroup() {
        return group.toString();
    }

    public String getGNV() {
        String v = getVersion();
        return getGroup() + ":" + getName() +
                (QualifiedModule.UNKNOWN_VALUE.equals(v) ? "" : ":" + v) +
                (GradleCoerce.isEmptyString(extraGnv) ? "" : extraGnv.startsWith(":") ? extraGnv : ":" + extraGnv);
    }

    // TODO: consider making this a structured object with a name and a transitivity / other options? or just put single item on SchemaDependency itself?
    public String getName() {
        return name.toString();
    }

    public String getVersion() {
        return version.toString();
    }

    public PlatformModule getCoords() {
        return coords;
    }

    public CharSequence getPlatformOrDefault() {
        PlatformModule src = getCoords();
        if (isEmptyString(src.getPlatform())) {
            return PlatformModule.defaultPlatform;
        }
        return src.getPlatform();
    }

    public CharSequence getModuleOrDefault() {
        PlatformModule src = getCoords();
        if (isEmptyString(src.getModule())) {
            return PlatformModule.defaultModule;
        }
        return src.getModule();
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

    public SchemaDependency withCoords(String plat, String mod) {
        return new SchemaDependency(type, new PlatformModule(plat, mod), group, version, name);
    }

    public Transitivity getTransitivity() {
        return transitivity == null ? Transitivity.api : transitivity;
    }

    public void setTransitivity(final Transitivity transitivity) {
        this.transitivity = transitivity;
    }

    public String getExtraGnv() {
        return extraGnv;
    }

    public void setExtraGnv(final String extraGnv) {
        this.extraGnv = extraGnv;
    }
}
