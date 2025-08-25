package net.wti.gradle.font.plugin

import com.github.blueboxware.gdxplugin.GdxPlugin
import com.github.blueboxware.gdxplugin.tasks.BitmapFont
import com.github.blueboxware.gdxplugin.tasks.PackTextures
import net.wti.gradle.font.ext.FontConfig
import net.wti.gradle.font.ext.XapiFontExtension
import net.wti.gradle.font.task.DownloadFontTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider

import java.nio.charset.StandardCharsets

///
/// -----------------------------------------------------------------------------
///  The root plugin class.  All heavy lifting is *lazy* thanks to Providers.
/// -----------------------------------------------------------------------------
///
class XapiFontGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.plugins.apply(GdxPlugin)

        /// 1 — Register the extension immediately (so user scripts can configure it)
        XapiFontExtension ext = project.extensions.create(
                'xapiFonts', XapiFontExtension, project)

        /// 2 — Single, lazily‑wired download task
        def downloadFonts = project.tasks.register('downloadFonts', DownloadFontTask) { t ->
            t.group = 'xapi-fonts'
            t.description = 'Download all configured Google fonts in parallel.'
            /// Defer the font list until task execution
            t.fontConfigs.set(project.providers.provider { ext.fonts.toList() })
            t.doFirst {
                t.logger.quiet("Downloading fonts ${ext.fonts.names}")
            }
        }

        /// 3 — Umbrella task that will later depend on bitmap + pack tasks
        final TaskProvider<Task> generateFonts = project.tasks.register('generateFonts') { t ->
            t.group = 'xapi-font'
            t.description = 'End‑to‑end pipeline: download → bitmap fonts → pack textures'
            t.dependsOn downloadFonts           /// always required
        }
        final TaskProvider<Delete> cleanFonts = project.tasks.register('cleanFonts', Delete) { t ->
            t.group = 'xapi-font'
            t.description = 'Clear out the generated fonts before any new font-generating task runs'
            t.delete(ext.fontOutputDir)
        }

        final Set<String> fontNames = []

        /// 4 — Respond lazily whenever the user *adds* a font to the container.
        ///     That lets us register tasks during the configuration phase without
        ///     depending on afterEvaluate{}.
        ext.fonts.configureEach { FontConfig fc ->
            /// Provider for the default characters file (may not exist yet)
            final Property<String> charFileProv = project.objects.property(String)
            charFileProv.convention( project.provider {
                if (fc.charFile) {
                    if (!fc.charFile.exists()) {
                        throw new GradleException("Configured charFile $fc.charFile does not exist!")
                    }
                    return fc.charFile.text
                }
                File charFile = ext.charFile.get().asFile
                project.logger.lifecycle("Using charFile file $charFile.absolutePath")

                if (charFile.exists()) {
                    final String txt = charFile.text
                    project.logger.quiet "Generating font charFile $charFile contents:\n$txt"
                    return txt
                }
                project.logger.lifecycle "Generating font with default common-ascii characters"
                return "ABCDEFGHIJKLMNOPQRSTUVWXYZ\nabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^\$-%+=#_&~*\u007f"
            })
            fc.addHandler {
                final String weight, final int[] sizes ->
                /// One BitmapFontTask per <family + weight>
                final int outputHeight = fc.outputHeight ?: ext.outputHeight
                final int outputWidth = fc.outputWidth ?: ext.outputWidth
                String id
                if (sizes.length > 1) {
                    id = "${fc.name}${weight ? "-$weight" : ''}"
                } else {
                    id = "${fc.name}${weight ? "-$weight" : ''}-${sizes.join('-')}"
                }
                fontNames.add("${id}.fnt")
                /// ----- lazy registration of the bitmap‑font task ---------------
                final TaskProvider<BitmapFont> bmpTask = project.tasks.register(
                        "generate${id.replaceFirst('-', '')}BitmapFont",
                        BitmapFont) { BitmapFont bt ->

                    bt.group = 'wti-fonts'
                    bt.description =
                            "Generate bitmap fonts for ${fc.name}-${weight} in ${sizes} px"

                    /// Dependencies - download .ttf fonts, delete previous output
                    bt.dependsOn downloadFonts, cleanFonts

                    /// Inputs
                    String fontName = "${fc.name}${fc.extraUrl ? URLEncoder.encode(fc.extraUrl, StandardCharsets.UTF_8.name()) : "-${weight}"}"
                    bt.inputFont = ext.fontDownloadDir.get().file("${fontName}.ttf").asFile
                    bt.inputs.property('fontConfig', fc)
                    /// Outputs
                    File output = ext.fontOutputDir.get().file("${id}.fnt").asFile
                    bt.outputFile = output

                    // Configure size of image file
                    bt.settings.setGlyphPageHeight(outputHeight)
                    bt.settings.setGlyphPageWidth(outputWidth)
                    bt.size(sizes)

                    // Extract the set of characters to include
                    bt.characters = charFileProv.get()
                }

                /// Umbrella depends on every bitmap‑font task
                generateFonts.configure { it.dependsOn bmpTask }
            }
        }

        /// 5 — Single texture packer that sweeps up *all* generated PNGs
        def packFonts = project.tasks.register('packFonts', PackTextures) { p ->
            p.group       = 'wti-fonts'
            p.description = 'Pack all generated font PNGs into one font.atlas'

            /// Will pick up outputs at execution time – no eager I/O now
            p.from(ext.fontOutputDir)
            p.destinationDir
            p.into(ext.assetOutputDir)
            p.outputs.file(ext.assetOutputDir.get().file("${ext.outputName}.png"))
            fontNames.each {
                String name ->
                    p.outputs.file(ext.assetOutputDir.get().file(name))
            }
            /// Need some extra leg-work to pull in the source .fnt files
            p.doLast {
                project.copy {
                    from ext.fontOutputDir
                    include '*.fnt'
                    into(ext.assetOutputDir)
                    filter {
                        String input ->
                            // the source bitmap font has an individual image file, which we are replacing w/ our packed file
                            // alter the .fnt file to point to this atlas instead
                            if (input.contains("file=")) {
                                return input.replaceFirst("file=[^ ]+", "atlas=${ext.outputName}.atlas file=${ext.outputName}.png")
                            }
                            return input
                    }
                }
            }

            p.packFileName = "${ext.outputName}.atlas"

            /// See [https://libgdx.com/wiki/tools/texture-packer] for details
            p.settings.filterMin = 'Linear'
            p.settings.filterMag = 'Linear'
            p.settings.limitMemory = false
            p.settings.prettyPrint = true
            p.settings.stripWhitespaceX = true
            p.settings.stripWhitespaceY = true
            p.settings.pot = false

            /// Needs every BitmapFontTask to finish first
            p.dependsOn project.tasks.withType(BitmapFont)
        }

        /// 6 — Umbrella includes the packer at the end
        generateFonts.configure { it.dependsOn packFonts }
    }
}
