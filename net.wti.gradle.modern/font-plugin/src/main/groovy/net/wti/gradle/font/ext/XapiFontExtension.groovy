package net.wti.gradle.font.ext


import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input

/// XapiFontExtension:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 17/04/2025 @ 17:21
class XapiFontExtension {

    /// 1 - Container of FontConfig objects; each one is keyed by Google‑font name.
    final NamedDomainObjectContainer<FontConfig> fonts
    final Provider<Directory> fontDownloadDir
    final Provider<Directory> fontOutputDir
    final Provider<Directory> assetOutputDir
    final RegularFileProperty charFile

    int outputWidth = 1024
    int outputHeight = 1024
    String outputName = 'font'

    XapiFontExtension(Project project) {
        fonts = project.container(FontConfig)
        fontDownloadDir = project.layout.buildDirectory.dir("downloaded-fonts")
        fontOutputDir = project.layout.buildDirectory.dir("generated-fonts")
        assetOutputDir = project.layout.buildDirectory.dir("generated-assets")
        charFile = project.objects.fileProperty()
        charFile.convention(project.layout.buildDirectory.file("all-chars.txt"))
    }

    /// 2 - Convenience DSL sugar that mirrors the usage example:
    ///     xapiFonts {
    ///         googleFont 'Roboto', { addWeight 'Regular', 12, 18 }
    ///     }
    void googleFont(String family, Closure<?> cfg = {}) {
        Closure<?> config = { FontConfig fc ->
            fc.name = family
            cfg.delegate        = fc
            //noinspection UnnecessaryQualifiedReference
            cfg.resolveStrategy = Closure.DELEGATE_FIRST
            cfg(fc)
            return fc
        }
        if (fonts.names.contains(family)) {
            // re-adding a font to get separate configuration options
            config(fonts.named(family, FontConfig).get())
        } else {
            // creating font for the first time
            fonts.create(family, config)
        }
    }

}
