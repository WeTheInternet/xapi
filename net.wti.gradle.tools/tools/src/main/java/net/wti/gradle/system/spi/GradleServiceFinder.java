package net.wti.gradle.system.spi;

import net.wti.gradle.system.service.GradleService;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/15/18 @ 4:53 AM.
 */
public class GradleServiceFinder {

    public static final String OVERRIDE_GRADLE_SERVICE = "xapi.gradle.service";

    @SuppressWarnings("unchecked")
    public static GradleService getService(Project project) {
        Function<? super Project, ? extends GradleService> factory = GradleServiceFinder::createService;
        final String serviceCls = (String) project.findProperty(OVERRIDE_GRADLE_SERVICE);
        if (serviceCls != null) {
            factory = p -> {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                try {
                    final Class<? extends GradleService> cls = (Class<? extends GradleService>) cl.loadClass(serviceCls);
                    final Constructor<? extends GradleService> ctor = cls.getConstructor(Project.class);
                    return ctor.newInstance(project);
                } catch (ClassNotFoundException e) {
                    throw new GradleException("Cannot load " + serviceCls + " defined by property " + OVERRIDE_GRADLE_SERVICE + " in " + project.getPath());
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    throw new GradleException(serviceCls + " must have a public constructor(Project onlyArg);", e);
                } catch (InstantiationException e) {
                    throw new GradleException(serviceCls + " is not instantiable", e);
                } catch (InvocationTargetException e) {
                    throw new GradleException(serviceCls + "(Project) threw unexpected exception for project " + project.getPath(), e);
                }
            };
        }

        return GradleService.buildOnce(GradleService.class, project, GradleService.EXT_NAME, factory);
    }

    private static GradleService createService(Project project) {
        return new DefaultGradleService(project);
    }
}
