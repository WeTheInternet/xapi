package net.wti.gradle.test

import java.util.function.Function

/**
 * Handy tools for creating test files.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/26/18 @ 2:20 AM.
 */
trait TestFileTools {
    abstract File getRootDir()

    File newFolder(String ... paths) {
        File f = getRootDir()
        for (String path : paths) {
            f = new File(f, path)
            // This assert in the loop will help diagnose any directory permissions errors you may have.
            assert f.directory || f.mkdirs() : "Unable to create directory $f"
        }
        f.mkdirs()
        return f
    }

    File file(String ... paths) {
        File f = folder(paths.init()).apply(paths.last())
        !f.exists() && f.createNewFile()
        assert f.file : "Not a file: $f"
        return f
    }

    Function<String, File> folder(String ... paths) {
        File f = newFolder(paths)
        return { rest -> new File(f, rest) }
    }

    File sourceFile(String srcSet = 'main', String pkg, String cls) {
        String[] paths = "src/$srcSet/java/${pkg.replace('.', '/')}".split("[/]")
        File dir = newFolder(paths)
        return new File(dir, cls + '.java')
    }


    /*
        private File subproject(String name, Closure structure) {
        file("settings.gradle") << "include '$name'\n"
        def pdir = new File(testDirectory, name)
        pdir.mkdirs()
        // Use standard groovy filetree builder, for concise test directory layouts...
        new FileTreeBuilder(pdir).call(structure)
        pdir
    }
    */

    File withSource(String srcSet = '', @DelegatesTo(value = FileTreeBuilder, strategy = Closure.DELEGATE_FIRST) Closure src) {
        File root = srcSet ? newFolder("src/$srcSet/java") : rootDir
        new FileTreeBuilder(root).call(src)
        return root
    }

    File addSource(String srcSet = 'main', String pkg, String cls, String body) {
        File source = sourceFile(srcSet, pkg, cls)
        source << body
        return source
    }
}
