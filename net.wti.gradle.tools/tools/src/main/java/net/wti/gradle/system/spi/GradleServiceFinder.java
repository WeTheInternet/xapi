package net.wti.gradle.system.spi;

import net.wti.gradle.system.service.GradleService;
import org.gradle.api.Project;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/15/18 @ 4:53 AM.
 */
public class GradleServiceFinder {

    public static GradleService getService(Project from) {
        return GradleService.buildOnce(from, GradleService.EXT_NAME, GradleServiceFinder::createService);
    }

    private static GradleService createService(Project project) {
        return new DefaultGradleService(project);
    }
}
