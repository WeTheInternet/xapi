package xapi.dev.api;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.annotation.inject.InstanceDefault;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.reflect.X_Reflect;

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
            System.out.println(path);
            Files.find(path, 50, (p, a)-> p.toString().endsWith(".xapi"))
                .forEach(file->generator.generate(path, file));
        }
    }

    private <Ctx extends ApiGeneratorContext<Ctx>> void generate(Path path, Path file) {
        try {
            String sourceFile = X_IO.toStringUtf8(Files.newInputStream(file));
            final Path relativePath = path.relativize(file);
            final UiContainerExpr ast = JavaParser.parseUiContainer(sourceFile);
            GeneratorVisitor<Ctx> visitor = new GeneratorVisitor<>(relativePath);
            final ApiGeneratorContext ctx = new ApiGeneratorContext();
            ast.accept(visitor, (Ctx)ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
