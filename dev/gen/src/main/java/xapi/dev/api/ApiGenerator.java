package xapi.dev.api;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import xapi.annotation.compile.Generated;
import xapi.annotation.inject.InstanceDefault;
import xapi.dev.source.SourceBuilder;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.reflect.X_Reflect;
import xapi.time.X_Time;
import xapi.util.api.Digester;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/17/16.
 */
@InstanceDefault(implFor = ApiGenerator.class)
public class ApiGenerator {

    public static void main(String ... coords) throws IOException {
        if (coords.length == 0) {
            final String loc = X_Reflect.getFileLoc(ApiGenerator.class);
            coords = new String[]{
                loc
                    .replace('\\', '/') // windows
                    .replace(
                        "dev/gen/target/classes",
                        "core/fu/src/main/xapi"
                    )
            };
        }
        ApiGenerator generator = X_Inject.instance(ApiGenerator.class);
        for (String coord : coords) {
            Path path = Paths.get(coord);
            X_Log.info(ApiGenerator.class, "Checking for xapi source in", coord);
            Files.find(path, 50, (p, a)-> p.toString().endsWith("Out.xapi"))
                .forEach(file->generator.generate(path, file));
        }
    }

    private <Ctx extends ApiGeneratorContext<Ctx>> void generate(Path path, Path file) {
        try {
            X_Log.info(ApiGenerator.class, "Generating classes from", file.toString(), "in", path.toString());
            String sourceFile = X_IO.toStringUtf8(Files.newInputStream(file));
            if (sourceFile.trim().isEmpty()) {
                X_Log.trace(ApiGenerator.class, "Skipping empty source file", file.toString());
                return;
            }
            Path relativePath = path.relativize(file);
            final Node ast = JavaParser.parseNode(sourceFile);
            GeneratorVisitor<Ctx> visitor = new GeneratorVisitor<>(relativePath);
            final ApiGeneratorContext<Ctx> ctx = new ApiGeneratorContext<>();
            ast.accept(visitor, (Ctx)ctx);

            Path sourceDir = file.getRoot();
            boolean sawSrc = false;
            for (Path subpath : file) {
                if (sawSrc) {
                    if ("main".equals(subpath.toString())) {
                        sourceDir = sourceDir.resolve(subpath);
                        sourceDir = sourceDir.resolve("gen");
                    } else {
                        sourceDir = sourceDir.getParent().resolve("gen");
                    }
                    break;
                } else if ("src".equals(subpath.toString())) {
                    sourceDir = sourceDir.resolve(subpath);
                    sawSrc = true;
                } else {
                    sourceDir = sourceDir.resolve(subpath);
                }
            }

            Digester digester = X_Inject.instance(Digester.class);
            for (SourceBuilder<?> source : ctx.getSourceFiles()) {
                String src = source.toSource();
                final byte[] digest = digester.digest(src.getBytes());
                String hash = digester.toString(digest);
                final String anno = "@" +
                    source.addImport(Generated.class) +
                    "(date=\"" +
                    X_Time.now().toString() +
                    "\",\n  value = {\"" +
                    ApiGenerator.class.getName() +
                    "\", \"" +
                    relativePath.toString() +
                    "\", \"" +
                    hash +
                    "\"})";

                source.getClassBuffer().addAnnotation(anno);
                Path saveTo = sourceDir;
                for (String part : source.getPackage().split("[.]")) {
                    saveTo = saveTo.resolve(part);
                }
                saveTo = saveTo.resolve(source.getSimpleName() + ".java");
                Files.createDirectories(saveTo.getParent());
                if (!Files.exists(saveTo)) {
                    saveTo = Files.createFile(saveTo);
                }
                X_IO.drain(Files.newOutputStream(saveTo),
                    X_IO.toStreamUtf8(source.toSource()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
