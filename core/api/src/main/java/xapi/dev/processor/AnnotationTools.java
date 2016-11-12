package xapi.dev.processor;

import xapi.dev.gen.SourceHelper;
import xapi.util.X_Util;

import static xapi.util.X_Util.defaultCharset;

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
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/29/16.
 */
public interface AnnotationTools extends SourceHelper<Element> {

    JavaFileManager getFileManager();

    Filer getFiler();

    Types getTypes();

    Elements getElements();

    String getPackageName(TypeElement type);

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

    @Override
    default String readSource(String pkgName, String clsName, Element hints) {
        try {
            final JavaFileManager filer = getFileManager();
            final FileObject file = filer.getFileForInput(
                StandardLocation.SOURCE_PATH,
                pkgName,
                clsName
            );
            return file.getCharContent(true).toString();
        } catch (IOException e) {
            throw X_Util.rethrow(e);
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
}
