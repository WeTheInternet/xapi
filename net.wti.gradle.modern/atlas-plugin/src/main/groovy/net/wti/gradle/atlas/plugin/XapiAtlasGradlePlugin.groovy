package net.wti.gradle.atlas.plugin

import com.github.blueboxware.gdxplugin.GdxPlugin
import com.github.blueboxware.gdxplugin.tasks.PackTextures
import groovy.transform.CompileStatic
import net.wti.gradle.atlas.ext.NinePatchSpec
import net.wti.gradle.atlas.ext.ShapeFamilySpec
import net.wti.gradle.atlas.ext.StateSpec
import net.wti.gradle.atlas.ext.XapiAtlasExtension
import net.wti.gradle.atlas.images.AtlasNinePatchWriter
import net.wti.gradle.atlas.tasks.GenerateAtlasFamiliesTask
import net.wti.gradle.atlas.tasks.GenerateAtlasNinePatchesTask
import net.wti.gradle.atlas.tasks.GenerateAtlasPixelsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import static org.gradle.language.base.plugins.LifecycleBasePlugin.BUILD_TASK_NAME

///
/// ### XapiAtlasGradlePlugin
///
/// End-to-end pipeline to **generate** UI imagery and **pack** it into a LibGDX
/// texture atlas:
///
/// 1) `generateAtlasPixels`    — 1×1 PNGs from `xapiAtlas.pixels`
/// 2) `generateAtlasNinePatches` — low-level .9 from `xapiAtlas.ninePatches`
/// 3) `generateAtlasFamilies`    — high-level .9 from `xapiAtlas.families`
/// 4) `packXapiAtlas`            — TexturePacker stage (atlas + page PNGs)
/// 5) `copyXapiAtlas`            — copies atlas + PNGs to your resources dir
///
/// The plugin is intentionally headless (Java2D) and `@CompileStatic`.
///
/// Created by James X. Nelson (James@WeTheInter.net) and chat gpt on 20/08/2025 @ 20/08/2025 @ 01:24
@CompileStatic
@SuppressWarnings('unused') // usage: plugins { id 'net.wti.gradle.atlas' }
class XapiAtlasGradlePlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {
        project.plugins.apply(BasePlugin)
        project.plugins.apply(GdxPlugin)
        final XapiAtlasExtension ext = project.extensions.create('xapiAtlas', XapiAtlasExtension, project)

        // 1 — 1×1 pixels (color pallette)
        final TaskProvider<GenerateAtlasPixelsTask> generateAtlasPixels = project.tasks.register('generateAtlasPixels', GenerateAtlasPixelsTask) {
            final GenerateAtlasPixelsTask t ->
                applyCommonSettings(t)
                t.description = 'Generate 1x1 utility pixels from hex colors.'
                t.outputDir.set(ext.pixelsDir)
                t.pixels.set(ext.pixels)
        }

        // 2 — low-level .9 (NinePatchSpec)
        final TaskProvider<GenerateAtlasNinePatchesTask> generateAtlasNinePatches = project.tasks.register('generateAtlasNinePatches', GenerateAtlasNinePatchesTask) {
            final GenerateAtlasNinePatchesTask t ->
                applyCommonSettings(t)
                t.description = 'Generate .9.png files from NinePatchSpec entries.'
                t.outputDir.set(ext.ninePatchDir)
                t.ninePatches.set(project.providers.provider ({ ext.ninePatches.toList() })) // snapshot!
        }

        // 3 — high-level .9 (ShapeFamilySpec)
        final TaskProvider<GenerateAtlasFamiliesTask> generateAtlasFamilies = project.tasks.register('generateAtlasFamilies', GenerateAtlasFamiliesTask) {
            final GenerateAtlasFamiliesTask t ->
                t.group = 'xapi-atlas'
                applyCommonSettings(t)
                t.description = 'Generate .9.png files from ShapeFamilySpec families.'
                t.outputDir.set(ext.shapesDir)
                t.families.set(ext.families) // snapshot!
        }

        // 4 — pack atlas
        final TaskProvider<PackTextures> packXapiAtlas = project.tasks.register('packXapiAtlas', PackTextures) { p ->
            p.group = 'xapi-atlas'
            p.description = 'Pack generated pixels and nine-patches into a texture atlas.'
            addTaskOutputs(project, p, generateAtlasPixels)
            addTaskOutputs(project, p, generateAtlasNinePatches)
            addTaskOutputs(project, p, generateAtlasFamilies)
            p.into(ext.packedDir)

            p.packFileName = "${ext.atlasName.get()}.atlas"
            // TexturePacker settings suitable for UI sprites
            p.settings.flattenPaths = true
            p.settings.duplicatePadding = true
            p.settings.edgePadding = true
            p.settings.filterMag = 'Linear'
            p.settings.filterMin = 'Linear'
            p.settings.fast = project.findProperty('quick') != 'false'
        }

        // 5 — copy atlas + pages
        project.tasks.register('copyXapiAtlas', Copy) { c ->
            c.group = 'xapi-atlas'
            c.description = 'Copy atlas (.atlas + .png pages) into resources directory.'
            addTaskOutputs(project, c, packXapiAtlas)
            c.from({ ext.packedDir.get() })
            c.include "${ext.atlasName.get()}.atlas"
            c.include "${ext.atlasName.get()}*.png"   // supports multi-page atlases
            c.into(ext.outputDir)
            c.duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        // convenience: ensure packing happens on `build`
        project.tasks.named(BUILD_TASK_NAME).configure {
            it.dependsOn(packXapiAtlas)
        }
    }

    static void applyCommonSettings(Task task) {
        task.group = 'xapi-atlas'
        task.mustRunAfter('clean')
    }

    static void addTaskOutputs(final Project p, AbstractCopyTask c, TaskProvider<? extends Task> fileProvider) {
        final Provider<FileCollection> files = p.provider({fileProvider.get().outputs.files})
        c.inputs.files(files)
        c.from( files.map { it.singleFile})
    }
}
