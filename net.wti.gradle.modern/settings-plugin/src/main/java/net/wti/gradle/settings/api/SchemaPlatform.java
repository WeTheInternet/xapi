package net.wti.gradle.settings.api;

import org.gradle.api.Named;

import java.io.Serializable;

/**
 * Abstraction layer over a platform descriptor, like <mod-name replace=["main"] published=true />
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-06 @ 4:39 a.m.
 */
public interface SchemaPlatform extends Named, Serializable {
    String getName();
    String getPublishPattern();
    String getReplace();
    boolean isPublished();
    boolean isSourcePublished();
    boolean isDisabled();
    boolean isTest();
    SchemaPlatform update(SchemaPlatform module);

    void setPublished(boolean published);

    void setSourcePublished(boolean sourcePublished);

    void setDisabled(boolean disabled);
}

