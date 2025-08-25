package net.wti.gradle.atlas.ext

import org.gradle.api.Named
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

import java.util.function.BiConsumer

/// FontConfig:
///
/// -----------------------------------------------------------------------------
///  Model object holding per‑font data; everything is an @Input so tasks can
///  attach to them as Providers and remain up‑to‑date.
/// -----------------------------------------------------------------------------
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/08/2025 @ 01:26
class AtlasConfig implements Named, Serializable {

    transient BiConsumer<String, int[]> callback

    AtlasConfig(String name) {
        this.name = name
    }

    /// Human‑readable Google Font family name (e.g. “Roboto”)
    @Input
    String name

    /// An extra token to attach to download url; "[wdth,wght]" must be uri encoded
    @Input
    String extraUrl

    /// List of style/weight suffixes that exist on Google Fonts
    @Input Set<String> weights = new LinkedHashSet<>()

    /// Optional override of characters file (defaults to $buildDir/all‑i18n.txt)
    @Input File charFile

    /// The output height of image to generate for this BitmapFont
    @Input Integer outputHeight

    /// The output width of image to generate for this BitmapFont
    @Input Integer outputWidth

    void addHandler(final BiConsumer<String, int[]> callback) {
        this.callback = {
            final String weight, final int[] sizes ->
                weights.add(weight)
                callback.accept(weight, sizes)
        }
    }

    void addWeight(String weight, int ... sizes) {
        callback.accept(weight, sizes)
    }

    void addSizedWeight(int outputWidth, int outputHeight, String weight, int size) {
        Integer oldHeight = this.outputHeight
        Integer oldWidth  = this.outputWidth
        this.outputHeight = outputHeight
        this.outputWidth  = outputWidth
        callback.accept(weight, new int[]{size})
        this.outputHeight = oldHeight
        this.outputWidth  = oldWidth
    }


    @Override
    public String toString() {
        return "FontConfig{" +
                "name='" + name + '\'' +
                ", extraUrl='" + extraUrl + '\'' +
                ", outputHeight=" + outputHeight +
                ", outputWidth=" + outputWidth +
                '}';
    }
}
