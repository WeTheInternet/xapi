package net.wti.gradle.migrate;

import org.apache.commons.lang.StringUtils;

import java.io.File;

/**
 * ArtifactFile:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 27/10/2024 @ 2:19 a.m.
 */
public class ArtifactFile {
    private String name;
    private String classifier;
    private String extension;

    public ArtifactFile(File file, String version) {
        this(file.getName(), version);
    }

    public ArtifactFile(String fileBaseName, String version) {
        this.name = fileBaseName;
        this.extension = "";
        this.classifier = "";
        boolean done = false;
        int startVersion = StringUtils.lastIndexOf(this.name, "-" + version);
        if (startVersion >= 0) {
            int endVersion = startVersion + version.length() + 1;
            if (endVersion == this.name.length()) {
                this.name = this.name.substring(0, startVersion);
                done = true;
            } else if (endVersion < this.name.length() && this.name.charAt(endVersion) == '-') {
                String tail = this.name.substring(endVersion + 1);
                this.name = this.name.substring(0, startVersion);
                this.classifier = StringUtils.substringBeforeLast(tail, ".");
                this.extension = StringUtils.substringAfterLast(tail, ".");
                done = true;
            } else if (endVersion < this.name.length() && StringUtils.lastIndexOf(this.name, ".") == endVersion) {
                this.extension = this.name.substring(endVersion + 1);
                this.name = this.name.substring(0, startVersion);
                done = true;
            }
        }

        if (!done) {
            this.extension = StringUtils.substringAfterLast(this.name, ".");
            this.name = StringUtils.substringBeforeLast(this.name, ".");
        }

        if (this.classifier.length() == 0) {
            this.classifier = null;
        }

    }

    public String getName() {
        return this.name;
    }

    public String getClassifier() {
        return this.classifier;
    }

    public String getExtension() {
        return this.extension;
    }
}

