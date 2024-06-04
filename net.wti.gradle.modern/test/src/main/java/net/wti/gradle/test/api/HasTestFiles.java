package net.wti.gradle.test.api;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.util.FileTreeBuilder;

import java.io.File;
import java.util.function.Function;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/15/19 @ 4:13 AM.
 */
public interface HasTestFiles extends HasRootDir {

    File folder(String ... paths);

    File file(String ... paths);

    Function<String, File> folderFactory(String ... paths);

    default File sourceFile(String pkg, String cls) {
        return sourceFile("main", pkg, cls);
    }
    File sourceFile(String srcSet, String pkg, String cls);

    default File withSource(@DelegatesTo(value = FileTreeBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure src) {
        return withSource("", src);
    }
    File withSource(String srcSet, @DelegatesTo(value = FileTreeBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure src);

    default File addSource(String pkg, String cls, String body) {
        return addSource("main", pkg, cls, body);
    }
    File addSource(String srcSet, String pkg, String cls, String body);
}
