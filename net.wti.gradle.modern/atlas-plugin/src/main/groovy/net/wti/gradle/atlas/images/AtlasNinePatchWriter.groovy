package net.wti.gradle.atlas.images

import groovy.transform.CompileStatic
import net.wti.gradle.atlas.ext.ShapeFamilySpec
import net.wti.gradle.atlas.ext.StateSpec
import net.wti.gradle.atlas.ext.NinePatchSpec

import javax.imageio.ImageIO
import java.awt.*
import java.awt.geom.Rectangle2D
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

    /** Opaque black for guide pixels. */
    private static final int MARK =  -0x1000000   // 0xFF000000 as signed int

    /** Safe, small default inset so anti-aliasing never touches the 1-px border. */
    static float DEFAULT_INSET = 0.65f

    private AtlasNinePatchWriter() {}

    // ---------------------------------------------------------------------------
    // Low-level (.9 from NinePatchSpec)
    // ---------------------------------------------------------------------------

    ///
    /// Write a rectangular 9-patch from a `NinePatchSpec`.
    ///
    /// Painting is clipped to the interior (1..w,1..h). Because we use axis-aligned
    /// fill/stroke here, an additional geometry inset is usually not necessary, but
    /// we still clip to be safe. Guides are inked last as opaque black.
    ///
    static void writeSimpleNinePatch(File out, NinePatchSpec spec, float defaultInset = DEFAULT_INSET) {
        final int w = Math.max(1, spec.width)
        final int h = Math.max(1, spec.height)
        final int tw = w + 2, th = h + 2

        final float inset = spec.resolveInsetPx(defaultInset)

        final BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB)
        final Graphics2D g = (Graphics2D) img.getGraphics()
        try {
            g.setComposite(AlphaComposite.Src)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Transparent canvas
            g.setColor(new Color(0,0,0,0))
            g.fillRect(0,0,tw,th)

            // Clip to interior; prevents AA from touching the 1-px border
            g.setClip(new Rectangle(1, 1, w, h))

            // Geometry (honor cornerRadius + optional inset)
            final float x = 1f + inset as float
            final float y = 1f + inset as float
            final float ww = Math.max(0f, w - 2f * inset) as float
            final float hh = Math.max(0f, h - 2f * inset) as float
            final boolean rounded = (spec.cornerRadius > 0)
            final float arc = rounded ? Math.max(0f, spec.cornerRadius * 2f - 2f * 0f) as float : 0f
            final Shape body = rounded
                    ? new RoundRectangle2D.Float(x, y, ww, hh, arc, arc)
                    : new Rectangle2D.Float(x, y, ww, hh)

            // Fill
            g.setColor(new Color(parseHexToARGB(spec.fillHex), true))
            if (rounded) g.fill(body) else g.fill(body.bounds2D)

            // Stroke (optional)
            if (spec.strokeHex && spec.strokePx > 0) {
                g.setStroke(new BasicStroke(spec.strokePx as float, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
                g.setColor(new Color(parseHexToARGB(spec.strokeHex), true))
                g.draw(body)
            }

            // Remove clip and sanitize border before inking guides
            g.setClip(null)
            sanitizeNinePatchBorders(img)

            // --- Stretch markers (top/left) ---
            final int sx0 = clamp(spec.stretchX0 != null ? spec.stretchX0 : 0,    0, w - 1)
            final int sx1 = clamp(spec.stretchX1 != null ? spec.stretchX1 : w - 1, 0, w - 1)
            final int sy0 = clamp(spec.stretchY0 != null ? spec.stretchY0 : 0,    0, h - 1)
            final int sy1 = clamp(spec.stretchY1 != null ? spec.stretchY1 : h - 1, 0, h - 1)

            for (int x1 = 1 + Math.min(sx0, sx1); x1 <= 1 + Math.max(sx0, sx1); x1++) img.setRGB(x1, 0, MARK)
            for (int y1 = 1 + Math.min(sy0, sy1); y1 <= 1 + Math.max(sy0, sy1); y1++) img.setRGB(0, y1, MARK)

            // --- Content markers (bottom/right) ---
            final int[] pads = spec.resolveContentPad()
            final int pt = clamp(pads[0], 0, h - 1)
            final int pl = clamp(pads[1], 0, w - 1)
            final int pb = clamp(pads[2], 0, h - 1)
            final int pr = clamp(pads[3], 0, w - 1)

            final int cx0 = clamp(pl, 0, w - 1)
            final int cy0 = clamp(pt, 0, h - 1)
            final int cx1 = clamp(w - 1 - pr, 0, w - 1)
            final int cy1 = clamp(h - 1 - pb, 0, h - 1)

            for (int x2 = 1 + cx0; x2 <= 1 + cx1; x2++) img.setRGB(x2, th - 1, MARK)
            for (int y2 = 1 + cy0; y2 <= 1 + cy1; y2++) img.setRGB(tw - 1, y2, MARK)

        } finally {
            g.dispose()
        }
        out.parentFile.mkdirs()
        ImageIO.write(img, "png", out)
    }

    // ---------------------------------------------------------------------------
    // High-level (.9 from ShapeFamilySpec + StateSpec)
    // ---------------------------------------------------------------------------

    ///
    /// Write a rounded-rectangle 9-patch from a `ShapeFamilySpec` + `StateSpec`.
    ///
    /// All painting is:
    /// 1) **clipped** to interior (1..w,1..h),
    /// 2) **inset** by `defaultInset` pixels (pass the resolved value from your DSL),
    /// 3) gradient-filled, stroked, shadowed (as configured),
    /// 4) **guide lines** inked last at full opacity.
    ///
    static strictfp void writeRoundedNinePatch(File out, ShapeFamilySpec fam, StateSpec st, float defaultInset = DEFAULT_INSET) {
        final int w = Math.max(1, fam.width)
        final int h = Math.max(1, fam.height)
        final int tw = w + 2, th = h + 2

        final float inset = fam.resolveInsetPx(defaultInset)

        final BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB)
        final Graphics2D g = (Graphics2D) img.getGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setComposite(AlphaComposite.Src)
            g.setColor(new Color(0,0,0,0))
            g.fillRect(0,0,tw,th)

            // Clip to interior so AA never touches 1-px border
            g.setClip(new Rectangle(1, 1, w, h))

            // Geometry for the painted area (already inset)
            final float x = 1f + inset as float
            final float y = 1f + inset as float
            final float ww = Math.max(0f, w - 2f * inset) as float
            final float hh = Math.max(0f, h - 2f * inset) as float
            final float arc = Math.max(0f, fam.radius * 2f - 2f * 0f) as float
            final Shape body = new RoundRectangle2D.Float(x, y, ww, hh, arc, arc)

            // Shadow (optional)
            if (fam.shadowPx > 0) {
                final int sx = fam.shadowPx, sy = fam.shadowPx
                final Shape shadow = new RoundRectangle2D.Float(x + sx as float, y + sy as float, ww, hh, arc, arc)
                g.setColor(new Color(parseHexToARGB(fam.shadowHex), true))
                g.fill(shadow)
            }

            // Fill with vertical gradient (using resolvers)
            final String baseHex = fam.resolveFillHex(st)
            final int base = parseHexToARGB(baseHex)
            final float tAdj = fam.resolveTopLight(st)
            final float bAdj = fam.resolveBotDark(st)
            final Color top = new Color(adjustARGB(base, tAdj), true)
            final Color bot = new Color(adjustARGB(base, bAdj), true)
            g.setPaint(new GradientPaint(x, y, top, x, y + hh as float, bot))
            g.fill(body)

            // Outer stroke
            if (fam.strokePx > 0 && fam.strokeHex) {
                g.setStroke(new BasicStroke(fam.strokePx as float, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
                g.setColor(new Color(parseHexToARGB(fam.strokeHex), true))
                g.draw(body)
            }

            // Inner stroke (optional)
            if (fam.innerStrokePx > 0 && fam.innerStrokeHex) {
                final float s = fam.innerStrokePx
                final float ix = x + s as float, iy = y + s as float
                final float iw = Math.max(0f, ww - 2f * s) as float, ih = Math.max(0f, hh - 2f * s) as float
                final float iArc = Math.max(0f, fam.radius * 2f - 2f * (inset + s)) as float
                final Shape inner = new RoundRectangle2D.Float(ix, iy, iw, ih, iArc, iArc)
                g.setStroke(new BasicStroke(fam.innerStrokePx as float, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
                g.setColor(new Color(parseHexToARGB(fam.innerStrokeHex), true))
                g.draw(inner)
            }

            // Global alpha (interior only, so guides remain opaque)
            if (st.alpha != null) {
                //noinspection GroovyUnusedAssignment
                final float mul = Math.max(0f, Math.min(1f, st.alpha))
                for (int yy = 1; yy <= h; yy++) for (int xx = 1; xx <= w; xx++) {
                    final int argb = img.getRGB(xx, yy)
                    final int a = (argb >>> 24) & 0xff
                    img.setRGB(xx, yy, ((int)Math.round(a * mul) << 24) | (argb & 0x00ffffff))
                }
            }

            // Remove clip and sanitize borders before inking guides
            g.setClip(null)
            sanitizeNinePatchBorders(img)

            // --- Stretch markers ---
            final int L = Math.max(1, fam.splitLeft)
            final int R = Math.max(1, fam.splitRight)
            final int T = Math.max(1, fam.splitTop)
            final int B = Math.max(1, fam.splitBottom)
            for (int px = 1 + L; px <= w + 1 - R; px++) img.setRGB(px, 0, MARK)
            for (int py = 1 + T; py <= h + 1 - B; py++) img.setRGB(0, py, MARK)

            // --- Content markers ---
            final int[] pads = fam.resolveContentPad(st) // (t,l,b,r)
            final int pt = clamp(pads[0], 0, h - 1)
            final int pl = clamp(pads[1], 0, w - 1)
            final int pb = clamp(pads[2], 0, h - 1)
            final int pr = clamp(pads[3], 0, w - 1)

            final int cx0 = clamp(pl, 0, w - 1)
            final int cy0 = clamp(pt, 0, h - 1)
            final int cx1 = clamp(w - 1 - pr, 0, w - 1)
            final int cy1 = clamp(h - 1 - pb, 0, h - 1)
            for (int px = 1 + cx0; px <= 1 + cx1; px++) img.setRGB(px, th - 1, MARK)
            for (int py = 1 + cy0; py <= 1 + cy1; py++) img.setRGB(tw - 1, py, MARK)

        } finally {
            g.dispose()
        }
        out.parentFile.mkdirs()
        ImageIO.write(img, "png", out)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    ///
    /// Ensure the entire 1-pixel border starts transparent (except the corners, which
    /// are fine to clear as well). We call this *before* re-inking guide pixels.
    ///
    private static void sanitizeNinePatchBorders(BufferedImage img) {
        final int tw = img.getWidth(), th = img.getHeight()
        final int CLEAR = 0x00000000
        for (int x = 0; x < tw; x++) { img.setRGB(x, 0, CLEAR); img.setRGB(x, th - 1, CLEAR) }
        for (int y = 0; y < th; y++) { img.setRGB(0, y, CLEAR); img.setRGB(tw - 1, y, CLEAR) }
    }


    /** Integer clamp. */
    private static int clamp(int v, int lo, int hi) { Math.max(lo, Math.min(hi, v)) }

    // ---------------------------------------------------------------------------
    // Color helpers
    // ---------------------------------------------------------------------------

    ///
    /// Accepts `#rgb`, `#rgba`, `#rrggbb`, `#rrggbbaa` (with or without `#`). Returns **ARGB** int.
    ///
    static int parseHexToARGB(String input) {
        String s = input?.trim()
        if (!s) throw new IllegalArgumentException("Empty hex color")
        if (s.startsWith("#")) s = s.substring(1)
        if (s.length() == 3 || s.length() == 4) {
            final StringBuilder b = new StringBuilder(8)
            for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); b.append(c).append(c) }
            s = b.toString()
        }
        if (s.length() == 6) s += "ff"
        if (s.length() != 8) throw new IllegalArgumentException("Bad hex '${input}' — need 3/4/6/8 hex digits")
        final long rgba = Long.parseLong(s, 16)
        final int r = (int)((rgba >> 24) & 0xff)
        final int g = (int)((rgba >> 16) & 0xff)
        final int b = (int)((rgba >>  8) & 0xff)
        final int a = (int)( rgba        & 0xff)
        // Return ARGB
        return ((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff)
    }

    ///
    /// Lighten/darken an ARGB color by a percentage (e.g. `+0.12`, `-0.08`).
    ///
    static int adjustARGB(final int argb, final float pct) {
        final int a = (argb >>> 24) & 0xff
        final int r = (argb >>> 16) & 0xff
        final int g = (argb >>>  8) & 0xff
        final int b =  argb         & 0xff
        return (a << 24) | (adj(r, pct) << 16) | (adj(g, pct) << 8) | adj(b, pct)
    }

    private static int adj(final int c, final float pct) {
        return Math.max(0, Math.min(255, Math.round(c * (1f + pct))))
    }
}
