package xapi.dev.processor;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;
import java.io.IOException;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/29/16.
 */
public interface AnnotationTools {

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
}
