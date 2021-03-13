package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.*;
import net.wti.gradle.internal.require.api.ArchiveRequest.ArchiveRequestType;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.schema.api.PlatformConfig;
import net.wti.gradle.schema.api.SchemaDependency;
import net.wti.gradle.schema.plugin.XapiSchemaPlugin;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.util.GUtil;
import xapi.gradle.fu.LazyString;

import java.util.*;
import java.util.stream.Collectors;

import static net.wti.gradle.system.tools.GradleCoerce.unwrapBoolean;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 1:45 PM.
 */
public class DefaultArchiveConfig implements ArchiveConfigInternal {

    private final String name;
    private final PlatformConfigInternal platform;
    private final SetProperty<LazyString> requires;
    private final ProjectView view;
    private final Property<Boolean> publish;
    private Object sourceAllowed;
    private Object test;
    private Object published;

    public DefaultArchiveConfig(
        PlatformConfigInternal platform,
        ProjectView view,
        String name
    ) {
        this.name = name;
        this.platform = platform;
        this.view = view;
        publish = view.getObjects().property(Boolean.class);
        publish.convention(view.lazyProvider(()->
            published == null ? view.getSchema().shouldPublish(this) : unwrapBoolean(published)
        ));
        requires = view.getObjects().setProperty(LazyString.class);
        requires.convention(view.lazyProvider(()->{
            // Nothing set?  apply convention.
            List<LazyString> result = new ArrayList<>();
            switch (name) {
                case "main":
                    result.add(new LazyString("api"));
                    result.add(new LazyString("spi"));
                    break;
                case "mainSource":
                    result.add(new LazyString("apiSource"));
                    result.add(new LazyString("spiSource"));
                    break;
                case "stub":
                    result.add(PlatformModule.defaultModule);
                    break;
                case "stubSource":
                    result.add(new LazyString("mainSource"));
                    break;
                case "impl":
                    result.add(PlatformModule.defaultModule);
                    result.add(new LazyString("stub*"));
                    break;
                case "implSource":
                    result.add(new LazyString("mainSource"));
                    result.add(new LazyString("stubSource*"));
                    break;
            }
            return result;
        }));
        sourceAllowed = true;

    }

    @Override
    public void require(Object ... units) {
        if (units != null && units.length > 0) {
            requires.addAll(
                // TODO: wire in some pre-built version of xapi-fu
                Arrays.stream(units)
                    .map(GradleCoerce::unwrapStrings)
                    .flatMap(Collection::stream)
                    .map(LazyString::new)
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public ArchiveRequest request(
            SchemaDependency dep, ArchiveRequestType type
    ) {
        ConsumerConfiguration consumer = ()->findGraph(view.getProjectGraph());
        final ProducerConfiguration producer;

        switch (dep.getType()) {
            case unknown:
                // unknown should either be an error, or something that can call helper methods trying all three below:
                // for now, fallthrough to project: below
            case project:
                // a project producer is some other project in our local build.
                ProjectGraph producerProject = view.getBuildGraph().getProject(dep.getName());
                PlatformGraph producerPlatform = producerProject.platform(dep.getPlatformOrDefault());
                ArchiveGraph producerModule = producerPlatform.archive(dep.getModuleOrDefault());
                producer = ()->producerModule;
                break;
            case internal:
                // an internal producer is same project, pointing at some other module.
                producerProject = view.getProjectGraph();
                producerPlatform = producerProject.platform(dep.getCoords().getPlatform());
                producerModule = producerPlatform.archive(dep.getCoords().getModule());
                producer = ()->producerModule;
                break;
            case external:
                // an external producer will be translated to an included build, or downloaded using a local configuration
                // TODO: finish the indexer, and then have a way to check said index, to decide what to do here...
                // Perhaps an ExternalProducerConfiguration would be in order?
                producer = new ExternalProducerConfiguration(dep);
                break;
            default:
                throw new UnsupportedOperationException(dep.getType() + " not supported for ( " + dep.getCoords() + " )" + " " + dep.getName());
        }
        DefaultArchiveRequest req = new DefaultArchiveRequest(producer, consumer, ArchiveRequestType.COMPILE);
        consumer.getConsumerModule().getIncoming().add(req);
        ArchiveGraph prodMod = producer.getProducerModule();
        if (prodMod != null) {
            prodMod.getOutgoing().add(req);
        }
        return req;
    }

    @Override
    public SetProperty<LazyString> required() {
        return requires;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "DefaultArchiveConfig{" +
            "name='" + name + '\'' +
            // hm...  finalizing any providers for these requires during .toString() could lead to nasty debugging sessions
            ", required=" + requires.get() +
            ", path=" + getPath() +
            '}';
    }

    public String getPath() {
        return platform.getName() + ":" + getName();
    }

    @Override
    public boolean isSourceAllowed() {
        return unwrapBoolean(sourceAllowed);
    }

    @Override
    public boolean isTest() {
        return test == null ? getName().matches(".*[tT]est.*") : unwrapBoolean(test);
    }

    @Override
    public boolean isPublished() {
        // TODO: assert that our ReadyState is past a given requirement,
        //  so we can be sure that our schema can resolve to boolean safely.
        //  In reality, we likely need to just use gradle Property api instead here...
        if (!getPlatform().isPublished()) {
            return false;
        }
        return publish.getOrElse(false);
    }

    @Override
    public void setSourceAllowed(Object sourceAllowed) {
        this.sourceAllowed = sourceAllowed;
    }

    @Override
    public void setPublished(Object published) {
        this.published = published;
    }

    @Override
    public void setPublishedProvider(Provider<Boolean> allowed) {
        publish.set(allowed);
    }

    @Override
    public void setTest(Object test) {
        this.test = test;
    }

    @Override
    public ImmutableAttributes getAttributes(ProjectView view) {
        final ImmutableAttributesFactory factory = view.getAttributesFactory();
        return factory.concat(
            factory.of(XapiSchemaPlugin.ATTR_PLATFORM_TYPE, getPlatform().getName()),
            factory.of(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE, getName())
        );
    }

    @Override
    public PlatformConfigInternal getPlatform() {
        return platform;
    }

    @Override
    public void baseOn(ArchiveConfig rooted, boolean requiresOnly) {
        this.requires.addAll(rooted.required());
        if (requiresOnly) {
            return;
        }
        if (rooted instanceof DefaultArchiveConfig) {
            // yup, we definitely want to be using Property<Boolean> instead of Object...
            DefaultArchiveConfig other = (DefaultArchiveConfig) rooted;
            if (other.sourceAllowed != null) {
              this.sourceAllowed = other.sourceAllowed;
            }
            if (other.test != null) {
                this.test = other.test;
            }
            if (other.published != null) {
                this.published = other.published;
            }
        } else {
            this.sourceAllowed = rooted.isSourceAllowed();
            this.test = rooted.isTest();
            this.published = rooted.isPublished();
        }
    }

    @Override
    public void fixRequires(PlatformConfig platConfig) {
        final PlatformConfig rootPlatform = platConfig.getRoot();
        if (platConfig != rootPlatform) {
            final ArchiveConfig target = platConfig.getRoot().getArchive(getName());
            requires.addAll(
                // add a provider of an iterable of mapped LazyString
                target.required().flatMap(sourceProvider ->
                    // convert each of the root platform's required() Set<LazyString>
                    view.lazyProvider(()->{
                        final ArrayList<LazyString> items = new ArrayList<>(sourceProvider);
                        items.replaceAll(str -> str.map(parentRequire ->
                                    // immediately create a new LazyString which does not compute anything until toString()d
                                    new LazyString(() -> platConfig.getName() + GUtil.toCamelCase(parentRequire))
                                ));
                        return items;
                        }
                    )
                )
            );
        }


    }

    @Override
    public boolean isOrRequires(ArchiveConfigInternal module) {
        return // module.isPublished() &&
            moduleRequires(module);
    }


    @Override
    public boolean moduleRequires(ArchiveConfigInternal module) {
        final PlatformConfigInternal myPlat = getPlatform();
        final String mod = module.getName();
        if (getName().equals(mod)) {
            return true;
        }
        final SetProperty<LazyString> req = required();
        final Set<LazyString> direct = req.get();
        req.finalizeValue();
        for (LazyString item : direct) {
            String required = item.toString();
            if (mod.equals(required)) {
                return true;
            }
            if (myPlat.getArchive(required).moduleRequires(module)) {
                view.getLogger().quiet("{}.{}.{} requires {}", view.getPath(), myPlat.getName(), required, module.getName());
                return true;
            }
        }
        return false;
    }

}
