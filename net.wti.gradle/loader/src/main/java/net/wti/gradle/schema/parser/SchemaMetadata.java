package net.wti.gradle.schema.parser;

import com.github.javaparser.ast.expr.UiContainerExpr;

import java.util.List;

/**
 * Example schema.xapi (edited snapshot of xapi project's main schema.xapi:
 * <pre>
 * <xapi-schema
 *
 *     defaultRepoUrl = jcenter()
 *
 *     schemaLocation = "schema/schema.gradle"
 *
 *     platforms = [
 *         <main />,
 *         <jre replace = "main" published = true/>,
 *         <gwt replace = "main" published = true/>,
 *     ]
 *
 *     modules = [
 *         <api />,
 *         <spi />,
 *         <main require = [ api, "spi" ] />,
 *     ]
 *
 *     projects = {
 *         // the projects below all have gwt, jre and other platforms
 *         multiPlatform: [
 *             "collections",
 *             "common",
 *             "model",
 *         ]
 *
 *         // the projects below all have a single "main" platform (potentially w/ multiple modules like api and testTools though!)
 *         standalone: [
 *             "util"
 *         ]
 *
 *         // the projects below are effectively parents of multiple child projects.
 *         // it will be left to the schema.xapi of these projects to determine whether
 *         // the child modules are multiPlatform, standalone, or nested multiProject
 *         multiProject: {
 *             dist: ["gwt", "jre"],
 *             samples: ["demo"]
 *         }
 *     }
 *
 *     // declare any external dependencies here,
 *     // so we can handle pre-emptively syncing jars (and maybe source checkouts) to a local cache,
 *     // then just reference these "blessed artifacts" w/out versions anywhere;
 *     // leaving it up to the system
 *     external = [
 *         // preload elements will be downloaded once, on build startup, into a local filesystem repository (xapi.repo)
 *         <preload
 *             name = "gwt"
 *             url = "https://wti.net/repo"
 *             version = "2.8.0"
 *             // limits these artifacts to gwt platform, where they will be auto-available as versionless dependencies
 *             // this inheritance is also given to any platform replacing gwt platform.
 *             platforms = [ "gwt" ]
 *             module = [ ... ] // optionally limit modules
 *             artifacts = {
 *                 "com.google.gwt" : [
 *                     "gwt-user",
 *                     "gwt-dev",
 *                     "gwt-codeserver",
 *                 ]
 *             }
 *         /preload>
 *         ,
 *     ]
 *
 * /xapi-schema>
 * </pre>
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 29/07/19 @ 5:09 AM.
 */
public class SchemaMetadata {
    public static final String EXT_NAME = "_xapi_schema";

    private String defaultUrl;
    private String schemaLocation;
    private UiContainerExpr projects;
    private List<UiContainerExpr> platforms, modules, external;

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public SchemaMetadata setDefaultUrl(String defaultUrl) {
        this.defaultUrl = defaultUrl;
        return this;
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public SchemaMetadata setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
        return this;
    }

    public UiContainerExpr getProjects() {
        return projects;
    }

    public SchemaMetadata setProjects(UiContainerExpr projects) {
        this.projects = projects;
        return this;
    }

    public List<UiContainerExpr> getPlatforms() {
        return platforms;
    }

    public SchemaMetadata setPlatforms(List<UiContainerExpr> platforms) {
        this.platforms = platforms;
        return this;
    }

    public List<UiContainerExpr> getModules() {
        return modules;
    }

    public SchemaMetadata setModules(List<UiContainerExpr> modules) {
        this.modules = modules;
        return this;
    }

    public List<UiContainerExpr> getExternal() {
        return external;
    }

    public SchemaMetadata setExternal(List<UiContainerExpr> external) {
        this.external = external;
        return this;
    }

    public void addPlatform(String platName, UiContainerExpr el) {
    }
}
