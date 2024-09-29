package xapi.dev.processor;

import xapi.dev.gen.SourceHelper;
import xapi.util.X_Util;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import static xapi.util.X_Util.defaultCharset;
import static xapi.util.X_Util.rethrow;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/29/16.
 */
public interface AnnotationTools extends SourceHelper<Element> {

    JavaFileManager getFileManager();

    Filer getFiler();

    Types getTypes();

    Elements getElements();

    String getPackageName(TypeElement type);

    @Override
    default Class<Element> hintType() {
        return Element.class;
    }

    default JavaFileObject outputJava(String qualifiedName, Element ... types) throws IOException {
        Filer filer = getFiler();
        if (filer == null) {
            // annotation processor filer might be null,
            // in which case we fall back to the file maanger.
            // Note that this (currently) ignores the types elements.
            return getFileManager().getJavaFileForOutput(
                  StandardLocation.SOURCE_OUTPUT,
                  qualifiedName,
                  Kind.SOURCE,
                  null
            );
        } else {
            return filer.createSourceFile(qualifiedName, types);
        }
    }

    default FileObject outputFile(String path, String file, Element ... types) throws IOException {
        Filer filer = getFiler();
        if (filer == null) {
            // annotation processor filer might be null,
            // in which case we fall back to the file maanger.
            // Note that this (currently) ignores the types elements.
            return getFileManager().getFileForOutput(
                  StandardLocation.SOURCE_OUTPUT,
                  path,
                file,
                  null
            );
        } else {
            return filer.createResource(StandardLocation.SOURCE_OUTPUT, path, file, types);
        }
    }

    @Override
    default String readSource(String pkgName, String clsName, Element hints) {
        final JavaFileManager filer = getFileManager();
        try {
            final FileObject file = filer.getFileForInput(
                StandardLocation.SOURCE_PATH,
                pkgName,
                clsName
            );
            if (file == null) {
                throw new NullPointerException("No file found for " + (pkgName.isEmpty() ? clsName : pkgName + "." + clsName) + " in source path");
            }
            return file.getCharContent(true).toString();
        } catch (Exception e) {
            if (e instanceof RuntimeException && e.getCause() instanceof FileNotFoundException) {
                throw new RuntimeException(
                    "If this error is occurring via maven / IDE, consider adding " +
                        "src/main/java to your resource path, so apt can see your resources.\nOriginal error:" + e.getMessage(), e);
            }
            throw rethrow(e);
        }

    }

    @Override
    default void saveSource(String pkgName, String clsName, String src, Element hints) {
        try {
            final JavaFileObject file = outputJava(
                pkgName == null || pkgName.isEmpty() ? clsName : pkgName + "." + clsName,
                hints
            );
            try (OutputStream o = file.openOutputStream()) {
                o.write(src.getBytes(defaultCharset()));
            }
        } catch (IOException e) {
            throw X_Util.rethrow(e);
        }
    }

    @Override
    default String saveResource(String path, String fileName, String src, Element hints) {

        try {
            if (path == null || path.isEmpty()) {
                path = "";
            }
            final FileObject file = outputFile(
                path,
                fileName,
                hints
            );
            try (OutputStream o = file.openOutputStream()) {
                o.write(src.getBytes(defaultCharset()));
            }
            return file.toUri().getPath().replace("file:", "");
        } catch (IOException e) {
            throw X_Util.rethrow(e);
        }

    }
}
