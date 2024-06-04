package net.wti.gradle.test.api


import java.util.function.Function
/**
 * Handy tools for creating test files.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/26/18 @ 2:20 AM.
 */
trait TestFileTools implements HasTestFiles {

    File folder(String ... paths) {
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
        File f = folderFactory(paths.init()).apply(paths.last())
        !f.parentFile.exists() && f.mkdirs()
        !f.exists() && f.createNewFile()
        assert f.file : "Not a file: $f"
        return f
    }

    Function<String, File> folderFactory(String ... paths) {
        File f = folder(paths)
        return { String rest -> new File(f, rest) }
    }

    File sourceFile(String srcSet = 'main', String pkg, String cls) {
        String[] paths = "src/$srcSet/java/${pkg.replace('.' as char, '/' as char)}".split("[/]")
        File dir = folder(paths)
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
        File root = srcSet ? folder("src/$srcSet/java") : rootDir
        FileTreeBuilder builder = new FileTreeBuilder(root)
        builder.call(src)
        return root
    }

    File addSource(String srcSet = 'main', String pkg, String cls, String body) {
        File source = sourceFile(srcSet, pkg, cls)
        source << body
        return source
    }
}
