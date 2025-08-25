package net.wti.gradle.atlas.tasks

import groovy.transform.CompileStatic
import net.wti.gradle.atlas.images.AtlasNinePatchWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

///
/// ### GenerateAtlasPixelsTask
///
/// Writes 1×1 PNG “utility pixels” (`white`, etc.) from a color map.
/// Marked cacheable; Gradle fingerprints the input map + output directory.
///
/// Usage (from your plugin):
/// ```groovy
/// tasks.register('generateAtlasPixels', GenerateAtlasPixelsTask) {
///   pixels.set(xapiAtlas.pixels)
///   outputDir.set(xapiAtlas.pixelsDir)
/// }
/// ```
///
/// Created by James X. Nelson (James@WeTheInter.net) and chat gpt on 25/08/2025 @ 06:27 CST
@CompileStatic
@CacheableTask
class GenerateAtlasPixelsTask extends DefaultTask {

    /// name → '#rrggbb[aa]'
    @Input
    final MapProperty<String, String> pixels = project.objects.mapProperty(String, String)

    @OutputDirectory
    final DirectoryProperty outputDir = project.objects.directoryProperty()

    @TaskAction
    void run() {
        final File out = outputDir.get().asFile
        out.mkdirs()
        final Map<String,String> map = pixels.get()
        if (map.isEmpty()) return
        map.each { String name, String hex ->
            final File png = new File(out, "${name}.png")
            png.parentFile.mkdirs()
            final BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            img.setRGB(0, 0, AtlasNinePatchWriter.parseHexToARGB(hex))
            ImageIO.write(img, "png", png)
            logger.info("Generated pixel: ${png}")
        }
    }
}