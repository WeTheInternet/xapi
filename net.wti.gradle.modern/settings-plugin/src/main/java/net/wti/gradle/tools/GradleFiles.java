package net.wti.gradle.tools;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;

/**
 * GradleFiles:
 * <p>
 * <p>The built-in GFileUtils shipped w/ gradle are internal and deprecated, so we'll do our own ugly helper methods.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 08/06/2024 @ 8:14 p.m.
 */
public class GradleFiles {
    public static void writeFile(final File file, final String text) {
        try {
            final File parent = file.getParentFile();
            if (!parent.isDirectory()) {
                if (!parent.mkdirs()) {
                    throw new IOException("Unable to create directory " + parent.getAbsolutePath());
                }
            }
            ResourceGroovyMethods.setText(file, text);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write to file " + file.getAbsolutePath(), e);
        }
    }

    public static String readFile(final File file) {
        try {
            return ResourceGroovyMethods.getText(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read file " + file.getAbsolutePath(), e);
        }
    }

    public static void touch(final File file) {
        try {
            if (file.exists()) {
                    Files.setLastModifiedTime(Paths.get(file.getAbsolutePath()), FileTime.from(Instant.now()));
            } else {
                if (!file.createNewFile() && !file.exists()) {
                    throw new GradleException("Cannot touch " + file.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            if (file.exists() && file.length() == 0) {
                writeFile(file, "");
            } else {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static void deleteQuietly(final File file) {
        if (file.isDirectory()) {
            final File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteQuietly(child);
                }
            }
        }
        if (!file.delete()) {
            throw new GradleException("Unable to cleanup file " + file.getAbsolutePath());
        }
    }
}
