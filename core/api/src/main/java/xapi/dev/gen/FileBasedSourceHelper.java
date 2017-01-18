package xapi.dev.gen;

import xapi.fu.Out1;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/7/17.
 */
public class FileBasedSourceHelper<Hint> implements SourceHelper<Hint> {

    private final Out1<String> dir;

    public FileBasedSourceHelper(Out1<String> dir) {
        this.dir = dir;
    }

    @Override
    public String readSource(String pkgName, String clsName, Object o) {
        throw new UnsupportedOperationException("Cannot read source from this test");
    }

    @Override
    public void saveSource(String pkgName, String clsName, String src, Object o) {
        final Path genDir = Paths.get(dir.out1());
        final Path packageDir = genDir.resolve(pkgName.replace('.', File.separatorChar));
        try {
            Files.createDirectories(packageDir);
            final Path file = packageDir.resolve(clsName.endsWith(".java") ? clsName : clsName + ".java");
            Files.write(file, src.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void saveResource(String path, String fileName, String src, Object o) {
        final Path genDir = Paths.get(dir.out1());
        final Path packageDir = genDir.resolve(path.replace('/', File.separatorChar));
        try {
            Files.createDirectories(packageDir);
            final Path file = packageDir.resolve(fileName);
            Files.write(file, src.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

}
