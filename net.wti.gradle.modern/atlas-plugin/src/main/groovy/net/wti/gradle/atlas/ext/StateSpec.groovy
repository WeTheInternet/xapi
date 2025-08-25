package net.wti.gradle.atlas.ext

import groovy.transform.CompileStatic
import org.gradle.api.Named

///
/// ### StateSpec
///
/// Describes **visual deltas** for a single state of a shape family (e.g. `default`,
/// `over`, `pressed`, `disabled`). StateSpec does **not** redefine the whole look;
/// it selectively overrides bits of the parent `ShapeFamilySpec`.
///
/// #### Typical uses
/// - Tweak gradient intensity for `over`
/// - Darken + nudge content area for `pressed`
/// - Lower alpha for `disabled`
///
/// #### DSL helpers
/// ```groovy
/// state('over') {
///   gradient 0.18f, -0.05f   // top light +18%, bottom dark -5%
/// }
/// state('pressed') {
///   gradient 0.06f, -0.18f
///   padDelta 1, 0, -1, 0     // nudge content down by 1px
/// }
/// state('disabled') { alpha 0.55f }
///
/// Created by James X. Nelson (James@WeTheInter.net) and chat gpt on 25/08/2025 @ 00:04
/// ```
@CompileStatic
class StateSpec implements Named {

    private final String name

    /// Optional: replace base fill color for this state (e.g. `'#3452b8ff'`).
    String overrideFill = null

    /// Optional: override top/bottom gradient modifiers (percent deltas).
    /// Positive = lighter, negative = darker. Example: `0.12f` means +12%.
    Float overrideTopLight = null
    Float overrideBotDark  = null

    /// Optional: multiply whole image alpha (e.g. `0.6f` → 60% opacity).
    Float alpha = null

    /// Optional: change **content padding** by a delta (top, left, bottom, right).
    /// This affects the **bottom/right** 9-patch markers (content area).
    Integer dPadTop = null, dPadLeft = null, dPadBottom = null, dPadRight = null

    StateSpec(String name) { this.name = name }

    @Override
    String getName() { name }

    // --------- DSL sugar ---------

    void fill(String hex) { this.overrideFill = hex }
    void gradient(Float topLight, Float botDark) {
        this.overrideTopLight = topLight; this.overrideBotDark = botDark
    }
    void alpha(float a) { this.alpha = a }
    void padDelta(int t, int l, int b, int r) {
        this.dPadTop = t; this.dPadLeft = l; this.dPadBottom = b; this.dPadRight = r
    }
}