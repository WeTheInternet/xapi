package xapi.dev.gen;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public interface SourceHelper <Hints> {

    String readSource(String pkgName, String clsName, Hints hints);

    void saveSource(String pkgName, String clsName, String src, Hints hints);

    String saveResource(String path, String fileName, String src, Hints hints);

}
