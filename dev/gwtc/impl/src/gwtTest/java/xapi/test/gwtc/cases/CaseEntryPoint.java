package xapi.test.gwtc.cases;

import xapi.annotation.compile.Dependency;
import xapi.annotation.compile.Dependency.DependencyType;
import xapi.annotation.compile.Resource;
import xapi.annotation.inject.SingletonOverride;
import xapi.annotation.ui.UiTemplate;
import xapi.collect.api.Fifo;
import xapi.gwt.log.JsLog;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.GwtcProperties;
import xapi.gwtc.api.ObfuscationLevel;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;
import xapi.platform.GwtPlatform;
import xapi.test.junit.JUnit4Runner;
import xapi.test.junit.JUnitUi;
import xapi.constants.X_Namespace;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

@Gwtc(
  includeGwtXml={
    @Resource("xapi.X_Inherit"),
    @Resource("xapi.X_Inject"),
    @Resource("xapi.X_Collect"),
    @Resource("com.google.gwt.core.Core"),
  },
    inheritClasses = {
        JUnitUi.class, JUnit4Runner.class
    }
  ,includeSource=""
  ,includeHostHtml={
    @UiTemplate("<div id='logger' />")
  }
  ,dependencies={
    @Dependency(dependencyType=DependencyType.MAVEN,
      groupId="net.wetheinter", value="xapi-elemental",version=X_Namespace.XAPI_VERSION),
    @Dependency(dependencyType=DependencyType.MAVEN,
      groupId="net.wetheinter", value="xapi-gwtc-impl",version=X_Namespace.XAPI_VERSION),
    @Dependency(dependencyType=DependencyType.MAVEN,
      // we should not have to declare this classifier'ed dependency, yet, we do...
      groupId="net.wetheinter", value="xapi-gwtc-impl",version=X_Namespace.XAPI_VERSION, classifier = "sources"),
    @Dependency(dependencyType=DependencyType.MAVEN,
      groupId="net.wetheinter", value="xapi-gwt-test",version=X_Namespace.XAPI_VERSION),
    @Dependency(dependencyType=DependencyType.MAVEN,
      groupId="net.wetheinter", value="xapi-gwt-api",version=X_Namespace.XAPI_VERSION),
    @Dependency(dependencyType=DependencyType.MAVEN,
      groupId="net.wetheinter", value="xapi-process-gwt",version=X_Namespace.XAPI_VERSION),
    @Dependency("src/gwtTest/java"),
    @Dependency("src/gwtTest/resources"),
  }
  ,propertiesLaunch=@GwtcProperties(
    obfuscationLevel=ObfuscationLevel.PRETTY
  )
)
public class CaseEntryPoint implements EntryPoint {

  @GwtPlatform
  @SingletonOverride(implFor=LogService.class, priority=1)
  public static class ElementLogger extends JsLog {
    private final Element logEl = getLogger();
    @Override
    public void doLog(LogLevel level, Fifo<Object> items) {
      Element pre = Document.get().createPreElement();
      // TODO: something way better than this...
      pre.setInnerHTML("<span>"+items.join("</span> <span>")+"</span>");
      logEl.appendChild(pre);
      super.doLog(level, items);
    }
    private native Element getLogger()
    /*-{
      var el = $doc.getElementById('logger');
      return el || (
        el = $doc.createElement('div'),
        el.id = 'logger',
        $doc.body.appendChild(el),
        el
      );
    }-*/;
  }

  @Override
  public void onModuleLoad() {
    X_Log.info(getClass(), "Hello World");
  }
}
