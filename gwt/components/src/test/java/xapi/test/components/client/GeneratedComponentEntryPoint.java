package xapi.test.components.client;

import xapi.annotation.compile.Dependency;
import xapi.annotation.compile.Dependency.DependencyType;
import xapi.annotation.compile.Resource;
import xapi.annotation.ui.UiTemplate;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.GwtcProperties;
import xapi.gwtc.api.ObfuscationLevel;
import xapi.constants.X_Namespace;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.ext.TreeLogger.Type;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/14/16.
 */

@Gwtc(
    includeGwtXml={
        @Resource("xapi.X_Components"),
        @Resource("xapi.X_Gwtc"),
        @Resource("xapi.X_Elemental"),
        @Resource("xapi.test.components.ComponentTest"),
        @Resource("com.google.gwt.user.User"),
    }
    ,includeSource=""
    ,includeHostHtml={
    @UiTemplate("<div id='logger' />")
}
    ,dependencies={
    @Dependency(dependencyType= DependencyType.MAVEN, loadChildren = false,
        groupId="net.wetheinter", value="xapi-gwt", version= X_Namespace.XAPI_VERSION)
    }
    ,propertiesLaunch=@GwtcProperties(
    obfuscationLevel= ObfuscationLevel.PRETTY
    ,logLevel= Type.INFO
)
)
public class GeneratedComponentEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {

  }

}
