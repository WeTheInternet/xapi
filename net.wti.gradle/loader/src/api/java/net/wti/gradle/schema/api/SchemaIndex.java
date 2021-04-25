package net.wti.gradle.schema.api;

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

}
