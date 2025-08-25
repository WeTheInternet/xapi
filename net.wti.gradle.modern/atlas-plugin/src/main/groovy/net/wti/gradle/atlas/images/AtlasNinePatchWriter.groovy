package net.wti.gradle.atlas.images


import groovy.transform.CompileStatic
import net.wti.gradle.atlas.ext.ShapeFamilySpec
import net.wti.gradle.atlas.ext.StateSpec
import net.wti.gradle.atlas.ext.NinePatchSpec

import javax.imageio.ImageIO
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

///
/// ### AtlasNinePatchWriter
///
/// Utility for writing **.9.png** files from either:
/// - a **low-level** `NinePatchSpec` (explicit paddings & splits), or
/// - a **high-level** `ShapeFamilySpec` + `StateSpec` (rounded rectangles,
///   gradient fills, strokes, shadows, and auto markers).
///
/// The top/left border markers are **solid black** (0xFF000000).
///
/// Created by James X. Nelson (James@WeTheInter.net) and chat gpt on 20/08/2025 @ 03:23
@CompileStatic
final class AtlasNinePatchWriter {

    private static final int MARK = -0x1000000   // 0xFF000000 as signed int

    private AtlasNinePatchWriter() {}

    // ---------------------------------------------------------------------------
    // Low-level (.9 from NinePatchSpec)
    // ---------------------------------------------------------------------------

    static void writeSimpleNinePatch(File out, NinePatchSpec spec) {
        final int w = Math.max(1, spec.width)
        final int h = Math.max(1, spec.height)
        final int tw = w + 2, th = h + 2

        BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB)
        Graphics2D g = (Graphics2D) img.getGraphics()
        try {
            g.setComposite(AlphaComposite.Src)
            g.setColor(new Color(0,0,0,0)); g.fillRect(0,0,tw,th)

            // Fill
            g.setColor(new Color(parseHexToARGB(spec.fillHex), true))
            g.fillRect(1,1,w,h)

            // Stroke (optional)
            if (spec.strokeHex && spec.strokePx > 0) {
                g.setStroke(new BasicStroke(spec.strokePx as float, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER))
                g.setColor(new Color(parseHexToARGB(spec.strokeHex), true))
                g.drawRect(1,1,w,h)
            }

            // Stretch markers
            final int sx0 = clamp(spec.stretchX0!=null ? spec.stretchX0 : 0, 0, w-1)
            final int sx1 = clamp(spec.stretchX1!=null ? spec.stretchX1 : w-1, 0, w-1)
            final int sy0 = clamp(spec.stretchY0!=null ? spec.stretchY0 : 0, 0, h-1)
            final int sy1 = clamp(spec.stretchY1!=null ? spec.stretchY1 : h-1, 0, h-1)

            for (int x=1+Math.min(sx0,sx1); x<=1+Math.max(sx0,sx1); x++) img.setRGB(x, 0, MARK)
            for (int y=1+Math.min(sy0,sy1); y<=1+Math.max(sy0,sy1); y++) img.setRGB(0, y, MARK)

            // Content area from padding
            final int cx0 = clamp(spec.padLeft, 0, w-1)
            final int cy0 = clamp(spec.padTop, 0, h-1)
            final int cx1 = clamp(w-1 - Math.max(0, spec.padRight), 0, w-1)
            final int cy1 = clamp(h-1 - Math.max(0, spec.padBottom), 0, h-1)

            for (int x=1+cx0; x<=1+cx1; x++) img.setRGB(x, th-1, MARK)
            for (int y=1+cy0; y<=1+cy1; y++) img.setRGB(tw-1, y, MARK)

        } finally {
            g.dispose()
        }
        out.parentFile.mkdirs()
        ImageIO.write(img, "png", out)
    }

    // ---------------------------------------------------------------------------
    // High-level (.9 from ShapeFamilySpec + StateSpec)
    // ---------------------------------------------------------------------------

    static strictfp void writeRoundedNinePatch(File out, ShapeFamilySpec fam, StateSpec st) {
        final int w = Math.max(1, fam.width)
        final int h = Math.max(1, fam.height)
        final int tw = w + 2, th = h + 2

        BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB)
        Graphics2D g = (Graphics2D) img.getGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setComposite(AlphaComposite.Src)
            g.setColor(new Color(0,0,0,0)); g.fillRect(0,0,tw,th)

            // Shadow (soft offset)
            if (fam.shadowPx > 0) {
                final int sx = fam.shadowPx, sy = fam.shadowPx
                Shape shadow = new RoundRectangle2D.Float(1+sx, 1+sy, w, h, fam.radius*2 as float, fam.radius*2 as float)
                g.setColor(new Color(parseHexToARGB(fam.shadowHex), true))
                g.fill(shadow)
            }

            // Fill w/ vertical gradient
            final String base = st.overrideFill ?: fam.fillHex
            final float tAdj = (st.overrideTopLight != null ? st.overrideTopLight : fam.gradTopLight)
            final float bAdj = (st.overrideBotDark  != null ? st.overrideBotDark  : fam.gradBotDark)
            final Color top = new Color(adjustARGB(parseHexToARGB(base), tAdj), true)
            final Color bot = new Color(adjustARGB(parseHexToARGB(base), bAdj), true)
            final Paint grad = new GradientPaint(1, 1, top, 1, 1+h, bot)

            Shape shape = new RoundRectangle2D.Float(1, 1, w, h, fam.radius*2 as float, fam.radius*2 as float)
            g.setPaint(grad); g.fill(shape)

            // Outer stroke
            if (fam.strokePx > 0 && fam.strokeHex) {
                g.setStroke(new BasicStroke(fam.strokePx as float, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
                g.setColor(new Color(parseHexToARGB(fam.strokeHex), true))
                g.draw(shape)
            }

            // Inner stroke
            if (fam.innerStrokePx > 0 && fam.innerStrokeHex) {
                final float inset = fam.innerStrokePx
                final float rad = Math.max(0f, fam.radius*2f - 2f*inset) as float
                final float paddedInset = inset + 1 as float
                final float paddedWidth = w - 2 * inset as float
                final float paddedHeight = h - 2 * inset as float
                Shape inner = new RoundRectangle2D.Float(paddedInset, paddedInset, paddedWidth, paddedHeight, rad, rad)
                g.setStroke(new BasicStroke(fam.innerStrokePx as float, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
                g.setColor(new Color(parseHexToARGB(fam.innerStrokeHex), true))
                g.draw(inner)
            }

            // Global alpha (e.g. disabled)
            if (st.alpha != null) {
                final float mul = Math.max(0f, Math.min(1f, st.alpha))
                for (int yy=0; yy<th; yy++) for (int xx=0; xx<tw; xx++) {
                    final int argb = img.getRGB(xx, yy)
                    final int a = (argb >>> 24) & 0xff
                    final int na = (int)Math.round(a * mul)
                    img.setRGB(xx, yy, (na << 24) | (argb & 0x00ffffff))
                }
            }

            // 9-patch markers (stretch)
            final int L = Math.max(1, fam.splitLeft)
            final int R = Math.max(1, fam.splitRight)
            final int T = Math.max(1, fam.splitTop)
            final int B = Math.max(1, fam.splitBottom)

            for (int x = 1 + L; x <= w + 1 - R; x++) img.setRGB(x, 0, MARK)   // top row
            for (int y = 1 + T; y <= h + 1 - B; y++) img.setRGB(0, y, MARK)   // left col

            // 9-patch markers (content)
            int pt = fam.padTop, pl = fam.padLeft, pb = fam.padBottom, pr = fam.padRight
            if (st.dPadTop   != null) pt += st.dPadTop
            if (st.dPadLeft  != null) pl += st.dPadLeft
            if (st.dPadBottom!= null) pb += st.dPadBottom
            if (st.dPadRight != null) pr += st.dPadRight

            final int cx0 = clamp(pl, 0, w-1)
            final int cy0 = clamp(pt, 0, h-1)
            final int cx1 = clamp(w-1-pr, 0, w-1)
            final int cy1 = clamp(h-1-pb, 0, h-1)

            for (int x = 1 + cx0; x <= 1 + cx1; x++) img.setRGB(x, th-1, MARK) // bottom row
            for (int y = 1 + cy0; y <= 1 + cy1; y++) img.setRGB(tw-1, y, MARK) // right col

        } finally {
            g.dispose()
        }
        out.parentFile.mkdirs()
        ImageIO.write(img, "png", out)
    }

    // ---------------------------------------------------------------------------
    // Color helpers
    // ---------------------------------------------------------------------------

    /** Accepts '#rgb', '#rgba', '#rrggbb', '#rrggbbaa' (with or without '#'). Returns ARGB int. */
    static int parseHexToARGB(String input) {
        String s = input?.trim()
        if (!s) throw new IllegalArgumentException("Empty hex color")
        if (s.startsWith("#")) s = s.substring(1)
        if (s.length() == 3 || s.length() == 4) s = s.collect { it * 2 }.join('')
        if (s.length() == 6) s += "ff"
        if (s.length() != 8) throw new IllegalArgumentException("Bad hex '${input}' — need 3/4/6/8 hex digits")
        final long rgba = Long.parseLong(s, 16)
        final int r = (int)((rgba >> 24) & 0xff), g = (int)((rgba >> 16) & 0xff),
                  b = (int)((rgba >>  8) & 0xff), a = (int)( rgba        & 0xff)
        return ((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff)
    }

    /** Lighten/darken an ARGB color by a percentage (e.g. +0.12, -0.08). */
    static int adjustARGB(final int argb, final float pct) {
        int a = (argb >>> 24) & 0xff, r = (argb >>> 16) & 0xff, g = (argb >>> 8) & 0xff, b = argb & 0xff
        return (a << 24) | (adj(r, pct) << 16) | (adj(g, pct) << 8) | adj(b, pct)
    }

    private static int adj(final int c, final float pct) {
        return Math.max(0, Math.min(255, Math.round(c * (1f + pct))))
    }

    private static int clamp(int v, int lo, int hi) { Math.max(lo, Math.min(hi, v)) }
}
