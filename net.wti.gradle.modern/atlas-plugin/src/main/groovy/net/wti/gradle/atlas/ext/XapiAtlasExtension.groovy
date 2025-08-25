package net.wti.gradle.atlas.ext

import groovy.transform.CompileStatic
import net.wti.gradle.atlas.images.AtlasNinePatchWriter
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/// XapiAtlasExtension:
///
///
/// ### XapiAtlasExtension
///
/// Gradle extension backing the `xapiAtlas { ... }` DSL.
/// Used to configure generated libgdx atlas + png files based on xapiAtlas{} configuration.
///
/// Provides three kinds of generated sources:
///
/// 1. **pixels** — 1×1 utility pixels by name (e.g. `white`, `tooltipBg`)
/// 2. **ninePatches** — explicit specs for single nine-patches (`NinePatchSpec`)
/// 3. **shapes** — explicit specs for high level shapes (`NinePatchSpec`)
/// 4. **families** — high-level widget families (`ShapeFamilySpec`) with states
///
/// Outputs are packed by BlueBox’s `PackTextures` into a `.atlas + .png`.
///
/// #### Typical build layout
/// - 'build/xapi-atlas/pixels/'     (generated 1×1 PNGs)
/// - 'build/xapi-atlas/ninepatch/'  (generated .9.png from NinePatchSpec)
/// - 'build/xapi-atlas/shapes/'     (generated .9.png from ShapeFamilySpec)
/// - `build/xapi-atlas/packed/`     (TexturePacker output: `*.atlas` + page PNGs)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/08/2025 @ 01:24
@CompileStatic
class XapiAtlasExtension {

    // --------- Directories ---------
    final DirectoryProperty ninePatchDir
    final DirectoryProperty outputDir
    final DirectoryProperty packedDir
    final DirectoryProperty pixelsDir
    final DirectoryProperty shapesDir

    // --------- Atlas file name ---------
    final Property<String> atlasName

    // --------- Atlas configuration ---------
    final Property<Float> defaultInsetPx

    // --------- Sources ---------
    final MapProperty<String, String> pixels                      /// name → hex (#rrggbb[aa])
    final NamedDomainObjectContainer<NinePatchSpec> ninePatches   /// manual .9 specs
    final NamedDomainObjectContainer<ShapeFamilySpec> families    /// higher-level families

    XapiAtlasExtension(Project project) {
        ninePatchDir = project.objects.directoryProperty()
        ninePatchDir.convention(project.layout.buildDirectory.dir("xapi-atlas/ninepatch"))

        outputDir    = project.objects.directoryProperty()
        outputDir.convention(project.layout.buildDirectory.dir("resources/xapi/atlas"))

        packedDir    = project.objects.directoryProperty()
        packedDir.convention(project.layout.buildDirectory.dir("xapi-atlas/packed"))

        pixelsDir    = project.objects.directoryProperty()
        pixelsDir.convention(project.layout.buildDirectory.dir("xapi-atlas/pixels"))

        shapesDir    = project.objects.directoryProperty()
        shapesDir.convention(project.layout.buildDirectory.dir("xapi-atlas/shapes"))

        atlasName = project.objects.property(String).convention("xapi-atlas")

        defaultInsetPx = project.objects.property(Float)
        defaultInsetPx.convention(AtlasNinePatchWriter.DEFAULT_INSET)

        pixels = project.objects.mapProperty(String, String).convention([
                white: "#ffffffff",
                black: "#000000ff"
        ])

        ninePatches = project.container(NinePatchSpec) { final String name -> new NinePatchSpec(name) }
        families    = project.container(ShapeFamilySpec) { final String name -> new ShapeFamilySpec(name, project) }
    }

    // --------- DSL sugar ---------

    /** Define a 1×1 pixel by name and color hex. */
    void pixel(String name, String hex) { pixels.put(name, hex) }

    /** Define a raw nine-patch (low-level). */
    NinePatchSpec ninePatch(String name, @DelegatesTo(NinePatchSpec) Closure<?> cfg = {}) {
        final NinePatchSpec spec = ninePatches.maybeCreate(name)
        cfg.resolveStrategy = Closure.DELEGATE_FIRST
        cfg.delegate = spec
        cfg.call(spec)
        return spec
    }

    /** Define a shape family (high-level). */
    ShapeFamilySpec family(String name, @DelegatesTo(ShapeFamilySpec) Closure<?> cfg = {}) {
        final ShapeFamilySpec fam = families.maybeCreate(name)
        cfg.resolveStrategy = Closure.DELEGATE_FIRST
        cfg.delegate = fam
        cfg.call(fam)
        return fam
    }

    /**
     * Create a derived family by copying a base and applying overrides.
     * States are copied by name as shallow deltas.
     *
     * /// Note:
     * /// We type the state-closure parameter as `StateSpec` and invoke methods on that
     * /// explicit receiver to avoid Groovy resolving `gradient(...)` to the *owner*
     * /// (`ShapeFamilySpec.gradient(float,float)`), which would autounbox nulls and
     * /// cause NPEs. Using `it.gradient(Float,Float)` reliably targets `StateSpec`.
     */
    ShapeFamilySpec variant(String name, String from, @DelegatesTo(ShapeFamilySpec) Closure<?> cfg = {}) {
        final ShapeFamilySpec base = families.getByName(from)
        final ShapeFamilySpec v = families.maybeCreate(name)

        // copy geometry
        v.width = base.width; v.height = base.height; v.radius = base.radius
        v.splitLeft = base.splitLeft; v.splitRight = base.splitRight
        v.splitTop  = base.splitTop;  v.splitBottom = base.splitBottom
        v.padTop = base.padTop; v.padLeft = base.padLeft; v.padBottom = base.padBottom; v.padRight = base.padRight

        // copy look
        v.fillHex = base.fillHex; v.gradTopLight = base.gradTopLight; v.gradBotDark = base.gradBotDark
        v.strokeHex = base.strokeHex; v.strokePx = base.strokePx
        v.innerStrokeHex = base.innerStrokeHex; v.innerStrokePx = base.innerStrokePx
        v.shadowPx = base.shadowPx; v.shadowHex = base.shadowHex
        v.insetPx = base.insetPx

        // copy states (shallow; explicit receiver prevents owner-method overload capture)
        base.states.each { StateSpec s ->
            v.state(s.name) { StateSpec it ->
                if (s.overrideFill) it.fill(s.overrideFill)

                if (s.overrideTopLight != null || s.overrideBotDark != null) {
                    it.gradient(s.overrideTopLight, s.overrideBotDark) // StateSpec.gradient(Float, Float)
                }

                if (s.alpha != null) {
                    it.alpha(s.alpha) // guarded; no null unboxing
                }

                if (s.dPadTop!=null || s.dPadLeft!=null || s.dPadBottom!=null || s.dPadRight!=null) {
                    it.padDelta(s.dPadTop ?: 0, s.dPadLeft ?: 0, s.dPadBottom ?: 0, s.dPadRight ?: 0)
                }
            }
        }

        // apply overrides
        cfg.resolveStrategy = Closure.DELEGATE_FIRST
        cfg.delegate = v
        cfg.call(v)
        return v
    }
}
