package net.wti.gradle.schema.api;

import net.wti.gradle.schema.api.SchemaIndex;

import static xapi.string.X_String.firstNotEmpty;

/**
 * SchemaPatternResolver:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 20/04/2021 @ 2:48 a.m..
 */
public interface SchemaPatternResolver {

    default String resolvePattern(String pattern, SchemaIndex index, String projectName, String platform, String module) {
        return resolvePattern(pattern, index.getBuildName(), projectName, index.getGroupIdNotNull(), index.getVersion().toString(), platform, module);
    }
    default String resolvePattern(String pattern, String buildName, String projectName, String groupId, String version, String platform, String module) {
        return pattern
                .replaceAll("[$]build", firstNotEmpty(buildName, ":"))
                .replaceAll("[$]name", projectName)
                .replaceAll("[$]group", groupId)
                .replaceAll("[$]version", version)
                .replaceAll("[$]platform", platform)
                .replaceAll("[$]module", module)
                ;
    }

}
