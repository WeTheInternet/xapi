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

    private final Out1<String> genDir;
    private final Out1<String> outputDir;

    public FileBasedSourceHelper(Out1<String> genDir, Out1<String> outputDir) {
        this.genDir = genDir;
        this.outputDir = outputDir;
    }

    @Override
    public String readSource(String pkgName, String clsName, Object o) {
        throw new UnsupportedOperationException("Cannot read source from this test");
    }

    @Override
    public void saveSource(String pkgName, String clsName, String src, Object o) {
        final Path target = Paths.get(genDir.out1());
        final Path packageDir = target.resolve(pkgName.replace('.', File.separatorChar));
        try {
            Files.createDirectories(packageDir);
            final Path file = packageDir.resolve(clsName.endsWith(".java") ? clsName : clsName + ".java");
            Files.write(file, src.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String saveResource(String path, String fileName, String src, Object o) {
        final Path target = Paths.get(outputDir.out1());
        final Path packageDir = target.resolve(path.replace('/', File.separatorChar));
        try {
            Files.createDirectories(packageDir);
            final Path file = packageDir.resolve(fileName);
            Files.write(file, src.getBytes());
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

}
