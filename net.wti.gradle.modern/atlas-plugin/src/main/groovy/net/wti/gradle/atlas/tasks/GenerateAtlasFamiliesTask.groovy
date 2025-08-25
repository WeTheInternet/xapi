package net.wti.gradle.atlas.tasks

import groovy.transform.CompileStatic
import net.wti.gradle.atlas.ext.ShapeFamilySpec
import net.wti.gradle.atlas.ext.StateSpec
import net.wti.gradle.atlas.images.AtlasNinePatchWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

///
/// ### GenerateAtlasFamiliesTask
///
/// Produces **.9.png** assets from higher-level `ShapeFamilySpec` + states.
/// - Cacheable: families are `@Nested`, each `ShapeFamilySpec` exposes nested `StateSpec`s.
/// - Each state outputs `<familyName>.9.png` or `<familyName>-<state>.9.png`.
///
/// Plugin wiring:
/// ```groovy
/// tasks.register('generateAtlasFamilies', GenerateAtlasFamiliesTask) {
///   families.set(project.providers.provider { xapiAtlas.families.toList() })
///   outputDir.set(xapiAtlas.familiesDir)
///   // optional
///   // defaultInsetPx.set(0.65f)
/// }
/// ```
///
/// Created by James X. Nelson (James@WeTheInter.net) and chat gpt on 25/08/2025 @ 06:27 CST
@CompileStatic
@CacheableTask
class GenerateAtlasFamiliesTask extends DefaultTask {

    /// Snapshot of families. Each family is a nested bean, with nested states.
    @Nested
    final ListProperty<ShapeFamilySpec> families = project.objects.listProperty(ShapeFamilySpec)

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

        families.get().each { ShapeFamilySpec fam ->
            // ensure at least a default state exists (non-mutating naming)
            final Iterable<StateSpec> states = fam.getStatesSnapshotOrDefault()
            states.each { StateSpec st ->
                final String baseName = st.name == 'default' ? fam.name : "${fam.name}-${st.name}"
                final File png = new File(out, "${baseName}.9.png")
                png.parentFile.mkdirs()
                if (inset == null) {
                    AtlasNinePatchWriter.writeRoundedNinePatch(png, fam, st)
                } else {
                    AtlasNinePatchWriter.writeRoundedNinePatch(png, fam, st, inset)
                }
                logger.info("Family: ${png.name}")
            }
        }
    }
}