package net.wti.gradle.atlas.plugin

import com.github.blueboxware.gdxplugin.GdxPlugin
import com.github.blueboxware.gdxplugin.tasks.PackTextures
import groovy.transform.CompileStatic
import net.wti.gradle.atlas.ext.NinePatchSpec
import net.wti.gradle.atlas.ext.ShapeFamilySpec
import net.wti.gradle.atlas.ext.StateSpec
import net.wti.gradle.atlas.ext.XapiAtlasExtension
import net.wti.gradle.atlas.images.AtlasNinePatchWriter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
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
/// 3) `generateAtlasShapes`    — high-level .9 from `xapiAtlas.families`
/// 4) `packXapiAtlas`          — TexturePacker stage (atlas + page PNGs)
/// 5) `copyXapiAtlas`          — copies atlas + PNGs to your resources dir
///
/// The plugin is intentionally headless (Java2D) and `@CompileStatic`.
///
/// Created by James X. Nelson (James@WeTheInter.net) and chat gpt on 20/08/2025 @ 20/08/2025 @ 01:24
@CompileStatic
@SuppressWarnings('unused') // usage: plugins { id 'net.wti.gradle.atlas' }
class XapiAtlasGradlePlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {
        project.plugins.apply(GdxPlugin)
        final XapiAtlasExtension ext = project.extensions.create('xapiAtlas', XapiAtlasExtension, project)

        // 1 — 1×1 pixels (color pallette)
        final TaskProvider<Task> generateAtlasPixels = project.tasks.register('generateAtlasPixels') { t ->
            t.group = 'xapi-atlas'
            t.description = 'Generate 1x1 utility pixels from hex colors.'
            t.outputs.dir(ext.pixelsDir)
            t.doLast {
                final File outDir = ext.pixelsDir.get().asFile
                outDir.mkdirs()
                Map<String, String> map = ext.pixels.get()
                if (map.isEmpty()) map = [white: "#ffffffff"]
                map.each { String name, String hex ->
                    final File png = new File(outDir, "${name}.png")
                    png.parentFile.mkdirs()
                    final BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    img.setRGB(0, 0, AtlasNinePatchWriter.parseHexToARGB(hex))
                    ImageIO.write(img, "png", png)
                    t.logger.lifecycle("Generated 1x1 pixel ${name} (${hex}) -> ${png}")
                }
            }
        }

        // 2 — low-level .9 (NinePatchSpec)
        final TaskProvider<Task> generateAtlasNinePatches = project.tasks.register('generateAtlasNinePatches') { t ->
            t.group = 'xapi-atlas'
            t.description = 'Generate .9.png files from NinePatchSpec entries.'
            t.outputs.dir(ext.ninePatchDir)
            t.dependsOn(generateAtlasPixels)
            t.doLast {
                final File outDir = ext.ninePatchDir.get().asFile
                outDir.mkdirs()
                ext.ninePatches.each { NinePatchSpec spec ->
                    final File png = new File(outDir, "${spec.name}.9.png")
                    AtlasNinePatchWriter.writeSimpleNinePatch(png, spec, spec.resolveInsetPx(ext.defaultInsetPx.get()))
                    t.logger.lifecycle("Generated nine-patch ${spec.name} -> ${png}")
                }
            }
        }

        // 3 — high-level .9 (ShapeFamilySpec)
        final TaskProvider<Task> generateAtlasShapes = project.tasks.register('generateAtlasShapes') { t ->
            t.group = 'xapi-atlas'
            t.description = 'Generate .9.png files from ShapeFamilySpec families.'
            t.outputs.dir(ext.shapesDir)
            t.dependsOn(generateAtlasPixels)
            t.doLast {
                final File outDir = ext.shapesDir.get().asFile
                outDir.mkdirs()
                ext.families.each { ShapeFamilySpec fam ->
                    if (fam.states.empty) {
                        // sensible defaults if user defined no states
                        fam.state('default') {}
                        fam.state('over')    { gradient(fam.gradTopLight * 1.5f as float, fam.gradBotDark * 0.6f as float) }
                        fam.state('pressed') { gradient(fam.gradTopLight * 0.5f as float, fam.gradBotDark * 1.8f as float); padDelta(1,0,-1,0) }
                        fam.state('disabled'){ alpha 0.55f }
                    }
                    fam.states.each { StateSpec st ->
                        final String baseName = (st.name == 'default') ? fam.name : "${fam.name}-${st.name}"
                        final File png = new File(outDir, "${baseName}.9.png")
                        AtlasNinePatchWriter.writeRoundedNinePatch(png, fam, st, fam.resolveInsetPx(ext.defaultInsetPx.get()))
                        t.logger.lifecycle("Generated shape ${baseName} -> ${png.name} (${fam.width}x${fam.height}, r=${fam.radius})")
                    }
                }
            }
        }

        // 4 — pack atlas
        final TaskProvider<PackTextures> packXapiAtlas = project.tasks.register('packXapiAtlas', PackTextures) { p ->
            p.group = 'xapi-atlas'
            p.description = 'Pack generated pixels and nine-patches into a texture atlas.'
            p.dependsOn(generateAtlasPixels, generateAtlasNinePatches, generateAtlasShapes)

            p.from(ext.ninePatchDir)
            p.from(ext.pixelsDir)
            p.from(ext.shapesDir)
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
            c.dependsOn packXapiAtlas
            c.from({ ext.packedDir.get() })
            c.include "${ext.atlasName.get()}.atlas"
            c.include "${ext.atlasName.get()}*.png"   // supports multi-page atlases
            c.into(ext.outputDir)
            c.duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        // convenience: ensure packing happens on `build`
        if (project.tasks.names.contains(BUILD_TASK_NAME)) {
            project.tasks.named(BUILD_TASK_NAME).configure {
                it.dependsOn(packXapiAtlas)
            }
        }
    }
}
