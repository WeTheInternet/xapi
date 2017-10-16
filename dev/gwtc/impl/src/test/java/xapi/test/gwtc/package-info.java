@Gwtc(
  includeGwtXml={
      @Resource("xapi.X_Inherit")
  }
    ,dependencies={
    @Dependency(dependencyType= DependencyType.MAVEN, loadChildren = false,
        groupId="net.wetheinter", value="xapi-gwt",version= X_Namespace.XAPI_VERSION),
}
//  ,includeHostHtml={
//    @UiTemplate(
//      value="<div>"
//            + "<h4>"+$package+"</h4>"
//            + $child
//          + "</div>",
//      embedStrategy=WrapEachPackage,
//      keys={$child, $package}
//    ),
//    @UiTemplate(
//      value="<div>"
//          + "<h5>"+$class+"</h5>"
//          + $child
//        + "</div>",
//        embedStrategy=WrapEachClass,
//        keys={$child, $class}
//    ),
//    @UiTemplate(
//      value="<div>"
//          + "<h5>"+$class+"."+$method+"</h5>"
//          + $child
//        + "</div>",
//        embedStrategy=WrapEachMethod,
//        keys={$method, $class}
//    ),
//  }
)
package xapi.test.gwtc;
import xapi.annotation.compile.Dependency;
import xapi.annotation.compile.Dependency.DependencyType;
import xapi.annotation.compile.Resource;
import xapi.gwtc.api.Gwtc;
import xapi.util.X_Namespace;
