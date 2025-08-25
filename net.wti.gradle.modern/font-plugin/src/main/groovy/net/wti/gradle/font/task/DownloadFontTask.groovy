package net.wti.gradle.font.task

import de.undercouch.gradle.tasks.download.DownloadAction
import net.wti.gradle.font.ext.FontConfig
import net.wti.gradle.font.ext.XapiFontExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets


///-----------------------------------------------------------------------------
/// Task that downloads *all* requested .ttf files in **parallel**.
/// Everything is wired via Providers so configuration stays lazy.
///-----------------------------------------------------------------------------
class DownloadFontTask extends DefaultTask {

    /// The fonts to fetch (injected by plugin via a ListProperty provider)
    @Input
    final ListProperty<FontConfig> fontConfigs = project.objects.listProperty(FontConfig)

    /// Destination directory (declared output so Gradle tracks caching)
    @OutputDirectory
    final DirectoryProperty outputDir = project.objects.directoryProperty()
            .value(project.extensions.findByType(XapiFontExtension).fontDownloadDir)

    @TaskAction
    void run() {

        /// Build the list of URLs *lazily* at execution time.
        List<Object> urls = fontConfigs.get().collectMany { FontConfig fc ->

            fc.weights.collect { w ->
                String v = "https://raw.githubusercontent.com/google/fonts/main/ofl/" +
                        "${fc.name.toLowerCase()}/${fc.name}${fc.extraUrl ? URLEncoder.encode(fc.extraUrl, StandardCharsets.UTF_8.name()) :"-$w"}.ttf"
                logger.info "⇒ Downloading $v to ${outputDir.get().asFile} due to $fc"
                return v
            }
        }

        logger.lifecycle "⇒ Downloading ${urls.size()} TTF file(s) to ${outputDir.get().asFile}"

        /// Use gradle-download‑task for robust, parallel GETs
        def dl = new DownloadAction(project)
        dl.src(urls)
        dl.dest(outputDir.get().asFile)
        dl.onlyIfModified(true)
        dl.execute()
    }
}