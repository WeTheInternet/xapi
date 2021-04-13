package net.wti.gradle.schema.api;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.require.api.PlatformModule;
import xapi.util.X_String;

import java.io.File;

import static net.wti.gradle.schema.api.QualifiedModule.mangleProjectPath;

/**
 * SchemaDirs:
 * <p>
 * <p>
 * A place to collect useful "default paths", based off a single base File: indexDir.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 04/04/2021 @ 2:56 a.m..
 */
public interface SchemaDirs {

    File getDirIndex();

    default File getDirByPpm() {
        return new File(getDirIndex(), "path");
    }

    default File getDirByGnv() {
        return new File(getDirIndex(), "coord");
    }

    default String calcPPM(final SchemaProject project, final SchemaPlatform platform, final SchemaModule module) {
        String projName = project.getPathIndex();
        String children = QualifiedModule.unparse(platform.getName(), module.getName());
        return projName + ":" + children;
    }

    default File calcPPM(final File root, final SchemaProject project, final SchemaPlatform platform, final SchemaModule module) {
        String ppm = calcPPM(project, platform, module);
        File dir = new File(root, ppm.replace(':', File.separatorChar));
        return dir;
    }

    default File calcPPM(File root, SchemaDependency dep, CharSequence defaultProject, CharSequence defaultPlatform, CharSequence defaultModule) {
        String ppm = calcPPM(dep, defaultProject, defaultPlatform, defaultModule);
        File dir = new File(root, ppm.replace(':', File.separatorChar));
        return dir;
    }

    default String calcPPM(SchemaDependency dep, CharSequence defaultProject, CharSequence defaultPlatform, CharSequence defaultModule) {
        PlatformModule platMod = dep.getCoords();
        final String projectName;
        String name = dep.getName();
        switch (dep.getType()) {
            case internal:
                platMod = platMod.edit(dep.getName());
            case external:
                projectName = QualifiedModule.mangleProjectPath(defaultProject.toString());
                break;
            case project:
            case unknown:
                if (!name.startsWith(":")) {
                    name = ":" + name;
                }
                projectName = QualifiedModule.mangleProjectPath(name);
                break;
            default:
                throw new IllegalArgumentException("Dependency type " + dep.getType() + " not yet supported by getPPM");
        }
        String plat = X_String.firstNotEmpty(platMod.getPlatform(), defaultPlatform.toString());
        String mod = X_String.firstNotEmpty(platMod.getModule(), defaultModule.toString());
        return projectName + ":" + QualifiedModule.unparse(plat, mod);

    }

    default File calcDependencyProjectDir(final SchemaDependency dep, final SchemaProject project, final SchemaPlatform platform, final SchemaModule module) {
        final String ppm;
        CharSequence projectPath = project.getPathIndex();
        ppm = calcPPM(dep, projectPath, platform.getName(), module.getName());
        final File ppmDir = new File(getDirByPpm(), ppm.replace(':', File.separatorChar));
        return ppmDir;
    }

    default File calcProjectDir(final SchemaProject project, CharSequence platform, CharSequence module) {
        final File projectDir = calcProjectDir(project);
        final String platMod = QualifiedModule.unparse(platform.toString(), module.toString());
        //noinspection UnnecessaryLocalVariable (easier debugging return values)
        final File resultDir = new File(projectDir, platMod);
        return resultDir;
    }

    default File calcProjectDir(final SchemaProject project) {
        final String path = project.getPathIndex();
        final File byPath = new File(getDirIndex(), "path"); // organized by gradle path $buildName:$project:$path
        //noinspection UnnecessaryLocalVariable (easier debugging return values)
        File projectDir = new File(byPath, path);
        return projectDir;
    }

}
