package net.wti.gradle.atlas.ext

import net.wti.gradle.atlas.images.AtlasNinePatchWriter
import org.gradle.api.Named
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

///
/// Declarative spec for generating a .9.png.
/// All coordinates/sizes are in interior pixels (excluding the 1px ninepatch border).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/08/2025 @ 02:51
class NinePatchSpec implements Named {

    @Input
    final String name

    // Interior size
    @Input int width  = 16
    @Input int height = 16

    // Fill color and optional stroke
    @Input String fillHex   = "#2a2a2acc"
    @Input @Optional String strokeHex = null
    @Input int    strokePx  = 0

    // Optional rounded corner radius
    @Input int cornerRadius = 0

    // Stretchable area markers (inclusive ranges). Null => auto-choose.
    @Input @Optional Integer stretchX0 = null
    @Input @Optional Integer stretchX1 = null
    @Input @Optional Integer stretchY0 = null
    @Input @Optional Integer stretchY1 = null

    // Content insets -> markers (bottom/right).
    @Input int padTop = 6
    @Input int padLeft = 8
    @Input int padBottom = 6
    @Input int padRight = 8

    /// Optional inset for this single nine-patch (px). Null => use global default.
    @Input @Optional
    Float insetPx = null

    NinePatchSpec(String name) { this.name = name }

    // ===== DSL sugar =====
    void size(int w, int h) { this.width = w; this.height = h }

    void fill(String hex) { this.fillHex = hex }

    void stroke(String hex, int px = 1) { this.strokeHex = hex; this.strokePx = px }

    void corner(int radius) { this.cornerRadius = radius }

    void stretchAll() {
        this.stretchX0 = 0; this.stretchX1 = Math.max(0, width - 1)
        this.stretchY0 = 0; this.stretchY1 = Math.max(0, height - 1)
    }

    void stretch(int x0, int x1, int y0, int y1) {
        this.stretchX0 = x0; this.stretchX1 = x1
        this.stretchY0 = y0; this.stretchY1 = y1
    }

    void stretchX(int x0, int x1) { this.stretchX0 = x0; this.stretchX1 = x1 }

    void stretchY(int y0, int y1) { this.stretchY0 = y0; this.stretchY1 = y1 }

    void contentPad(int top, int left, int bottom, int right) {
        this.padTop = top; this.padLeft = left; this.padBottom = bottom; this.padRight = right
    }

    /// Set inset for this spec (px).
    void inset(float px) { this.insetPx = px }

    /// Resolve the inset (in pixels) for this single nine-patch.
    /// If this spec has an explicit inset, that always wins.
    /// Otherwise, use the passed-in global default; if null, fall back to 0.65f.
    float resolveInsetPx(@SuppressWarnings('GrMethodMayBeStatic') Float globalDefault) {
        final float base = (globalDefault != null ? globalDefault : AtlasNinePatchWriter.DEFAULT_INSET)
        return (insetPx != null ? Math.max(0f, insetPx) : Math.max(0f, base))
    }

    /// Return (top, left, bottom, right) content padding as an int[4].
    int[] resolveContentPad() {
        return [padTop, padLeft, padBottom, padRight] as int[]
    }

}
