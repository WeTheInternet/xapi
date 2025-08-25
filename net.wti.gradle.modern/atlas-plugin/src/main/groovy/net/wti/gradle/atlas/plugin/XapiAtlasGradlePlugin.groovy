package net.wti.gradle.atlas.plugin

import com.github.blueboxware.gdxplugin.GdxPlugin
import com.github.blueboxware.gdxplugin.tasks.PackTextures
import groovy.transform.CompileStatic
import net.wti.gradle.atlas.ext.NinePatchSpec
import net.wti.gradle.atlas.ext.XapiAtlasExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider

import javax.imageio.ImageIO
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

///
/// A plugin which is used via xapiAtlas { ... } to create tasks which generate libgdx atlases / pngs / etc
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/08/2025 @ 01:24
@CompileStatic
@SuppressWarnings('unused') // usage: plugins { id 'net.wti.gradle.atlas' }
class XapiAtlasGradlePlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {

        project.plugins.apply(GdxPlugin)
        final XapiAtlasExtension ext = project.extensions.create('xapiAtlas', XapiAtlasExtension, project)

        /// single-pixel (color pallette)
        final TaskProvider<Task> generateAtlasPixels = project.tasks.register('generateAtlasPixels') { t ->
            t.group = 'xapi-atlas'
            t.description = 'Generate 1x1 utility pixels from hex colors.'
            t.outputs.dir(ext.pixelsDir)

            t.doLast {
                File outDir = ext.pixelsDir.get().asFile
                outDir.mkdirs()

                Map<String, String> map = ext.pixels.get()
                if (map.isEmpty()) {
                    map = [white: "#ffffffff"] // fail-safe, but convention already sets this
                }

                map.each { String name, String hex ->
                    File png = new File(outDir, "${name}.png")
                    png.parentFile.mkdirs()
                    BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    int argb = parseHexToARGB(hex)
                    img.setRGB(0, 0, argb)
                    ImageIO.write(img, "png", png)
                    t.logger.lifecycle("Generated 1x1 pixel ${name} (${hex}) -> ${png}")
                }
            }
        }

        /// nine-patches
        final TaskProvider<Task> generateAtlasNinePatches = project.tasks.register('generateAtlasNinePatches') { t ->
            t.group = 'xapi-atlas'; t.description = 'Generate .9.png nine-patch images.'
            t.outputs.dir(ext.ninePatchDir)
            t.doLast {
                File outDir = ext.ninePatchDir.get().asFile; outDir.mkdirs()
                ext.ninePatches.each { final NinePatchSpec spec ->
                    File png = new File(outDir, "${spec.name}.9.png")
                    png.parentFile.mkdirs()
                    writeNinePatch(png, spec)
                    t.logger.lifecycle("Generated nine-patch ${spec.name} -> ${png}")
                }
            }
        }

        final TaskProvider<PackTextures> packXapiAtlas = project.tasks.register(
                'packXapiAtlas', PackTextures) { p ->

            p.group = 'xapi-atlas'
            p.description = 'Pack utility pixels into the xapi-atlas texture atlas.'
            p.dependsOn(generateAtlasPixels, generateAtlasNinePatches)

            // Input and output: use the same style you already use for fonts
            p.from(ext.pixelsDir)
            p.from(ext.ninePatchDir)
            p.into(ext.packedDir)

            p.packFileName = "${ext.atlasName.get()}.atlas"

            // sensible packer settings for tiny sprites
            p.settings.flattenPaths = true
            p.settings.duplicatePadding = true
            p.settings.edgePadding = true
            p.settings.filterMag = 'Linear'
            p.settings.filterMin = 'Linear'
            p.settings.fast = project.findProperty('quick') != 'false'
        }

        // Handy copier to your resources
        project.tasks.register('copyXapiAtlas', Copy) { c ->
            c.group = 'xapi-atlas'
            c.description = 'Copy xapi-atlas generated files into resources directory.'

            c.dependsOn packXapiAtlas

            c.from({ ext.packedDir.get() })            // copy from the directory, not outputs
            c.include "${ext.atlasName.get()}.atlas"
            c.include "${ext.atlasName.get()}*.png"   // handles multi-page atlases (_0.png, _1.png, …)

            c.into(ext.outputDir)
            c.dependsOn(packXapiAtlas)
            c.duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        // (Optional) hook into the build lifecycle like you do for fonts:
        project.tasks.named('build').configure { it.dependsOn(packXapiAtlas) }
    }

    /**
     * Accepts "#rgb", "#rgba", "#rrggbb", "#rrggbbaa" (with or without '#').
     * Returns ARGB int for BufferedImage.setRGB().
     */
    static int parseHexToARGB(String input) {
        String s = input?.trim()
        if (!s) throw new GradleException("Empty hex color")
        if (s.startsWith("#")) s = s.substring(1)
        if (s.size() == 3 || s.size() == 4) {
            // expand short hex
            s = s.collect { it * 2 }.join('')
        }
        if (s.size() == 6) {
            s = s + "ff"  // add opaque alpha
        }
        if (s.size() != 8) {
            throw new GradleException("Bad hex color '${input}' — expected 3/4/6/8 hex digits (with optional #)")
        }
        int rgba = (int) Long.parseLong(s, 16)
        int r = (rgba >> 24) & 0xff
        int g = (rgba >> 16) & 0xff
        int b = (rgba >> 8)  & 0xff
        int a = (rgba)       & 0xff
        // BufferedImage expects ARGB:
        return ((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff)
    }


    /**
     * Write a .9.png for the given spec.
     * Image size includes 1px border; interior is (width x height).
     */
    static void writeNinePatch(File out, NinePatchSpec spec) {
        int w = Math.max(1, spec.width)
        int h = Math.max(1, spec.height)
        int tw = w + 2, th = h + 2  // total size incl. border

        BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB)
        Graphics2D g = (Graphics2D) img.getGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Transparent everywhere
            g.setComposite(AlphaComposite.Src)
            g.setColor(new Color(0,0,0,0))
            g.fillRect(0, 0, tw, th)

            // Fill interior (rounded or rect)
            final Shape shape
            if (spec.cornerRadius > 0) {
                shape = new RoundRectangle2D.Float(1, 1, w, h, spec.cornerRadius * 2 as float, spec.cornerRadius * 2 as float)
            } else {
                shape = new Rectangle(1, 1, w, h)
            }
            g.setColor(new Color(parseHexToARGB(spec.fillHex), true))
            g.fill(shape)

            // Optional stroke
            if (spec.strokeHex && spec.strokePx > 0) {
                g.setStroke(new BasicStroke(spec.strokePx as float, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
                g.setColor(new Color(parseHexToARGB(spec.strokeHex), true))
                g.draw(shape)
            }

            // Marker color: solid black (required by 9patch)
            final int MARK = 0xFF << 24 // == 0xFF000000 as a signed int

            // Auto stretch bands if unspecified (avoid corners)
            int sx0 = (spec.stretchX0 != null) ? spec.stretchX0 : Math.min(w - 1, Math.max(0, spec.cornerRadius))
            int sx1 = (spec.stretchX1 != null) ? spec.stretchX1 : Math.max(0, w - 1 - spec.cornerRadius)
            int sy0 = (spec.stretchY0 != null) ? spec.stretchY0 : Math.min(h - 1, Math.max(0, spec.cornerRadius))
            int sy1 = (spec.stretchY1 != null) ? spec.stretchY1 : Math.max(0, h - 1 - spec.cornerRadius)

            // Clamp
            sx0 = Math.max(0, Math.min(sx0, w - 1)); sx1 = Math.max(0, Math.min(sx1, w - 1))
            sy0 = Math.max(0, Math.min(sy0, h - 1)); sy1 = Math.max(0, Math.min(sy1, h - 1))
            if (sx1 < sx0) { int t = sx0; sx0 = sx1; sx1 = t }
            if (sy1 < sy0) { int t = sy0; sy0 = sy1; sy1 = t }

            // Stretch markers: top row (x: 1+sx0..1+sx1, y:0) and left col (y:1+sy0..1+sy1, x:0)
            for (int x = 1 + sx0; x <= 1 + sx1; x++) img.setRGB(x, 0, MARK)
            for (int y = 1 + sy0; y <= 1 + sy1; y++) img.setRGB(0, y, MARK)

            // Content area markers: bottom row and right col
            // Content rectangle from insets:
            int cx0 = Math.max(0, spec.padLeft)
            int cy0 = Math.max(0, spec.padTop)
            int cx1 = Math.max(cx0, w - 1 - Math.max(0, spec.padRight))
            int cy1 = Math.max(cy0, h - 1 - Math.max(0, spec.padBottom))

            for (int x = 1 + cx0; x <= 1 + cx1; x++) img.setRGB(x, th - 1, MARK) // bottom
            for (int y = 1 + cy0; y <= 1 + cy1; y++) img.setRGB(tw - 1, y, MARK) // right

        } finally {
            g.dispose()
        }
        out.withOutputStream { os -> ImageIO.write(img, "png", os) }
    }
}
