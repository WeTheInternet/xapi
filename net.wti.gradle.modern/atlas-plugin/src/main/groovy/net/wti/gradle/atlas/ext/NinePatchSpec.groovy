package net.wti.gradle.atlas.ext

import org.gradle.api.Named

///
/// Declarative spec for generating a .9.png.
/// All coordinates/sizes are in interior pixels (excluding the 1px ninepatch border).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/08/2025 @ 02:51
class NinePatchSpec implements Named {
    final String name

    // Interior size (content area not counting the 1px border around)
    int width  = 16
    int height = 16

    // Fill color (background) and optional stroke
    String fillHex   = "#2a2a2acc"  // ARGB or RGB (RGB => alpha=ff)
    String strokeHex = null         // optional; null=no stroke
    int    strokePx  = 0            // 0 disables stroke

    // Optional rounded corner radius (in interior pixels)
    int cornerRadius = 0            // 0 => rectangle

    // Stretchable area markers (top/left) as inclusive ranges in interior coords.
    // If left null, we auto-choose a safe center band (inside the corners).
    Integer stretchX0 = null, stretchX1 = null
    Integer stretchY0 = null, stretchY1 = null

    // Content insets -> markers (bottom/right). Defaults typical for small tooltips.
    int padTop = 6, padLeft = 8, padBottom = 6, padRight = 8

    /// Optional inset for this *single* nine-patch (px).
    /// If null, the global xapiAtlas.defaultInsetPx (or writer fallback) is used.
    Float insetPx

    NinePatchSpec(String name) { this.name = name }

    @Override String getName() { name }

    // ===== DSL sugar =====
    void size(int w, int h) { this.width = w; this.height = h }
    void fill(String hex)   { this.fillHex = hex }
    void stroke(String hex, int px = 1) { this.strokeHex = hex; this.strokePx = px }
    void corner(int radius) { this.cornerRadius = radius }

    void stretchAll() {
        this.stretchX0 = 0; this.stretchX1 = Math.max(0, width  - 1)
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
        final float base = (globalDefault != null ? globalDefault : 0.65f)
        return (insetPx != null ? Math.max(0f, insetPx) : Math.max(0f, base))
    }

    /// Return (top, left, bottom, right) content padding as an int[4].
    int[] resolveContentPad() {
        return [padTop, padLeft, padBottom, padRight] as int[]
    }
}
