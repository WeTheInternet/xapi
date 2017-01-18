package xapi.server.gen;

import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.SourceBuilder;
import xapi.fu.Rethrowable;
import xapi.io.X_IO;
import xapi.process.X_Process;
import xapi.source.X_Source;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public class WebAppGeneratorContext extends ApiGeneratorContext<WebAppGeneratorContext> implements Rethrowable {

    public WebAppGeneratorContext() {

    }

    private String generatorDirectory;

    public String readSource(String pkgName, String clsName) {
        // Search for source file on classpath.
        String resource = X_Source.qualifiedName(pkgName, clsName).replace('.', '/') + ".java";
        try (
            final InputStream input = rootClassloader().getResourceAsStream(resource);
        ) {
           String existing = X_IO.toStringUtf8(input);
           return existing;
        } catch (IOException e) {
            final SourceBuilder<WebAppGeneratorContext> buffer = getOrMakeClass(
                pkgName,
                clsName,
                false
            );
            final WebAppGeneratorContext was = buffer.getPayload();
            if (was != this) {
                buffer.setPayload(this);
                X_Process.runFinally(()-> {
                    if (buffer.getPayload() == this) {
                        buffer.setPayload(was);
                    }
                });
                initializeBuffer(buffer, pkgName, clsName);
            }
            return buffer.toSource();
        }
    }

    protected void initializeBuffer(SourceBuilder<WebAppGeneratorContext> buffer, String pkgName, String clsName) {

    }

    protected ClassLoader rootClassloader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public void setGeneratorDirectory(String generatorDirectory) {
        this.generatorDirectory = generatorDirectory;
    }

    public String getGeneratorDirectory() {
        return generatorDirectory;
    }
}
