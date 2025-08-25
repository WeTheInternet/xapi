package net.wti.gradle.atlas.ext

import groovy.transform.CompileStatic
import org.gradle.api.Named

///
/// ### StateSpec
///
/// Describes **visual deltas** for a single state of a shape family (e.g. `default`,
/// `over`, `pressed`, `disabled`). A state does **not** redefine the whole look; it
/// only overrides portions of the parent [`ShapeFamilySpec`](./ShapeFamilySpec.groovy).
///
/// #### What a state can change
/// - **Fill color**: replace the family’s base fill (`overrideFill`)
/// - **Gradient tweaks**: lighten/darken top and bottom independently
///   (`overrideTopLight`, `overrideBotDark`)
/// - **Opacity**: multiply the interior alpha (`alpha`)
/// - **Content padding**: adjust the family’s content insets via deltas
///   (`padDelta(t,l,b,r)`) — this shifts the **bottom/right** nine-patch markers
///
/// These deltas are applied by the writer after the family’s values:
/// - Fill: `base := state.overrideFill ?: family.fillHex`
/// - Gradient: `top := state.overrideTopLight ?: family.gradTopLight`,
///   `bot := state.overrideBotDark  ?: family.gradBotDark`
/// - Padding: `pad := family.pad + state.dPad` (each side), then clamped to the interior
/// - Alpha: if set, scales interior alpha; nine-patch guide pixels remain fully opaque
///
/// #### Ranges & conventions
/// - `alpha`: `0.0 .. 1.0` (values outside are clamped by the writer)
/// - `overrideTopLight`, `overrideBotDark`: usually `-1.0 .. +1.0` for ±100%,
///   but any float is accepted; writer clamps color channels to `[0,255]`
/// - `padDelta`: integer pixel deltas; **positive shrinks** the content area on that side
///   (e.g. `padDelta(1,0,-1,0)` nudges content **down** by 1px)
///
/// #### DSL examples
/// ```groovy
/// state('over') {
///   gradient 0.18f, -0.05f   // top light +18%, bottom dark -5%
/// }
/// state('pressed') {
///   gradient 0.06f, -0.18f
///   padDelta 1, 0, -1, 0     // nudge content down by 1px
/// }
/// state('disabled') {
///   alpha 0.55f              // 55% opacity for interior pixels
/// }
/// ```
///
/// Created by James X. Nelson (James@WeTheInter.net) and chat gpt on 25/08/2025 @ 00:04
@CompileStatic
class StateSpec implements Named {

    private final String name

    /// Optional: replace base fill color for this state (e.g. `'#3452b8ff'`).
    String overrideFill = null

    /// Optional: override top/bottom gradient modifiers (percent deltas).
    /// Positive = lighter, negative = darker. Example: `0.12f` means +12%.
    Float overrideTopLight = null
    Float overrideBotDark  = null

    /// Optional: multiply whole image alpha for **interior** pixels.
    /// Typical range `0.0 .. 1.0`; writer clamps out-of-range values.
    Float alpha = null

    /// Optional: change **content padding** by a delta (top, left, bottom, right).
    /// This affects the **bottom/right** 9-patch markers (content area).
    /// Positive values *reduce* content area on that side (push inward).
    Integer dPadTop = null, dPadLeft = null, dPadBottom = null, dPadRight = null

    StateSpec(String name) { this.name = name }

    @Override
    String getName() { name }

    // --------- DSL sugar ---------

    void fill(String hex) { this.overrideFill = hex }

    void gradient(Float topLight, Float botDark) {
        this.overrideTopLight = topLight
        this.overrideBotDark  = botDark
    }

    void alpha(float a) { this.alpha = a }

    void padDelta(int t, int l, int b, int r) {
        this.dPadTop = t; this.dPadLeft = l; this.dPadBottom = b; this.dPadRight = r
    }

    // --------- Bonus helpers (non-breaking) ---------

    /// True if this state provides a replacement fill color.
    boolean hasFillOverride() { overrideFill != null && !overrideFill.isEmpty() }

    /// True if this state overrides either gradient endpoint.
    boolean hasGradientOverride() { overrideTopLight != null || overrideBotDark != null }

    /// True if this state applies a global alpha multiplier.
    boolean hasAlpha() { alpha != null }

    /// True if any padding delta is provided.
    boolean hasPadDelta() {
        return dPadTop != null || dPadLeft != null || dPadBottom != null || dPadRight != null
    }

    /// Alpha clamped to [0,1], or the provided default if unset.
    float resolvedAlphaOr(float defaultValue) {
        if (alpha == null) return defaultValue
        return Math.max(0f, Math.min(1f, alpha))
    }

    /// Individual padding deltas with null → 0
    int deltaTop()    { dPadTop    == null ? 0 : dPadTop }
    int deltaLeft()   { dPadLeft   == null ? 0 : dPadLeft }
    int deltaBottom() { dPadBottom == null ? 0 : dPadBottom }
    int deltaRight()  { dPadRight  == null ? 0 : dPadRight }

    /// Apply this state’s padding deltas to a base (t,l,b,r) and return a new array.
    /// Note: clamping to interior bounds should be handled by the writer/family.
    int[] applyToPad(int baseTop, int baseLeft, int baseBottom, int baseRight) {
        return new int[] {
                baseTop    + deltaTop(),
                baseLeft   + deltaLeft(),
                baseBottom + deltaBottom(),
                baseRight  + deltaRight()
        }
    }
}
