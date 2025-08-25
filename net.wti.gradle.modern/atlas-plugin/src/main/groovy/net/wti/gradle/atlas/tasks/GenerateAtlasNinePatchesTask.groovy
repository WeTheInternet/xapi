package net.wti.gradle.atlas.tasks

import groovy.transform.CompileStatic
import net.wti.gradle.atlas.ext.NinePatchSpec
import net.wti.gradle.atlas.images.AtlasNinePatchWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

///
/// ### GenerateAtlasNinePatchesTask
///
/// Emits **.9.png** files from `NinePatchSpec` objects (low-level).
/// - Cacheable via `@Nested` specs + `@OutputDirectory`.
/// - Optional `defaultInsetPx` lets you push content markers in a bit (for AA).
///
/// Plugin wiring:
/// ```groovy
/// tasks.register('generateAtlasNinePatches', GenerateAtlasNinePatchesTask) {
///   // IMPORTANT: pass a SNAPSHOT LIST, not the container itself
///   ninePatches.set(project.providers.provider { xapiAtlas.ninePatches.toList() })
///   outputDir.set(xapiAtlas.ninePatchDir)
///   // optional
///   // defaultInsetPx.set(0.65f)
/// }
/// ```
///
/// Created by James X. Nelson (James@WeTheInter.net) and chat gpt on 25/08/2025 @ 06:27 CST
@CompileStatic
@CacheableTask
class GenerateAtlasNinePatchesTask extends DefaultTask {

    /// Snapshot of specs. Each spec is a nested input bean.
    @Nested
    final ListProperty<NinePatchSpec> ninePatches = project.objects.listProperty(NinePatchSpec)

    /// Optional default content inset (px). If absent, writer’s default is used.
    @Input @Optional
    final Property<Float> defaultInsetPx = project.objects.property(Float)

    @OutputDirectory
    final DirectoryProperty outputDir = project.objects.directoryProperty()

    @TaskAction
    void run() {
        final File out = outputDir.get().asFile
        out.mkdirs()
        final Float inset = defaultInsetPx.orNull
        ninePatches.get().each { NinePatchSpec spec ->
            final File png = new File(out, "${spec.name}.9.png")
            png.parentFile.mkdirs()
            if (inset == null) {
                AtlasNinePatchWriter.writeSimpleNinePatch(png, spec)
            } else {
                AtlasNinePatchWriter.writeSimpleNinePatch(png, spec, inset)
            }
            logger.info("Nine-patch: ${png.name}")
        }
    }
}