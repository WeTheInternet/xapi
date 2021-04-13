package net.wti.gradle.schema.spi;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.schema.api.QualifiedModule;

import java.io.File;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-07-20 @ 2:08 a.m..
 */
public interface SchemaIndex {

    String getBuildName();

    CharSequence getGroupId();

    CharSequence getVersion();

    default String getGroupIdNotNull() {
        CharSequence groupId = getGroupId();

        if (groupId == null || QualifiedModule.UNKNOWN_VALUE.equals(groupId.toString())) {
            groupId = getBuildName();
        }
        return groupId == null ? QualifiedModule.UNKNOWN_VALUE : groupId.toString();
    }

    SchemaIndexReader getReader();

    static String getIndexLocation(MinimalProjectView view, SchemaProperties properties) {
        Object prop = properties.getIndexLocation(view);
        if (prop == null && view != null) {
            prop = view.getRootProject().getProjectDir();
            if (prop != null) {
                prop = new File((File)prop, "build/index");
            }
        }
        assert prop != null || view == null: view + " has root project with null projectDir";
        return prop == null ? new File("./build/index").getAbsolutePath() : String.valueOf(prop);
    }

    static String getIndexLocation(MinimalProjectView view) {
        return getIndexLocation(view, SchemaProperties.getInstance());
    }
}
