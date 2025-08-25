package net.wti.gradle.atlas.ext

import groovy.transform.CompileStatic
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.NamedDomainObjectContainer

///
/// ### ShapeFamilySpec
///
/// Declarative spec for generating *families* of UI widgets as **.9.png**
/// (nine-patch) images: buttons, tabs, select boxes, text fields, etc.
/// Each family defines the **base interior size**, corner radius, 9-slice
/// **split** and **content padding**, plus a default look (fill, gradient,
/// strokes, shadow). Individual `StateSpec`s apply deltas.
///
/// #### Coordinates & sizes
/// - **Interior** width/height exclude the 1px nine-patch border.
/// - **split(l,r,t,b)** defines the *non-stretch* margins in interior pixels.
///   The generator marks the center band(s) as stretchable (top/left markers).
/// - **pad(t,l,b,r)** defines content insets → bottom/right markers.
/// - **corner(r)** sets rounded-corner radius (interior pixels).
///
/// #### Example
/// ```groovy
/// family('button') {
///   size 52, 50
///   corner 8
///   split 8, 8, 7, 8
///   pad 4, 4, 5, 4
///   fill '#2c2f3aff'
///   gradient 0.12f, -0.08f
///   stroke '#00000066', 1
///   innerStroke '#ffffff10', 1
///   shadow 1, '#00000040'
///
///   state('default') {}
///   state('over')    { gradient 0.18f, -0.05f }
///   state('pressed') { gradient 0.06f, -0.18f; padDelta 1,0,-1,0 }
///   state('disabled'){ alpha 0.55f }
/// }
/// ```
///
/// See `XapiAtlasExtension.family(..)` and `XapiAtlasExtension.variant(..)`.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/08/2025 @ 03:19
@CompileStatic
class ShapeFamilySpec implements Named {

    private final String name

    // ---------- Geometry ----------
    int width  = 16          /// interior width (excludes 1px border)
    int height = 16          /// interior height (excludes 1px border)
    int radius = 8           /// rounded corner radius

    // ---------- Nine-slice (non-stretch margins) ----------
    int splitLeft = 6, splitRight = 6, splitTop = 6, splitBottom = 6

    // ---------- Content padding (bottom/right markers) ----------
    int padTop = 0, padLeft = 0, padBottom = 0, padRight = 0

    // ---------- Look (base) ----------
    String fillHex = "#2b2e3aff"     /// base fill color
    float  gradTopLight = 0.12f       /// +% to lighten top of gradient
    float  gradBotDark  = -0.08f      /// -% to darken bottom of gradient
    String strokeHex = "#00000066"    /// outer stroke color (nullable)
    int    strokePx  = 1              /// outer stroke thickness

    String innerStrokeHex = null      /// optional inner stroke color
    int    innerStrokePx  = 0         /// inner stroke thickness (0 disables)

    int    shadowPx = 0               /// soft offset shadow (px)
    String shadowHex = "#00000040"    /// shadow color

    /// Named states (default/over/pressed/disabled...)
    final NamedDomainObjectContainer<StateSpec> states

    /// Optional inset (px) applied when drawing family shapes.
    /// Use a negative value to mean "use global default".
    float insetPx = -1f

    ShapeFamilySpec(String name, Project project) {
        this.name = name
        this.states = project.container(StateSpec) { final String state -> new StateSpec(state) }
    }

    @Override
    String getName() { name }

    // ---------- DSL sugar ----------

    /// Set inset (px) for this family.
    void inset(float px) { this.insetPx = px }

    void size(int w, int h) { this.width = w; this.height = h }
    void corner(int r) { this.radius = r }

    void split(int l, int r, int t, int b) {
        this.splitLeft = l; this.splitRight = r; this.splitTop = t; this.splitBottom = b
    }
    void pad(int t, int l, int b, int r) {
        this.padTop = t; this.padLeft = l; this.padBottom = b; this.padRight = r
    }

    void fill(String hex) { this.fillHex = hex }
    void gradient(float topLight, float botDark) { this.gradTopLight = topLight; this.gradBotDark = botDark }
    void stroke(String hex, int px = 1) { this.strokeHex = hex; this.strokePx = px }
    void innerStroke(String hex, int px = 1) { this.innerStrokeHex = hex; this.innerStrokePx = px }
    void shadow(int px, String hex = "#00000040") { this.shadowPx = px; this.shadowHex = hex }

    StateSpec state(String name, @DelegatesTo(StateSpec) Closure<?> cfg = {}) {
        final StateSpec s = states.maybeCreate(name)
        cfg.resolveStrategy = Closure.DELEGATE_FIRST
        cfg.delegate = s
        cfg.call(s)
        return s
    }

    /// Resolve the inset (in pixels) for this family.
    /// A non-negative family-level inset overrides; otherwise use the provided global default,
    /// and if that is null, fall back to 0.65f.
    float resolveInsetPx(@SuppressWarnings('GrMethodMayBeStatic') Float globalDefault) {
        if (insetPx >= 0f) return insetPx
        final float base = (globalDefault != null ? globalDefault : 0.65f)
        return Math.max(0f, base)
    }

    /// Resolve the base fill hex for a given state (state override > family fill).
    String resolveFillHex(StateSpec st) {
        return (st.overrideFill != null ? st.overrideFill : fillHex)
    }

    /// Resolve gradient lightening at the top for a given state.
    float resolveTopLight(StateSpec st) {
        return (st.overrideTopLight != null ? st.overrideTopLight : gradTopLight)
    }

    /// Resolve gradient darkening at the bottom for a given state.
    float resolveBotDark(StateSpec st) {
        return (st.overrideBotDark != null ? st.overrideBotDark : gradBotDark)
    }

    /// Resolve content padding for a given state (applies state deltas).
    /// Returns (top, left, bottom, right) as an int[4].
    int[] resolveContentPad(StateSpec st) {
        int t = padTop, l = padLeft, b = padBottom, r = padRight
        if (st.dPadTop    != null) t += st.dPadTop
        if (st.dPadLeft   != null) l += st.dPadLeft
        if (st.dPadBottom != null) b += st.dPadBottom
        if (st.dPadRight  != null) r += st.dPadRight
        return [t, l, b, r] as int[]
    }
}